package cn.edu.zjut.interview.dto;

import java.util.List;

/**
 * 成长曲线 + 提升路径的响应体（<b>本项目扩展（课设指标 ⑥）</b>）。
 *
 * <p>点按时间升序排列，前端直接按顺序画折线，不再自己排序。
 *
 * @param requiredPoints   画趋势线所需的最少场次数（低于它就显示「数据不足」）
 * @param points           只有剧本场次且已产出评估的场次才会出现
 * @param recommendations  按最近几场的短板维度推荐的知识库条目，可为空
 * @param practicePlan     3 天练习计划（最弱两维度各一天 + 一天综合），可为空
 */
public record TrendView(
        int requiredPoints,
        List<TrendPoint> points,
        List<Recommendation> recommendations,
        List<PracticeDay> practicePlan) {

    /**
     * 一场面试在曲线上的一个点。
     *
     * @param date     取结束时间（没有就取开始/创建时间），形如 {@code 2026-10-29 14:23:25}
     * @param jobCode  该场岗位，推荐语料按它限定范围
     * @param dimensions 四个维度，复用评估结果的 {@link EvaluationResult.DimensionScore}
     */
    public record TrendPoint(
            Long sessionId,
            String date,
            String title,
            String jobCode,
            Integer overallScore,
            List<EvaluationResult.DimensionScore> dimensions) {
    }

    /**
     * 一个短板维度的推荐清单。
     *
     * @param averageScore 最近几场该维度的平均分，用来解释「为什么推荐它」
     * @param query        实际拿去检索的查询串（维度评语优先），方便答辩时对着讲
     * @param hits         命中的知识库片段；知识库为空时是空数组
     */
    public record Recommendation(
            String dimCode,
            String label,
            int averageScore,
            String query,
            List<KnowledgeHit> hits) {
    }

    /**
     * 3 天练习计划里的一天。
     *
     * <p>第 1、2 天各攻一个最弱维度，第 3 天综合复盘——把两个短板放进一场完整面试，
     * 再去回看上一场报告。由后端算好「练什么」而不是前端硬编码，
     * 是为了让计划跟着评估数据变，而不是一张永远不变的静态清单。
     *
     * @param day        第几天（1 起）
     * @param dimCode    针对的维度 code；第 3 天是综合，为 null
     * @param label      维度中文名；第 3 天为「综合复盘」
     * @param title      这一天的主题（一句话）
     * @param tasks      具体动作，2~3 条
     * @param entryRoute 前端路由，前端直接 push
     * @param entryLabel 入口按钮文案
     */
    public record PracticeDay(
            int day,
            String dimCode,
            String label,
            String title,
            List<String> tasks,
            String entryRoute,
            String entryLabel) {
    }
}
