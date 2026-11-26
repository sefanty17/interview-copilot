package cn.edu.zjut.interview.report;

import cn.edu.zjut.interview.domain.InterviewEvaluation;
import cn.edu.zjut.interview.domain.InterviewEvaluationDimension;
import cn.edu.zjut.interview.domain.InterviewSession;
import cn.edu.zjut.interview.dto.EvaluationResult.DimensionScore;
import cn.edu.zjut.interview.dto.KnowledgeHit;
import cn.edu.zjut.interview.dto.TrendView;
import cn.edu.zjut.interview.dto.TrendView.PracticeDay;
import cn.edu.zjut.interview.dto.TrendView.Recommendation;
import cn.edu.zjut.interview.dto.TrendView.TrendPoint;
import cn.edu.zjut.interview.engine.InterviewEngine;
import cn.edu.zjut.interview.evaluation.EvaluationDimension;
import cn.edu.zjut.interview.knowledge.RetrievalService;
import cn.edu.zjut.interview.repository.InterviewEvaluationDimensionRepository;
import cn.edu.zjut.interview.repository.InterviewEvaluationRepository;
import cn.edu.zjut.interview.repository.InterviewSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 成长曲线与提升路径（<b>本项目扩展（课设指标 ⑥）</b>）。
 *
 * <p>数据只有两处：{@code interview_session}（剧本场次、结束时间、总分）与
 * {@code interview_evaluation_dimension}（四维度分）。不调大模型——趋势是既成事实的汇总，
 * 再调一次模型只会让同一个数字每次刷新都变。
 *
 * <p><b>只收剧本场次</b>：自由对话 {@code script_id} 为空、也不产生评估，
 * 混进来会把一条没有分数的空点插进曲线，让趋势失真。预览（试玩）场次同理排除。
 *
 * <p>提升路径按「最近几场里平均分最低的两个维度」找知识库条目。查询串优先用该维度的
 * 模型评语（里面有具体短板，如「Spring 事务传播级别没区分」），评语为空才退回维度名——
 * 这一步是让推荐落在用户真实缺陷上，而不是笼统的「加强基础」。
 */
@Service
public class TrendService {

    private static final Logger log = LoggerFactory.getLogger(TrendService.class);

    /** 画折线所需的最少场次：两个点只有一条线段，看不出起伏，三次才谈得上趋势。 */
    public static final int REQUIRED_POINTS = 3;

    /** 短板取最近这么多场，越久远的表现越不该影响「现在该补什么」。 */
    private static final int RECENT_WINDOW = 5;

    /** 只推荐两个短板，多了用户不会看。 */
    private static final int RECOMMEND_COUNT = 2;

    /** 每个短板推荐几条知识片段。 */
    private static final int TOP_K = 3;

    /** 评语当查询串时截断长度：BM25 对超长查询会命中过多词，排序反而更散。 */
    private static final int MAX_QUERY_LENGTH = 120;

    /** 评语嵌进练习计划任务时的截断长度：太长会把卡片撑爆，60 字够说清一个具体缺陷。 */
    private static final int COMMENT_IN_TASK = 60;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final InterviewSessionRepository sessionRepository;
    private final InterviewEvaluationRepository evaluationRepository;
    private final InterviewEvaluationDimensionRepository dimensionRepository;
    private final RetrievalService retrievalService;

    public TrendService(InterviewSessionRepository sessionRepository,
                        InterviewEvaluationRepository evaluationRepository,
                        InterviewEvaluationDimensionRepository dimensionRepository,
                        RetrievalService retrievalService) {
        this.sessionRepository = sessionRepository;
        this.evaluationRepository = evaluationRepository;
        this.dimensionRepository = dimensionRepository;
        this.retrievalService = retrievalService;
    }

    /**
     * 趋势 + 推荐。
     *
     * @param userId 为空表示不按用户过滤（本项目当前是单用户，前端不传）
     */
    @Transactional(readOnly = true)
    public TrendView trend(Long userId) {
        List<InterviewSession> sessions = sessionRepository
                .findByStatusAndPreviewFalseOrderByCreatedAtDesc(InterviewEngine.STATUS_FINISHED)
                .stream()
                .filter(session -> session.getScriptId() != null)
                .filter(session -> userId == null || userId.equals(session.getUserId()))
                .sorted(Comparator.comparing(this::timeline))
                .toList();
        if (sessions.isEmpty()) {
            return new TrendView(REQUIRED_POINTS, List.of(), List.of(), List.of());
        }

        List<Long> sessionIds = new ArrayList<>(sessions.size());
        for (InterviewSession session : sessions) {
            sessionIds.add(session.getId());
        }
        Map<Long, InterviewEvaluation> evaluationBySession = new HashMap<>();
        for (InterviewEvaluation evaluation : evaluationRepository.findBySessionIdIn(sessionIds)) {
            evaluationBySession.put(evaluation.getSessionId(), evaluation);
        }
        Map<Long, List<InterviewEvaluationDimension>> dimensionsBySession =
                groupBySession(dimensionRepository.findBySessionIdInOrderByIdAsc(sessionIds));

        List<TrendPoint> points = new ArrayList<>(sessions.size());
        for (InterviewSession session : sessions) {
            InterviewEvaluation evaluation = evaluationBySession.get(session.getId());
            // 还没评估的场次没有分数，画上去就是一个 0 分点，比不画更误导
            if (evaluation == null || evaluation.getOverallScore() == null) {
                continue;
            }
            points.add(new TrendPoint(
                    session.getId(),
                    DATE_FORMAT.format(timeline(session)),
                    session.getTitle(),
                    evaluation.getJobCode() == null ? session.getJobCode() : evaluation.getJobCode(),
                    evaluation.getOverallScore(),
                    toScores(dimensionsBySession.getOrDefault(session.getId(), List.of()))));
        }
        List<Weakness> weaknesses = weaknesses(points);
        String jobCode = points.isEmpty() ? null : points.get(points.size() - 1).jobCode();
        return new TrendView(REQUIRED_POINTS, List.copyOf(points),
                recommend(weaknesses, jobCode), plan(weaknesses, points));
    }

    /** 取曲线上的时间：优先结束时间，其次开始时间，最后创建时间；全空时排最前而不是抛错。 */
    private LocalDateTime timeline(InterviewSession session) {
        if (session.getEndedAt() != null) {
            return session.getEndedAt();
        }
        if (session.getStartedAt() != null) {
            return session.getStartedAt();
        }
        return session.getCreatedAt() == null ? LocalDateTime.MIN : session.getCreatedAt();
    }

    private static Map<Long, List<InterviewEvaluationDimension>> groupBySession(
            List<InterviewEvaluationDimension> rows) {
        Map<Long, List<InterviewEvaluationDimension>> grouped = new LinkedHashMap<>();
        for (InterviewEvaluationDimension row : rows) {
            grouped.computeIfAbsent(row.getSessionId(), key -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    /**
     * 维度行 → 展示对象。
     *
     * <p>不直接用 {@link EvaluationDimension#of}：它会为未知 code 抛异常，而历史脏数据
     * 不该让整条成长曲线 500——认不出的维度跳过即可。
     */
    private static List<DimensionScore> toScores(List<InterviewEvaluationDimension> rows) {
        Map<String, String> labelByCode = new HashMap<>();
        for (EvaluationDimension dimension : EvaluationDimension.all()) {
            labelByCode.put(dimension.code(), dimension.label());
        }
        List<DimensionScore> scores = new ArrayList<>(rows.size());
        for (InterviewEvaluationDimension row : rows) {
            String label = labelByCode.get(row.getDimCode());
            if (label == null) {
                log.warn("[Trend] 跳过未知维度 {}", row.getDimCode());
                continue;
            }
            scores.add(new DimensionScore(row.getDimCode(), label,
                    row.getScore() == null ? 0 : row.getScore(),
                    row.getComment() == null ? "" : row.getComment()));
        }
        return List.copyOf(scores);
    }

    /**
     * 按最近几场的短板维度生成推荐与 3 天练习计划。
     *
     * <p>平均分用「最近窗口内该维度有分的场次」算，缺维度的场次不拉低平均；
     * 评语取窗口内最新的那条，因为它描述的是当前状态。
     */
    private List<Recommendation> recommend(List<Weakness> weaknesses, String jobCode) {
        List<Recommendation> recommendations = new ArrayList<>(RECOMMEND_COUNT);
        for (int i = 0; i < weaknesses.size() && i < RECOMMEND_COUNT; i++) {
            Weakness weakness = weaknesses.get(i);
            List<KnowledgeHit> hits = recommendHits(weakness.query(), jobCode);
            recommendations.add(new Recommendation(weakness.code(), weakness.label(),
                    weakness.average(), weakness.query(), hits));
        }
        return List.copyOf(recommendations);
    }

    /**
     * 推荐场景的检索：先用注入门槛试一次，空结果再放开阈值兜底。
     *
     * <p>为什么不是一步放开：同一个 RRF 排序下，严阈值通过的结果一定是松阈值的子集，
     * 先严后松不会牺牲质量。评语具体（带术语，如「Spring 事务传播级别没区分」）时
     * 第一次就能命中，质量最高；评语是「深度不够」这类抽象短语时，8.0 会把候选全筛掉，
     * 此时宁可给一条不太贴切的资料，也好过让页面误报「知识库没有条目」。
     *
     * <p>只影响推荐这一次调用，不改 {@link RetrievalService} 的全局默认阈值，注入链路不受影响。
     */
    private List<KnowledgeHit> recommendHits(String query, String jobCode) {
        List<KnowledgeHit> hits = retrievalService.search(query, null, jobCode,
                RetrievalService.SearchOptions.defaults().withTopK(TOP_K),
                RetrievalService.RetrievalMode.HYBRID);
        if (!hits.isEmpty()) {
            return hits;
        }
        return retrievalService.search(query, null, jobCode,
                RetrievalService.SearchOptions.defaults().withTopK(TOP_K)
                        .withThresholds(0.0, 0.0),
                RetrievalService.RetrievalMode.HYBRID);
    }

    /**
     * 最近窗口内各维度的平均分，按从低到高排（最弱的在最前）。
     *
     * <p>同时服务「提升路径」与「练习计划」：两处必须基于同一份短板排序，
     * 否则用户会看到推荐里写 fit 最弱、计划第一天却练 logic。
     */
    private List<Weakness> weaknesses(List<TrendPoint> points) {
        if (points.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, points.size() - RECENT_WINDOW);
        List<TrendPoint> recent = points.subList(from, points.size());

        Map<String, Integer> total = new HashMap<>();
        Map<String, Integer> count = new HashMap<>();
        Map<String, String> latestComment = new HashMap<>();
        Map<String, String> labelByCode = new HashMap<>();
        for (TrendPoint point : recent) {
            for (DimensionScore dimension : point.dimensions()) {
                total.merge(dimension.code(), dimension.score(), Integer::sum);
                count.merge(dimension.code(), 1, Integer::sum);
                labelByCode.put(dimension.code(), dimension.label());
                if (dimension.comment() != null && !dimension.comment().isBlank()) {
                    // points 升序，后写覆盖先写 = 最新评语
                    latestComment.put(dimension.code(), dimension.comment().strip());
                }
            }
        }
        if (total.isEmpty()) {
            return List.of();
        }

        List<String> codes = new ArrayList<>(total.keySet());
        codes.sort(Comparator.comparingDouble(code -> average(total, count, code)));

        List<Weakness> weaknesses = new ArrayList<>(codes.size());
        for (String code : codes) {
            String label = labelByCode.getOrDefault(code, code);
            String comment = latestComment.getOrDefault(code, "");
            String query = comment.isBlank() ? label : comment;
            if (query.length() > MAX_QUERY_LENGTH) {
                query = query.substring(0, MAX_QUERY_LENGTH);
            }
            weaknesses.add(new Weakness(code, label, (int) Math.round(average(total, count, code)),
                    query, comment));
        }
        return List.copyOf(weaknesses);
    }

    /**
     * 3 天练习计划：两个最弱维度各一天，第三天综合复盘并链到最后一场报告。
     *
     * <p>只有一条短板数据时第二天用「巩固」而不是重复「专项」——文案上要说得通。
     *
     * <p><b>任务不是写死的文案</b>：{@link #tasksOf} 里的动作按维度给的是「怎么练」的通用步骤，
     * 但每天的第一个任务会把该维度<b>最新的模型评语</b>嵌进去（「模型上次指出：…」），
     * 让任务落在用户真实缺陷上。评语为空时退回通用步骤，不会出现空条目。
     */
    private List<PracticeDay> plan(List<Weakness> weaknesses, List<TrendPoint> points) {
        if (weaknesses.isEmpty()) {
            return List.of();
        }
        Weakness first = weaknesses.get(0);
        boolean hasSecond = weaknesses.size() > 1;
        Weakness second = hasSecond ? weaknesses.get(1) : first;

        Long lastSessionId = points.isEmpty() ? null : points.get(points.size() - 1).sessionId();
        // 回看的是最后一场报告；场次 id 为空的极端情况退回记录页，不拼一个打不开的地址
        String reportRoute = lastSessionId == null
                ? "/records"
                : "/report/s-" + lastSessionId + "?sid=" + lastSessionId;

        return List.of(
                practiceDay(1, first, "专项："),
                practiceDay(2, second, hasSecond ? "专项：" : "巩固："),
                new PracticeDay(3, null, "综合复盘", "把两个短板放进一场完整面试",
                        List.of("用完整流程面一场，刻意在回答里补齐前两天练的两个维度",
                                "结束后对照最后一问的逐题复盘，重答得分最低的两题",
                                "回看成长曲线，确认这两个维度的分数有没有回升"),
                        reportRoute, "回看上一场报告"));
    }

    private static PracticeDay practiceDay(int day, Weakness weakness, String prefix) {
        return new PracticeDay(day, weakness.code(), weakness.label(),
                prefix + themeOf(weakness.code()), tasksFor(weakness),
                "/interview", "开始专项面试");
    }

    /**
     * 这一天的任务列表：把模型评语嵌进第一步。
     *
     * <p>为什么评语要进 tasks 而不是只留在「提升路径」的 query 字段里：用户看计划时
     * 需要知道「为什么今天练这个」，光给维度名（「岗位匹配度」）和通用步骤，
     * 和网上抄来的模板没有区别。评语是这场面试里模型对<b>这个人</b>说的具体话，
     * 放进来才叫个性化。
     */
    private static List<String> tasksFor(Weakness weakness) {
        List<String> base = tasksOf(weakness.code());
        if (weakness.comment() == null || weakness.comment().isBlank()) {
            return base;
        }
        List<String> tasks = new ArrayList<>(base.size() + 1);
        tasks.add("先看模型上次的点评：「" + trim(weakness.comment(), COMMENT_IN_TASK) + "」");
        tasks.addAll(base);
        return List.copyOf(tasks);
    }

    /** 评语进任务时的截断长度：太长会把卡片撑爆，60 字够说清一个具体缺陷。 */
    private static String trim(String text, int limit) {
        String flat = text.replaceAll("\\s+", " ").strip();
        return flat.length() <= limit ? flat : flat.substring(0, limit) + "…";
    }

    /** 每个维度一句「练什么」的主题，与评估口径里的判定标准对应。 */
    private static String themeOf(String code) {
        return switch (code) {
            case "technical" -> "补技术事实，先把概念说准";
            case "depth" -> "往底层挖一层，讲清原理与取舍";
            case "logic" -> "练表达结构，让结论有论据";
            case "fit" -> "对准岗位，用项目证明匹配度";
            default -> "复盘最近一场的点评";
        };
    }

    /** 每天 3 个可执行动作；刻意写成「能当场做」的动作，而不是「加强学习」这种空话。 */
    private static List<String> tasksOf(String code) {
        return switch (code) {
            case "technical" -> List.of(
                    "对着本页推荐的片段，逐条核对定义、版本差异与适用条件",
                    "把最容易说错的 3 个点各写成一句结论，答题时先给结论再展开",
                    "练习时主动使用准确术语，不用「那个东西」指代");
            case "depth" -> List.of(
                    "每个知识点按「是什么 → 底层怎么做 → 为什么这么设计」三层各说一句",
                    "挑一条推荐片段，试着自己讲出它的取舍与边界，而不是背定义",
                    "录一段回答复听，检查有没有停在用法层面");
            case "logic" -> List.of(
                    "用「结论 → 原因 → 对比 → 例子」四段式重答上轮得分最低的一题",
                    "每个答案至少出现一次「因为/所以」或「相比」，把论据和结论绑住",
                    "回答前先停两秒列要点，避免边想边说导致跑题");
            case "fit" -> List.of(
                    "对照目标岗位的技能要求，把简历里的项目改写成「问题—方案—结果」",
                    "准备 2 个能体现岗位核心能力的项目细节，主动往岗位场景靠",
                    "删掉与岗位无关的答题内容，只保留贴合岗位的论据");
            default -> List.of("复盘最近一场的逐题点评，挑一题重答");
        };
    }

    private static double average(Map<String, Integer> total, Map<String, Integer> count, String code) {
        int n = count.getOrDefault(code, 0);
        return n == 0 ? 0.0 : (double) total.getOrDefault(code, 0) / n;
    }

    /**
     * 一个短板维度：code、展示名、最近窗口平均分、检索用的查询串、最新评语原文。
     *
     * <p>{@code comment} 与 {@code query} 分开存：{@code query} 是给检索引擎的（截断到
     * {@link #MAX_QUERY_LENGTH}），{@code comment} 是给人看的原文（进练习计划的任务）。
     * 两者长度上限不同，合并成一个字段会让检索或展示其中一头失真。
     */
    private record Weakness(String code, String label, int average, String query, String comment) {
    }
}
