package cn.edu.zjut.interview.worldbook;

import cn.edu.zjut.interview.domain.InterviewSession;
import cn.edu.zjut.interview.llm.LlmMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按会话岗位注入世界书（issue #1 的最后一块）。
 *
 * <p>与 {@code KnowledgeInjector} 分工：这里管「固定岗位背景」（公司、团队、职责），
 * 那边管按回答检索的知识片段；两者互不感知，都只改本轮请求副本，不写台词表。
 *
 * <p>自由对话（ChatService）与剧本面试（引擎的 ai_dialogue / free_dialogue）共用这一个入口。
 */
@Component
public class WorldbookInjector {

    private final WorldbookLoader loader;

    public WorldbookInjector(WorldbookLoader loader) {
        this.loader = loader;
    }

    /**
     * @param userInput 第 1 轮传玩家输入（触发 keyword 条目），之后传 null（只注入 always）
     */
    public void inject(InterviewSession session, List<LlmMessage> messages, String userInput) {
        if (session == null || messages == null || messages.isEmpty()) {
            return;
        }
        Worldbook book = loader.forJob(session.getJobCode());
        if (book != null) {
            book.inject(messages, userInput);
        }
    }
}
