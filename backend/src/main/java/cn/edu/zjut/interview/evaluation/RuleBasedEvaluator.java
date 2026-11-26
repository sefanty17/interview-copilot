package cn.edu.zjut.interview.evaluation;

import cn.edu.zjut.interview.domain.InterviewMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 规则估分：大模型评估不可用时的兜底（issue #5）。
 *
 * <p>为什么要有这条兜底：评估是报告页的核心内容，大模型偶发超时 / 限流 / 返回不可解析的 JSON 时，
 * 如果整页 502，用户连「这场的维度分数大致在什么水平」都看不到。规则估分只用对话本身可观测的特征
 * （轮次、篇幅、连接词、技术术语），不调网络，因此不会二次失败。
 *
 * <p><b>它不是「假装 AI 评过」</b>：结果里的 {@code model} 固定为 {@link #MODEL}，
 * 报告页据此打「本次为规则估分」标注，总分求和口径与 AI 评估一致（同一套权重）。
 *
 * <p>打分刻意压在 40~85 区间：规则看到的只是「回答得像不像在认真答」，
 * 看不出技术内容对错，给到 90+ 就是虚假承诺。
 */
public final class RuleBasedEvaluator {

    /** 落库到 {@code interview_evaluation.model} 的标记值，前端按它显示规则估分标注。 */
    public static final String MODEL = "rule-based";

    /** 兜底结果的固定总评：把「为什么是估分」写清楚，不让用户误以为是模型评的。 */
    public static final String SUMMARY =
            "本次为规则估分：大模型评估暂不可用，分数依据回答轮次、篇幅与论述结构估算，仅供参考。";

    /** 判断「有没有在讲因果 / 层次」的连接词。不追求语言学完备，只求能区分「一句结论」与「展开论述」。 */
    private static final Set<String> CONNECTIVES = Set.of(
            "因为", "所以", "因此", "首先", "其次", "最后", "例如", "比如",
            "但是", "不过", "另外", "一方面", "另一方面", "总结", "区别", "原理");

    /** 技术术语的近似判据：连续两个及以上拉丁字母（JVM、GC、Spring、HTTP 等）。 */
    private static final Pattern LATIN_TERM = Pattern.compile("[a-z]{2,}", Pattern.CASE_INSENSITIVE);

    /** 作答轮次达到这个数量就算「样本充分」，再多不叠加加分。 */
    private static final int FULL_COVERAGE_ANSWERS = 6;

    /** 单题回答达到这个字数就算「篇幅完整」，再多不叠加加分。 */
    private static final int FULL_LENGTH_CHARS = 120;

    private RuleBasedEvaluator() {
    }

    /**
     * 按对话特征生成一份评估草稿。
     *
     * @param dialogue 本场对话（含面试官与求职者；system 行由调用方排除，这里再兜一次）
     */
    public static EvaluationParser.EvaluationDraft draft(List<InterviewMessage> dialogue) {
        List<String> answers = userAnswers(dialogue);
        int count = answers.size();

        int totalChars = 0;
        int structured = 0;
        int withTerm = 0;
        for (String answer : answers) {
            totalChars += answer.length();
            if (containsConnective(answer)) {
                structured++;
            }
            if (LATIN_TERM.matcher(answer).find()) {
                withTerm++;
            }
        }

        double avgChars = count == 0 ? 0.0 : (double) totalChars / count;
        double coverage = clamp01((double) count / FULL_COVERAGE_ANSWERS);
        double completeness = clamp01(avgChars / FULL_LENGTH_CHARS);
        double structure = count == 0 ? 0.0 : (double) structured / count;
        double terminology = count == 0 ? 0.0 : (double) withTerm / count;

        Map<String, EvaluationParser.DimScore> dimensions = new LinkedHashMap<>();
        dimensions.put(EvaluationDimension.TECHNICAL.code(), new EvaluationParser.DimScore(
                score(45 + 25 * terminology + 10 * coverage),
                "规则估分：按技术术语的使用情况估算，未校验内容对错"));
        dimensions.put(EvaluationDimension.DEPTH.code(), new EvaluationParser.DimScore(
                score(40 + 30 * completeness + 15 * terminology),
                "规则估分：按单题回答篇幅估算，篇幅长不等于讲得深"));
        dimensions.put(EvaluationDimension.LOGIC.code(), new EvaluationParser.DimScore(
                score(45 + 30 * structure + 10 * completeness),
                "规则估分：按因果与层次连接词的使用估算"));
        dimensions.put(EvaluationDimension.FIT.code(), new EvaluationParser.DimScore(
                score(50 + 20 * coverage + 10 * completeness),
                "规则估分：按作答轮次与篇幅估算，未结合简历比对"));

        int weighted = 0;
        for (EvaluationDimension dimension : EvaluationDimension.all()) {
            weighted += dimensions.get(dimension.code()).score() * dimension.weight();
        }
        return new EvaluationParser.EvaluationDraft(SUMMARY,
                strengths(coverage, completeness, structure, terminology),
                weaknesses(coverage, completeness, structure, terminology),
                suggestions(coverage, completeness, structure, terminology),
                dimensions, List.of(), (int) Math.round(weighted));
    }

    /** 只取求职者回答；空白回答不算一轮（说不出内容不构成评估样本）。 */
    private static List<String> userAnswers(List<InterviewMessage> dialogue) {
        List<String> answers = new ArrayList<>();
        if (dialogue == null) {
            return answers;
        }
        for (InterviewMessage message : dialogue) {
            if (!InterviewMessage.ROLE_USER.equals(message.getRole())) {
                continue;
            }
            String content = message.getContent() == null ? "" : message.getContent().strip();
            if (!content.isEmpty()) {
                answers.add(content);
            }
        }
        return answers;
    }

    private static boolean containsConnective(String answer) {
        String normalized = answer.toLowerCase(Locale.ROOT);
        for (String word : CONNECTIVES) {
            if (normalized.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> strengths(double coverage, double completeness,
                                          double structure, double terminology) {
        List<String> values = new ArrayList<>();
        if (coverage >= 0.7) {
            values.add("作答轮次充分，样本量足够看出水平");
        }
        if (completeness >= 0.5) {
            values.add("单题回答有展开，不是一两句话带过");
        }
        if (structure >= 0.5) {
            values.add("回答里能听到因果与层次，论述有结构");
        }
        if (terminology >= 0.3) {
            values.add("能主动使用技术术语");
        }
        if (values.isEmpty()) {
            values.add("完成了整场问答，具备继续练习的基础");
        }
        return List.copyOf(values);
    }

    private static List<String> weaknesses(double coverage, double completeness,
                                           double structure, double terminology) {
        List<String> values = new ArrayList<>();
        if (coverage < 0.5) {
            values.add("作答轮次偏少，样本不足以反映真实水平");
        }
        if (completeness < 0.5) {
            values.add("单题回答偏短，没把原理或取舍讲开");
        }
        if (structure < 0.5) {
            values.add("论述缺少因果与层次，结论没有论据支撑");
        }
        if (terminology < 0.3) {
            values.add("技术术语使用较少，表达偏口语");
        }
        if (values.isEmpty()) {
            values.add("规则估分看不到内容对错，深度仍需人工复核");
        }
        return List.copyOf(values);
    }

    private static List<String> suggestions(double coverage, double completeness,
                                            double structure, double terminology) {
        List<String> values = new ArrayList<>();
        if (structure < 0.5) {
            values.add("先给结论，再补原因与取舍，用「因为/所以/相比」把逻辑串起来");
        }
        if (completeness < 0.5) {
            values.add("单题回答补到 3~5 句：定义、原理、适用场景各说一句");
        }
        if (terminology < 0.3) {
            values.add("多提具体技术名词与版本差异，减少「那个东西」这类指代");
        }
        if (coverage < 0.5) {
            values.add("多练几场把作答轮次提上来，趋势才有参考价值");
        }
        if (values.isEmpty()) {
            values.add("恢复大模型后重新评估一次，拿到带逐题复盘的完整报告");
        }
        return List.copyOf(values);
    }

    /** 四舍五入并夹到 0~100，避免浮点累加把分数推出展示区间。 */
    private static int score(double raw) {
        return (int) Math.max(0, Math.min(100, Math.round(raw)));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
