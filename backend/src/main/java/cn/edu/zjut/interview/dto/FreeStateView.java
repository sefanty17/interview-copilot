package cn.edu.zjut.interview.dto;

/**
 * {@code GET /api/sessions/{id}/free-state} 与 SSE {@code ai:question} 的返回体（issue #11）。
 *
 * @param free          当前场次是否自由面试（剧本场次为 false，其余字段全空 / 0）
 * @param jobCode       从候选人自述里识别出的岗位编码；还没说清时为 null
 * @param jobName       岗位中文名（前端抬头直接展示）
 * @param askedCount    本场已抽中的题目数（含正在聊的这道）
 * @param questionCount 该岗位题库总数
 * @param question      当前题目；未识别岗位时为 null
 */
public record FreeStateView(
        boolean free,
        String jobCode,
        String jobName,
        int askedCount,
        int questionCount,
        QuestionView question) {

    /** 前端需要的题目元信息；不含参考答案。 */
    public record QuestionView(Long id, String kp, String type, String difficulty, String text) {
    }
}
