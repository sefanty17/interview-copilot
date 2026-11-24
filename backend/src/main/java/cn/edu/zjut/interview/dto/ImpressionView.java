package cn.edu.zjut.interview.dto;

import java.util.Map;

/**
 * 面试印象的对外视图。
 *
 * @param roleId            面试官 id（一个场次里可能有多个面试官，各存一份）
 * @param evaluated         是否已产生过至少一次评估；{@code false} 时前端显示"待观察"
 * @param evaluatedUserCount 最近一次评估时的用户消息条数
 * @param positive          正面六维当前值（键为维度代号）
 * @param negative          负面六维当前值
 * @param positiveDeltas    本次评估的正面增量（刷新时为上次的增量）
 * @param negativeDeltas    本次评估的负面增量
 * @param reason            本次评估的一句话原因
 */
public record ImpressionView(int roleId,
                             boolean evaluated,
                             int evaluatedUserCount,
                             Map<String, Integer> positive,
                             Map<String, Integer> negative,
                             Map<String, Integer> positiveDeltas,
                             Map<String, Integer> negativeDeltas,
                             String reason) {
}
