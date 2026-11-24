package cn.edu.zjut.interview.dto;

import cn.edu.zjut.interview.domain.InterviewQuestion;
import cn.edu.zjut.interview.question.QuestionType;

import java.util.List;

/**
 * 题库里的一道题（{@code GET /api/questions} 的返回元素）。
 *
 * <p>字段与前端 {@code types/domain.ts} 的 {@code Question} 对齐，前端拿到后只需
 * 把 {@code code} 当 id、{@code jobCode} 当 jobId 就能直接塞进现有题池逻辑，
 * 不需要再写一层字段映射。
 *
 * @param id         库内主键，前端做 key 用
 * @param code       稳定业务编码（{@code j1} / {@code fe-vue-01}），幂等种子的 MERGE KEY
 * @param jobCode    岗位编码，与 {@code interview_script.job_code} 同一套值
 * @param kp         知识点编码，与前端 {@code data/jobs.ts} 的 knowledgePoints.id 对齐
 * @param type       中文题型（技术知识 / 项目深挖 / 场景题 / 行为题），与库列同值
 * @param typeCode   英文题型（technical / project / scenario / behavior），供前端做过滤
 * @param difficulty 难度（简单 / 中等 / 较难）
 * @param text       题干
 * @param reference  参考要点。<b>只在后端进 prompt 用</b>，这里仍下发是因为题库页
 *                   需要展示「参考答案」给用户自查；自由面试链路不会把它说给候选人
 * @param followUps  备选追问方向
 */
public record QuestionView(
        Long id,
        String code,
        String jobCode,
        String kp,
        String type,
        String typeCode,
        String difficulty,
        String text,
        String reference,
        List<String> followUps) {

    public static QuestionView of(InterviewQuestion question, List<String> followUps) {
        return new QuestionView(
                question.getId(),
                question.getCode(),
                question.getJobCode(),
                question.getKp(),
                question.getType(),
                QuestionType.codeOf(question.getType()),
                question.getDifficulty(),
                question.getText(),
                question.getReference(),
                followUps);
    }
}
