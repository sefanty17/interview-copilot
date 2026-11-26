package cn.edu.zjut.interview.dto;

/**
 * {@code POST /api/sessions} 的请求体（规格 B3-BE §1.1，规格 F-1 §B6）。
 *
 * @param interviewerCode 角色卡编码，对应 {@code interviewer.code}
 * @param jobId           岗位编码，对应 {@code interview_script.job_code}
 * @param scriptCode      剧本编码，对应 {@code interview_script.code}；为 null/空白时
 *                        不绑剧本（纯自由对话），要演剧本走 {@code POST /{id}/script/start}
 * @param focusCode       专项维度 code（technical / depth / logic / fit）；从练习计划进入时带上
 * @param focusLabel      专项名称，如「岗位匹配度」；为空表示本场不是专项训练
 * @param focusBrief      专项训练要点（练习计划当天的 tasks 拼串），拼进 system 引导深挖
 */
public record CreateSessionRequest(String interviewerCode, String jobId, String scriptCode,
                                   String focusCode, String focusLabel, String focusBrief) {

    /** 不带专项的旧调用口径（测试 / 试玩等）。 */
    public CreateSessionRequest(String interviewerCode, String jobId, String scriptCode) {
        this(interviewerCode, jobId, scriptCode, null, null, null);
    }
}
