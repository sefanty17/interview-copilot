package cn.edu.zjut.interview.question;

import java.util.Locale;

/**
 * 题库的四种题型（本项目扩展，课设指标 ①）。
 *
 * <p>为什么要有这个枚举而不是直接比较字符串：题库里 {@code type} 列存的是<b>中文标签</b>
 * （「技术知识」这种，种子数据与前端 {@code types/domain.ts} 的 {@code Question.type}
 * 都是中文），而接口的 {@code ?type=} 参数外部调用方习惯传英文。两者必须能对上，
 * 又不能让「未知题型」静默返回空列表——那就得有一个地方同时持有 code 与 label。
 *
 * <p>与 PR #20 的差别（刻意不同）：那边把 {@code type_code} 另存一列英文，
 * 本项目沿用已有的 {@code type} 中文列，不新增列——{@code schema-h2.sql} 与
 * {@code FreeInterviewService} 都已经按中文取值在跑，加列会双写两份真相。
 */
public enum QuestionType {

    /** 技术知识：概念、原理、源码。 */
    TECHNICAL("technical", "技术知识"),
    /** 项目深挖：从简历 / 项目出发追问实现细节与取舍。 */
    PROJECT("project", "项目深挖"),
    /** 场景题：给定业务场景做设计或排查。 */
    SCENARIO("scenario", "场景题"),
    /** 行为题：协作、冲突、复盘等软素质。 */
    BEHAVIOR("behavior", "行为题");

    private final String code;
    private final String label;

    QuestionType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /** 英文 code，接口文档与前端传参用。 */
    public String code() {
        return code;
    }

    /** 中文标签，与库里 {@code interview_question.type} 的取值逐字一致。 */
    public String label() {
        return label;
    }

    /**
     * 解析接口的 {@code ?type=}：英文 code（大小写不敏感）与中文标签都认。
     *
     * @return {@code null} 表示没传，不加题型过滤
     * @throws IllegalArgumentException 未知取值，由控制器转成 400 的中文提示
     */
    public static QuestionType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        for (QuestionType type : values()) {
            if (type.code.equals(text.toLowerCase(Locale.ROOT)) || type.label.equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "未知的题型：" + text + "（可选 technical / project / scenario / behavior，或对应中文）");
    }

    /**
     * 中文标签 → 英文 code；库里存了未知取值时原样返回。
     *
     * <p>不做成抛异常：列表接口的展示字段不该因为一条脏数据整份 500。
     */
    public static String codeOf(String label) {
        if (label != null) {
            for (QuestionType type : values()) {
                if (type.label.equals(label.trim())) {
                    return type.code;
                }
            }
        }
        return label;
    }
}
