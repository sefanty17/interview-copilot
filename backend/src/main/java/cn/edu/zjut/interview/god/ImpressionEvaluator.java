package cn.edu.zjut.interview.god;

import cn.edu.zjut.interview.domain.InterviewMessage;
import cn.edu.zjut.interview.llm.LlmMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试印象评估：状态快照 + 评估 prompt + 结果解读。
 *
 * <p>早前从 LingChat 迁过来的六维是「好感 / 信赖 / 亲密 / 默契 / 兴趣 / 思念」，
 * 那是恋爱模拟里角色对玩家的感情，面试官不会用这些词记住一个人。这里整体换成
 * 招聘场景真正在判断的六个方向（专业性 / 表达力 / 逻辑性 / 深度 / 可信度 / 潜力），
 * 负面一侧换成面试官的心态（质疑 / 困惑 / 不耐 / 失望 / 戒备 / 顾虑）。
 *
 * <p>数值按「场次 + 面试官」存在 {@code session_impression} 表；评估是后台动作，
 * 失败只记日志，绝不影响已经呈现给候选人的回复。
 */
public final class ImpressionEvaluator {

    /** 数值上下限：模型偶发给出越界值时兜一层。 */
    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 100;

    private ImpressionEvaluator() {
    }

    /** 正面印象六维（0-100，越高越好）。 */
    public record ImpressionVector(int professionalism, int clarity, int logic,
                                   int depth, int credibility, int potential) {

        /** （序列化键名, 中文显示名），顺序即展示顺序。 */
        public static final List<String[]> DIMENSIONS = List.of(
                new String[]{"professionalism", "专业性"},
                new String[]{"clarity", "表达力"},
                new String[]{"logic", "逻辑性"},
                new String[]{"depth", "深度"},
                new String[]{"credibility", "可信度"},
                new String[]{"potential", "潜力"});

        /** 出场一律为 0：面试刚开始时面试官对候选人没有先入为主的判断。 */
        public static ImpressionVector initial() {
            return new ImpressionVector(0, 0, 0, 0, 0, 0);
        }

        public int average() {
            return (professionalism + clarity + logic + depth + credibility + potential) / 6;
        }

        public int get(String dimension) {
            return switch (dimension) {
                case "professionalism" -> professionalism;
                case "clarity" -> clarity;
                case "logic" -> logic;
                case "depth" -> depth;
                case "credibility" -> credibility;
                case "potential" -> potential;
                default -> 0;
            };
        }
    }

    /** 负面观感六维（0-100，越高越糟）。 */
    public record NegativeVector(int skepticism, int confusion, int impatience,
                                 int disappointment, int wariness, int reservation) {

        public static final List<String[]> DIMENSIONS = List.of(
                new String[]{"skepticism", "质疑"},
                new String[]{"confusion", "困惑"},
                new String[]{"impatience", "不耐"},
                new String[]{"disappointment", "失望"},
                new String[]{"wariness", "戒备"},
                new String[]{"reservation", "顾虑"});

        public static NegativeVector initial() {
            return new NegativeVector(0, 0, 0, 0, 0, 0);
        }

        public int get(String dimension) {
            return switch (dimension) {
                case "skepticism" -> skepticism;
                case "confusion" -> confusion;
                case "impatience" -> impatience;
                case "disappointment" -> disappointment;
                case "wariness" -> wariness;
                case "reservation" -> reservation;
                default -> 0;
            };
        }
    }

    /** 一次评估的完整结果：评估后的两组值 + 本次增量 + 一句理由。 */
    public record Result(ImpressionVector positive, NegativeVector negative,
                         Map<String, Integer> positiveDeltas,
                         Map<String, Integer> negativeDeltas,
                         String reason) {
    }

    /** 评估任务的 system 说明（面试口径）。 */
    public static String systemPrompt() {
        return String.join("\n",
                "你是一名资深技术面试官。请阅读候选人最近的表现，给出你此刻对这位候选人的判断。",
                "只输出一个 JSON 对象，不要输出解释、不要用代码块包裹。",
                "",
                "按 0-100 的绝对分打分（不是增量，不是加减分），越高越强：",
                "正面印象：",
                "- professionalism 专业性：基础是否扎实，概念、术语与结论是否准确",
                "- clarity 表达力：能否讲清楚，结构是否清楚，有没有把控好时长",
                "- logic 逻辑性：拆解问题的顺序、因果、取舍是否站得住",
                "- depth 深度：是否讲到原理、边界与权衡，而不是只背结论",
                "- credibility 可信度：经历与回答是否自洽，不夸大，不会就说不会",
                "- potential 潜力：学习能力、反思能力与可塑性",
                "",
                "负面观感（0 = 完全没有这种感受，100 = 非常强烈）：",
                "- skepticism 质疑：技术结论明显站不住脚，让你想当场反驳",
                "- confusion 困惑：你听不懂候选人的思路，需要反复追问",
                "- impatience 不耐：绕圈、跑题、抓不住重点",
                "- disappointment 失望：该会的基础题没答出来，低于预期",
                "- wariness 戒备：像在背题、套模板、经历注水",
                "- reservation 顾虑：你在犹豫要不要给通过，亮点不够或有硬伤",
                "",
                "评分参考：",
                "- 正面：40 以下明显不足，50-64 一般，65-74 合格，75-84 良好，85 以上强烈推荐",
                "- 负面：20 以下只是轻微疑虑，40 左右需要继续追问确认，60 以上已经明显影响评价",
                "",
                "评估原则：",
                "1. 只根据这段对话里真实出现的信息判断，不要凭空推测。",
                "2. 六个正面维度和六个负面维度都要给分，不要留空。",
                "3. 参考【当前印象】保持连续：除非本轮表现明显改变判断，否则不要让分数大幅跳动。",
                "4. reason 用一句话说明你给这个判断的主要依据。",
                "",
                "输出格式：",
                "{\"positive\":{\"professionalism\":70,\"clarity\":70,\"logic\":70,\"depth\":70,"
                        + "\"credibility\":70,\"potential\":70},"
                        + "\"negative\":{\"skepticism\":10,\"confusion\":10,\"impatience\":10,"
                        + "\"disappointment\":10,\"wariness\":10,\"reservation\":10},"
                        + "\"reason\":\"这个判断的一句话依据\"}");
    }

    /** 组给模型的对话：system + 「当前印象 + 最近对话」。 */
    public static List<LlmMessage> messages(InterviewerBrief interviewer,
                                            ImpressionVector positive,
                                            NegativeVector negative,
                                            List<InterviewMessage> lines) {
        return List.of(
                LlmMessage.system(systemPrompt()),
                LlmMessage.user(buildPayload(interviewer, positive, negative, lines)));
    }

    /** 面试官简介（只用来给评估提供人设背景）。 */
    public record InterviewerBrief(String name, String role) {
    }

    /** 数据载荷：面试官人设 + 当前印象 + 最近对话。 */
    public static String buildPayload(InterviewerBrief interviewer,
                                      ImpressionVector positive,
                                      NegativeVector negative,
                                      List<InterviewMessage> lines) {
        StringBuilder block = new StringBuilder();
        block.append("【面试官】")
                .append(interviewer.name() == null || interviewer.name().isBlank() ? "面试官" : interviewer.name());
        if (interviewer.role() != null && !interviewer.role().isBlank()) {
            block.append("（").append(interviewer.role()).append("）");
        }
        block.append('\n');

        block.append("【当前印象】\n");
        List<String> pos = new ArrayList<>();
        for (String[] dimension : ImpressionVector.DIMENSIONS) {
            pos.add(dimension[1] + " " + positive.get(dimension[0]));
        }
        block.append("正面：").append(String.join("、", pos)).append('\n');
        List<String> neg = new ArrayList<>();
        for (String[] dimension : NegativeVector.DIMENSIONS) {
            neg.add(dimension[1] + " " + negative.get(dimension[0]));
        }
        block.append("负面：").append(String.join("、", neg)).append('\n');
        block.append("（以上是上一次的评估结果；本次请重新给出 0-100 的绝对分）\n");

        block.append("【最近对话（由旧到新）】\n");
        if (lines.isEmpty()) {
            block.append("（无对话记录）\n");
        } else {
            for (InterviewMessage line : lines) {
                String name = line.getDisplayName() == null || line.getDisplayName().isBlank()
                        ? "候选人" : line.getDisplayName();
                block.append(name).append(": ")
                        .append(line.getContent() == null ? "" : line.getContent()).append('\n');
            }
        }
        return block.toString();
    }

    /**
     * 解析模型输出：值是 0-100 的绝对分，不是增量。
     *
     * <p>模型漏掉的维度沿用上一次的值；两组都没有任何有效维度时返回 {@code null}。
     *
     * @return 解析失败（不是 JSON / 没有有效维度）时返回 {@code null}，调用方按「本次不调整」处理
     */
    public static Result parse(String raw, ImpressionVector beforePositive,
                               NegativeVector beforeNegative, ObjectMapper mapper) {
        JsonNode root = readJson(raw, mapper);
        if (root == null) {
            return null;
        }
        Map<String, Integer> positiveScores = readScores(root.get("positive"), ImpressionVector.DIMENSIONS);
        Map<String, Integer> negativeScores = readScores(root.get("negative"), NegativeVector.DIMENSIONS);
        if (positiveScores.isEmpty() && negativeScores.isEmpty()) {
            return null;
        }
        ImpressionVector positive = new ImpressionVector(
                score(positiveScores, beforePositive, "professionalism"),
                score(positiveScores, beforePositive, "clarity"),
                score(positiveScores, beforePositive, "logic"),
                score(positiveScores, beforePositive, "depth"),
                score(positiveScores, beforePositive, "credibility"),
                score(positiveScores, beforePositive, "potential"));
        NegativeVector negative = new NegativeVector(
                score(negativeScores, beforeNegative, "skepticism"),
                score(negativeScores, beforeNegative, "confusion"),
                score(negativeScores, beforeNegative, "impatience"),
                score(negativeScores, beforeNegative, "disappointment"),
                score(negativeScores, beforeNegative, "wariness"),
                score(negativeScores, beforeNegative, "reservation"));
        String reason = root.path("reason").isTextual() ? root.path("reason").asText() : "";
        return new Result(positive, negative,
                deltas(positiveScores, ImpressionVector.DIMENSIONS, beforePositive::get),
                deltas(negativeScores, NegativeVector.DIMENSIONS, beforeNegative::get),
                reason);
    }

    /** 容忍模型把 JSON 包在 ```json 里，或多说了一句前缀。 */
    private static JsonNode readJson(String raw, ObjectMapper mapper) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.strip();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return mapper.readTree(text.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Integer> readScores(JsonNode node, List<String[]> dimensions) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return scores;
        }
        for (String[] dimension : dimensions) {
            JsonNode value = node.get(dimension[0]);
            if (value == null || !value.canConvertToInt()) {
                continue;
            }
            scores.put(dimension[0], clamp(value.asInt()));
        }
        return scores;
    }

    private static int score(Map<String, Integer> scores, ImpressionVector before, String key) {
        Integer value = scores.get(key);
        return value == null ? before.get(key) : value;
    }

    private static int score(Map<String, Integer> scores, NegativeVector before, String key) {
        Integer value = scores.get(key);
        return value == null ? before.get(key) : value;
    }

    /** 本次分数与上一次的差，用于面板「最近变化」。 */
    private static Map<String, Integer> deltas(Map<String, Integer> scores,
                                               List<String[]> dimensions,
                                               java.util.function.Function<String, Integer> before) {
        Map<String, Integer> deltas = new LinkedHashMap<>();
        for (String[] dimension : dimensions) {
            Integer score = scores.get(dimension[0]);
            if (score == null) {
                continue;
            }
            int delta = score - before.apply(dimension[0]);
            if (delta != 0) {
                deltas.put(dimension[0], delta);
            }
        }
        return deltas;
    }

    private static int clamp(int value) {
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
    }
}
