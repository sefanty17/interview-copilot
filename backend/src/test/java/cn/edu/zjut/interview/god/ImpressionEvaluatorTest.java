package cn.edu.zjut.interview.god;

import cn.edu.zjut.interview.domain.InterviewMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 面试印象：维度口径、绝对分解析、模型输出容错。
 *
 * <p>核心验收点：不再是「好感 / 亲密 / 思念」那一套恋爱词；模型给的是 0-100 绝对分，
 * 解析后不会越界，漏给的维度沿用上一次的值。
 */
class ImpressionEvaluatorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void dimensionsUseInterviewVocabulary() {
        List<String[]> positive = ImpressionEvaluator.ImpressionVector.DIMENSIONS;
        List<String[]> negative = ImpressionEvaluator.NegativeVector.DIMENSIONS;

        assertEquals(6, positive.size());
        assertEquals(6, negative.size());
        assertEquals("professionalism", positive.get(0)[0]);
        assertEquals("专业性", positive.get(0)[1]);
        assertEquals("潜力", positive.get(5)[1]);
        assertEquals("质疑", negative.get(0)[1]);
        assertEquals("顾虑", negative.get(5)[1]);
    }

    @Test
    void allDimensionsStartAtZero() {
        ImpressionEvaluator.ImpressionVector positive = ImpressionEvaluator.ImpressionVector.initial();
        ImpressionEvaluator.NegativeVector negative = ImpressionEvaluator.NegativeVector.initial();

        for (String[] dimension : ImpressionEvaluator.ImpressionVector.DIMENSIONS) {
            assertEquals(0, positive.get(dimension[0]), dimension[0] + " 出场应为 0");
        }
        for (String[] dimension : ImpressionEvaluator.NegativeVector.DIMENSIONS) {
            assertEquals(0, negative.get(dimension[0]), dimension[0] + " 出场应为 0");
        }
    }

    @Test
    void parseToleratesCodeFenceAndClampsOutOfRangeScores() {
        String raw = """
                ```json
                {"positive":{"professionalism":120,"depth":63},"negative":{"impatience":90},"reason":"原理讲清楚了"}
                ```""";
        ImpressionEvaluator.Result result = ImpressionEvaluator.parse(raw,
                ImpressionEvaluator.ImpressionVector.initial(),
                ImpressionEvaluator.NegativeVector.initial(), mapper);

        assertNotNull(result);
        assertEquals(100, result.positive().professionalism(), "绝对分上界 100");
        assertEquals(63, result.positive().depth());
        assertEquals(90, result.negative().impatience());
        assertEquals("原理讲清楚了", result.reason());
        assertEquals(100, result.positiveDeltas().get("professionalism"), "变化 = 本次分 - 上次分");
    }

    @Test
    void missingDimensionsKeepPreviousScores() {
        ImpressionEvaluator.ImpressionVector before = new ImpressionEvaluator.ImpressionVector(
                70, 60, 65, 60, 70, 65);
        String raw = """
                {"positive":{"clarity":80},"negative":{}}""";

        ImpressionEvaluator.Result result = ImpressionEvaluator.parse(raw, before,
                ImpressionEvaluator.NegativeVector.initial(), mapper);

        assertNotNull(result);
        assertEquals(70, result.positive().professionalism(), "没给的维度沿用上次");
        assertEquals(80, result.positive().clarity());
        assertEquals(20, result.positiveDeltas().get("clarity"));
    }

    @Test
    void parseReturnsNullForInvalidOrEmptyOutput() {
        ImpressionEvaluator.ImpressionVector positive = ImpressionEvaluator.ImpressionVector.initial();
        ImpressionEvaluator.NegativeVector negative = ImpressionEvaluator.NegativeVector.initial();

        assertNull(ImpressionEvaluator.parse("我不知道", positive, negative, mapper), "非 JSON 视为本次不调整");
        assertNull(ImpressionEvaluator.parse("{\"positive\":{},\"negative\":{}}", positive, negative, mapper),
                "一个维度都没给就不算一次评估");
    }

    @Test
    void payloadCarriesCurrentValuesAndRecentLines() {
        InterviewMessage line = new InterviewMessage();
        line.setRole(InterviewMessage.ROLE_USER);
        line.setContent("我用 AQS 实现过限流");
        line.setDisplayName("候选人");

        String payload = ImpressionEvaluator.buildPayload(
                new ImpressionEvaluator.InterviewerBrief("张老师", "后端面试官"),
                ImpressionEvaluator.ImpressionVector.initial(),
                ImpressionEvaluator.NegativeVector.initial(),
                List.of(line));

        assertTrue(payload.contains("张老师"), "要带面试官名字");
        assertTrue(payload.contains("专业性 0"), "要带当前正面维度");
        assertTrue(payload.contains("不耐 0"), "要带当前负面维度");
        assertTrue(payload.contains("我用 AQS 实现过限流"), "要带最近的对话");
    }

    @Test
    void systemPromptIsInterviewNotRomance() {
        String prompt = ImpressionEvaluator.systemPrompt();
        assertTrue(prompt.contains("专业性"));
        assertTrue(prompt.contains("credibility"));
        assertTrue(!prompt.contains("好感"), "不能再出现恋爱模拟的维度名");
        assertTrue(!prompt.contains("亲密"));
    }
}
