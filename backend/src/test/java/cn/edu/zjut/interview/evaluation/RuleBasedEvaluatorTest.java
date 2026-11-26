package cn.edu.zjut.interview.evaluation;

import cn.edu.zjut.interview.domain.InterviewMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 规则估分的单测：不依赖 Spring、不调网络，只验证「特征 → 分数」的趋势与边界。
 *
 * <p>这些断言不是为了把某个具体分数钉死（阈值一调就会全红），而是保证方向正确：
 * 结构化、篇幅长的回答必须比一句话回答得分高；任何输入都落在 0~100。
 */
class RuleBasedEvaluatorTest {

    /** 1. 没有任何回答：不抛异常，四个维度都在，总分在合法区间，且有兜底文案。 */
    @Test
    void 空对话也能给出合法草稿() {
        var draft = RuleBasedEvaluator.draft(List.of());

        assertEquals(4, draft.dimensions().size());
        assertTrue(draft.overall() >= 0 && draft.overall() <= 100, "总分越界：" + draft.overall());
        assertFalse(draft.strengths().isEmpty(), "亮点不能为空，前端会直接渲染");
        assertFalse(draft.weaknesses().isEmpty());
        assertFalse(draft.suggestions().isEmpty());
        assertTrue(draft.questions().isEmpty(), "规则估分没有逐题复盘");
        assertTrue(draft.summary().contains("规则估分"), draft.summary());
        assertNotNull(draft.dimensions().get(EvaluationDimension.TECHNICAL.code()));
    }

    /** 2. 展开论述的回答，深度与逻辑分必须高于一句话回答。 */
    @Test
    void 结构化长回答比一句话回答得分高() {
        var terse = RuleBasedEvaluator.draft(dialogue(
                "不知道", "没了解过", "大概是吧"));
        var detailed = RuleBasedEvaluator.draft(dialogue(
                "首先 JVM 把内存分成堆和栈，因为堆放对象、栈放栈帧，所以 GC 主要扫堆。",
                "例如 Spring 的 AOP 用代理实现，但是同类内部调用不经过代理，因此事务会失效。",
                "HashMap 在 JDK8 里是数组加链表，链表超过阈值就转红黑树，相比 JDK7 的头插改成了尾插。"));

        assertTrue(detailed.dimensions().get(EvaluationDimension.DEPTH.code()).score()
                        > terse.dimensions().get(EvaluationDimension.DEPTH.code()).score(),
                "篇幅长、术语多的回答深度分应更高");
        assertTrue(detailed.dimensions().get(EvaluationDimension.LOGIC.code()).score()
                        > terse.dimensions().get(EvaluationDimension.LOGIC.code()).score(),
                "带因果/层次的回答逻辑分应更高");
        assertTrue(detailed.overall() > terse.overall(), "整体也要更高");
    }

    /** 3. system 行与面试官台词不参与特征统计：加入了也不改变结果。 */
    @Test
    void 只统计求职者回答() {
        List<InterviewMessage> messages = new ArrayList<>();
        messages.add(message(1, InterviewMessage.ROLE_SYSTEM, "你是面试官"));
        messages.add(message(2, InterviewMessage.ROLE_ASSISTANT, "请讲讲 JVM 内存模型"));
        messages.add(message(3, InterviewMessage.ROLE_USER, "堆、栈、方法区"));
        messages.add(message(4, InterviewMessage.ROLE_USER, "   "));

        var withNoise = RuleBasedEvaluator.draft(messages);
        var onlyAnswer = RuleBasedEvaluator.draft(List.of(
                message(3, InterviewMessage.ROLE_USER, "堆、栈、方法区")));

        assertEquals(onlyAnswer, withNoise, "system / 面试官 / 空白回答都不该影响分数");
    }

    private static List<InterviewMessage> dialogue(String... answers) {
        List<InterviewMessage> messages = new ArrayList<>();
        int seq = 1;
        for (String answer : answers) {
            messages.add(message(seq++, InterviewMessage.ROLE_ASSISTANT, "下一个问题"));
            messages.add(message(seq++, InterviewMessage.ROLE_USER, answer));
        }
        return messages;
    }

    private static InterviewMessage message(int seq, String role, String content) {
        InterviewMessage message = new InterviewMessage();
        message.setSessionId(1L);
        message.setSeq(seq);
        message.setRole(role);
        message.setContent(content);
        return message;
    }
}
