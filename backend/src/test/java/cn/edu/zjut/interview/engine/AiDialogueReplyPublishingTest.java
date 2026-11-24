package cn.edu.zjut.interview.engine;

import cn.edu.zjut.interview.dto.CreateSessionRequest;
import cn.edu.zjut.interview.dto.ReplyEvent;
import cn.edu.zjut.interview.engine.payload.ModifyCharacterPayload;
import cn.edu.zjut.interview.importer.ImportResult;
import cn.edu.zjut.interview.importer.ScriptYamlImporter;
import cn.edu.zjut.interview.llm.LlmClient;
import cn.edu.zjut.interview.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;

/**
 * {@code ai_dialogue} 的台词投递验收。
 *
 * <p>回归点：模型先给一句很短的转场、再给真正的提问时，两句都必须推出去。
 * 早前分句器开启了跨台词合并（{@code chat.merge_threshold}），转场和提问被并进同一个句子，
 * 发布侧只取第一段，导致面试官只说了「现在换个方向」却没问问题。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai-dialogue-publish-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                + "CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "app.import.asset-target=target/test-assets/backgrounds/publish"
})
@ActiveProfiles("test")
class AiDialogueReplyPublishingTest {

    @TempDir
    Path tempDir;

    @Autowired
    private ScriptYamlImporter importer;
    @Autowired
    private InterviewEngine engine;
    @Autowired
    private ChatService chatService;

    @MockBean
    private LlmClient llmClient;

    @Test
    void shortLeadInDoesNotSwallowFollowingQuestion() throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve("publish-case"));
        write(dir.resolve("story_config.yaml"), """
                script_name: 台词投递用例
                intro_chapter: main
                """);
        Path chapters = Files.createDirectories(dir.resolve("Chapters"));
        write(chapters.resolve("main.yaml"), """
                name: 主章
                events:
                  - type: narration
                    text: 面试开始。
                  - type: ai_dialogue
                    character: MAIN
                    prompt: 提问 JVM 内存分区。
                  - type: chapter_end
                    end_type: linear
                    next_chapter: "next"
                """);
        write(chapters.resolve("next.yaml"), """
                name: 下一章
                events:
                  - type: narration
                    text: 到了下一章
                """);
        ImportResult result = importer.importDirectory(dir);
        assertEquals(0, result.report().summary().red(), "用例剧本不该有红项：" + result.report().red());

        // 第一段远短于 merge_threshold（55 字），第二段才是真正的提问
        doAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(1);
            onDelta.accept("【平静】缓存这块先放着，我待会儿回来啃。现在换个方向。\n\n");
            onDelta.accept("【平静】JVM 内存分区，堆、栈、方法区分别放什么，什么情况下会 OOM？");
            return null;
        }).when(llmClient).stream(anyList(), any());

        Long sessionId = chatService.createSession(
                new CreateSessionRequest("strict", "java", result.scriptCode())).sessionId();

        RecordingSink sink = new RecordingSink();
        engine.run(sessionId, sink);

        assertEquals(2, sink.replies.size(), "转场与提问必须各成一条台词：" + sink.messages());
        assertTrue(sink.replies.get(0).message().contains("现在换个方向"), sink.replies.get(0).message());
        assertTrue(sink.replies.get(1).message().contains("JVM 内存分区"), sink.replies.get(1).message());
        assertFalse(sink.replies.get(0).isFinal(), "第一条不是本轮收尾");
        assertTrue(sink.replies.get(1).isFinal(), "提问才是本轮收尾");
    }

    private static void write(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    /** 只收台词，不碰网络。 */
    private static final class RecordingSink implements EventSink {

        private final List<ReplyEvent> replies = new ArrayList<>();

        @Override
        public void reply(ReplyEvent event) {
            replies.add(event);
        }

        @Override
        public void narration(String text) {
        }

        @Override
        public void input(String hint) {
        }

        @Override
        public void character(ModifyCharacterPayload payload) {
        }

        @Override
        public void chapter(String code, String name) {
        }

        @Override
        public void progress(String chapter, int eventIndex, int lineCount, JsonNode vars) {
        }

        @Override
        public void finished() {
        }

        private String messages() {
            List<String> texts = new ArrayList<>();
            for (ReplyEvent reply : replies) {
                texts.add(reply.message());
            }
            return texts.toString();
        }
    }
}
