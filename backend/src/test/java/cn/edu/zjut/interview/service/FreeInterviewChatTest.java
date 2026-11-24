package cn.edu.zjut.interview.service;

import cn.edu.zjut.interview.domain.InterviewMessage;
import cn.edu.zjut.interview.domain.InterviewQuestion;
import cn.edu.zjut.interview.domain.InterviewSession;
import cn.edu.zjut.interview.dto.CreateSessionRequest;
import cn.edu.zjut.interview.dto.CreateSessionResponse;
import cn.edu.zjut.interview.llm.LlmClient;
import cn.edu.zjut.interview.llm.LlmMessage;
import cn.edu.zjut.interview.llm.LlmOptions;
import cn.edu.zjut.interview.repository.InterviewMessageRepository;
import cn.edu.zjut.interview.repository.InterviewQuestionRepository;
import cn.edu.zjut.interview.repository.InterviewSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;

/**
 * 自由面试对话链路（issue #11）：一句「我想面前端」之后，
 * 岗位要落库、要抽题、且当前题目 / 参考要点必须真的进了本轮 system prompt。
 *
 * <p>LLM 用替身，不调网络。
 */
@SpringBootTest
@ActiveProfiles("test")
class FreeInterviewChatTest {

    private static final long WAIT_MILLIS = 5_000L;

    @Autowired
    private ChatService chatService;
    @Autowired
    private InterviewSessionRepository sessionRepository;
    @Autowired
    private InterviewMessageRepository messageRepository;
    @Autowired
    private InterviewQuestionRepository questionRepository;

    @MockBean
    private LlmClient llmClient;

    @Test
    void freeTurnDetectsJobAndInjectsCurrentQuestion() throws Exception {
        List<List<LlmMessage>> captured = new ArrayList<>();
        doAnswer(invocation -> {
            List<LlmMessage> messages = invocation.getArgument(0);
            captured.add(new ArrayList<>(messages));
            Consumer<String> onDelta = invocation.getArgument(2);
            onDelta.accept("【好奇】好，那我们从事件循环开始聊。");
            return null;
        }).when(llmClient).stream(anyList(), any(LlmOptions.class), any());

        CreateSessionResponse created = chatService.createSession(
                new CreateSessionRequest("strict", "java", null));
        Long sessionId = created.sessionId();

        chatService.processMessage(sessionId, "我想面 Web 前端，做过 Vue");

        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        while (messageRepository.countBySessionIdAndRole(sessionId, InterviewMessage.ROLE_ASSISTANT) == 0
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50L);
        }

        InterviewSession session = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals("frontend", session.getFreeJobCode(), "应从自述里识别出前端岗位");
        assertNotNull(session.getFreeCurrentQuestionId());
        InterviewQuestion question = questionRepository.findById(session.getFreeCurrentQuestionId()).orElseThrow();

        assertEquals(1, captured.size(), "一轮只应调一次模型");
        String system = captured.get(0).get(0).content();
        assertTrue(system.contains("【自由面试 · 题库出题】"), "本轮 system 应带题库出题块");
        assertTrue(system.contains("本场面试岗位：Web 前端"), "岗位名应进 prompt");
        assertTrue(system.contains(question.getText()), "当前题面应进 prompt");
        assertTrue(system.contains("参考要点"), "参考要点应进 prompt");
    }

    @Test
    void firstFreeTurnWithoutPositionAsksForIt() throws Exception {
        List<List<LlmMessage>> captured = new ArrayList<>();
        doAnswer(invocation -> {
            List<LlmMessage> messages = invocation.getArgument(0);
            captured.add(new ArrayList<>(messages));
            Consumer<String> onDelta = invocation.getArgument(2);
            onDelta.accept("【好奇】你好，今天想面什么岗位？");
            return null;
        }).when(llmClient).stream(anyList(), any(LlmOptions.class), any());

        CreateSessionResponse created = chatService.createSession(
                new CreateSessionRequest("strict", "java", null));
        chatService.processMessage(created.sessionId(), "你好");

        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        while (messageRepository.countBySessionIdAndRole(created.sessionId(), InterviewMessage.ROLE_ASSISTANT) == 0
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50L);
        }

        InterviewSession session = sessionRepository.findById(created.sessionId()).orElseThrow();
        assertNull(session.getFreeJobCode(), "没说岗位时先不落岗位");
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).get(0).content().contains("【自由面试 · 确认岗位】"),
                "首轮应让面试官先问岗位");
    }
}
