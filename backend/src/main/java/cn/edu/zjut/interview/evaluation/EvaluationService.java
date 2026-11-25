package cn.edu.zjut.interview.evaluation;

import cn.edu.zjut.interview.config.ConfigKey;
import cn.edu.zjut.interview.config.ConfigService;
import cn.edu.zjut.interview.domain.InterviewEvaluation;
import cn.edu.zjut.interview.domain.InterviewEvaluationDimension;
import cn.edu.zjut.interview.domain.InterviewMessage;
import cn.edu.zjut.interview.domain.InterviewSession;
import cn.edu.zjut.interview.domain.UserProfile;
import cn.edu.zjut.interview.dto.EvaluationResult;
import cn.edu.zjut.interview.dto.EvaluationResult.DimensionScore;
import cn.edu.zjut.interview.dto.EvaluationResult.QuestionReview;
import cn.edu.zjut.interview.llm.LlmClient;
import cn.edu.zjut.interview.repository.InterviewEvaluationDimensionRepository;
import cn.edu.zjut.interview.repository.InterviewEvaluationRepository;
import cn.edu.zjut.interview.repository.InterviewMessageRepository;
import cn.edu.zjut.interview.repository.InterviewSessionRepository;
import cn.edu.zjut.interview.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 多维内容评估的编排（<b>本项目扩展（课设指标 ③）</b>）。
 *
 * <p>与 {@code /finish} 完全解耦：{@code InterviewEngine.finishNow} 一行都不动，
 * 评估由报告页单独触发。理由是一次评估要十几秒，「结束面试」不能卡在这上面。
 *
 * <p>整个 {@link #evaluate} 在一个事务里：模型失败时<b>不抛 502</b>，而是降级成
 * {@link RuleBasedEvaluator} 的规则估分并照常落库——报告页至少能看到「大致在什么水平」，
 * 且结果带 {@code ruleBased} 标记，不会被当成模型评的。对话不足仍直接报错：
 * 那不是模型故障，给分数就是编造样本。
 */
@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    /** 至少要这么多次求职者回答才评估：一问一答说明不了深度，评出来只是噪声。 */
    private static final int MIN_USER_MESSAGES = 2;

    private final InterviewSessionRepository sessionRepository;
    private final InterviewMessageRepository messageRepository;
    private final InterviewEvaluationRepository evaluationRepository;
    private final InterviewEvaluationDimensionRepository dimensionRepository;
    private final UserProfileRepository userProfileRepository;
    private final LlmClient llmClient;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;

    public EvaluationService(InterviewSessionRepository sessionRepository,
                             InterviewMessageRepository messageRepository,
                             InterviewEvaluationRepository evaluationRepository,
                             InterviewEvaluationDimensionRepository dimensionRepository,
                             UserProfileRepository userProfileRepository,
                             LlmClient llmClient,
                             ConfigService configService,
                             ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.evaluationRepository = evaluationRepository;
        this.dimensionRepository = dimensionRepository;
        this.userProfileRepository = userProfileRepository;
        this.llmClient = llmClient;
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    /**
     * 对一场面试跑（或重跑）评估：组 prompt → 调大模型 → 解析 → 落库 → 回写场次总分。
     *
     * <p>重跑是覆盖而不是追加：先 {@code deleteBySessionId} 两张表再插新的，
     * 配合 {@code uk_evaluation_session} 唯一约束保证一场只有一份。
     *
     * <p>模型调用失败或输出不可解析时降级为规则估分，不抛错；结果里的 {@code ruleBased=true}。
     *
     * @throws ResponseStatusException 场次不存在（404）
     * @throws EvaluationException     对话不足（模型故障不再抛，走规则估分）
     */
    @Transactional
    public EvaluationResult evaluate(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到场次：" + sessionId));

        // system 行是给模型的内部指令，不是面试内容，必须排除——把它当对话会污染评估口径
        List<InterviewMessage> dialogue = new ArrayList<>();
        int userCount = 0;
        for (InterviewMessage message : messageRepository.findBySessionIdOrderBySeqAsc(sessionId)) {
            if (InterviewMessage.ROLE_SYSTEM.equals(message.getRole())) {
                continue;
            }
            if (InterviewMessage.ROLE_USER.equals(message.getRole())
                    || InterviewMessage.ROLE_ASSISTANT.equals(message.getRole())) {
                dialogue.add(message);
                if (InterviewMessage.ROLE_USER.equals(message.getRole())) {
                    userCount++;
                }
            }
        }
        if (userCount < MIN_USER_MESSAGES) {
            throw new EvaluationException(EvaluationException.NO_ENOUGH_DIALOGUE,
                    "至少需要 " + MIN_USER_MESSAGES + " 次回答才能评估，当前只有 " + userCount + " 次");
        }

        UserProfile profile = userProfileRepository.findFirstByOrderByIdAsc().orElse(null);
        String resumeSummary = profile == null ? null : profile.getResumeSummary();

        EvaluationParser.EvaluationDraft draft;
        String model;
        try {
            String raw = llmClient.complete(
                    EvaluationPrompter.build(session.getJobCode(), resumeSummary, dialogue));
            draft = EvaluationParser.parse(raw);
            model = configService.get(ConfigKey.LLM_MODEL);
        } catch (LlmClient.LlmException | EvaluationException e) {
            // 两类失败都兜底：调用失败（网络/超时/非 200）与输出不可解析（不是 JSON / 缺维度）。
            // 对用户来说结果一样——这次拿不到模型评估，但报告不该整页变红。
            log.warn("[EvaluationService] 场次 {} 的模型评估失败，降级为规则估分：{}", sessionId, e.getMessage());
            draft = RuleBasedEvaluator.draft(dialogue);
            model = RuleBasedEvaluator.MODEL;
        }

        // 先删旧的再插新的：唯一约束 + 这两条 DELETE 保证一场只有一份评估
        dimensionRepository.deleteBySessionId(sessionId);
        evaluationRepository.deleteBySessionId(sessionId);
        dimensionRepository.flush();
        evaluationRepository.flush();

        LocalDateTime now = LocalDateTime.now();
        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setSessionId(sessionId);
        evaluation.setJobCode(session.getJobCode());
        evaluation.setOverallScore(draft.overall());
        evaluation.setSummary(draft.summary());
        evaluation.setStrengths(toJson(draft.strengths()));
        evaluation.setWeaknesses(toJson(draft.weaknesses()));
        evaluation.setSuggestions(toJson(draft.suggestions()));
        evaluation.setQuestionReviews(objectMapper.valueToTree(draft.questions()).toString());
        evaluation.setModel(model);
        evaluation.setCreatedAt(now);
        evaluationRepository.save(evaluation);

        List<DimensionScore> dimensions = new ArrayList<>();
        for (EvaluationDimension dimension : EvaluationDimension.all()) {
            EvaluationParser.DimScore score = draft.dimensions().get(dimension.code());
            InterviewEvaluationDimension row = new InterviewEvaluationDimension();
            row.setSessionId(sessionId);
            row.setDimCode(dimension.code());
            row.setScore(score.score());
            row.setComment(score.comment());
            dimensionRepository.save(row);
            dimensions.add(new DimensionScore(dimension.code(), dimension.label(), score.score(), score.comment()));
        }

        // 回写场次总分：报告页与场次列表都从 interview_session.score 读，不另开一列
        session.setScore(draft.overall());
        sessionRepository.save(session);

        List<QuestionReview> questions = new ArrayList<>();
        for (EvaluationParser.QuestionDraft question : draft.questions()) {
            questions.add(new QuestionReview(question.index(), question.question(), question.quote(),
                    question.score(), question.comment(), question.suggestion()));
        }

        log.info("[EvaluationService] 场次 {} 评估完成，总分 {}，逐题 {} 道（model={}）",
                sessionId, draft.overall(), questions.size(), model);
        return new EvaluationResult(sessionId, session.getJobCode(), draft.overall(), draft.summary(),
                draft.strengths(), draft.weaknesses(), draft.suggestions(), List.copyOf(dimensions),
                List.copyOf(questions), model, RuleBasedEvaluator.MODEL.equals(model), now);
    }

    /**
     * 取已有的评估；没有 → 404。
     *
     * <p>不在这里触发评估：一次评估十几秒，GET 语义必须是「查」，
     * 「没有就现算」会让前端一次点击挂起十几秒、且重试行为不可控。
     */
    @Transactional(readOnly = true)
    public EvaluationResult load(Long sessionId) {
        List<InterviewEvaluation> rows = evaluationRepository.findBySessionId(sessionId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "场次 " + sessionId + " 还没有评估结果");
        }
        InterviewEvaluation row = rows.get(0);
        List<DimensionScore> dimensions = new ArrayList<>();
        for (InterviewEvaluationDimension dim : dimensionRepository.findBySessionIdOrderByIdAsc(sessionId)) {
            EvaluationDimension dimension = EvaluationDimension.of(dim.getDimCode());
            dimensions.add(new DimensionScore(dimension.code(), dimension.label(),
                    dim.getScore() == null ? 0 : dim.getScore(),
                    dim.getComment() == null ? "" : dim.getComment()));
        }
        return new EvaluationResult(row.getSessionId(), row.getJobCode(), row.getOverallScore(), row.getSummary(),
                fromJson(row.getStrengths()), fromJson(row.getWeaknesses()), fromJson(row.getSuggestions()),
                List.copyOf(dimensions), questionReviews(row.getQuestionReviews()),
                row.getModel(), RuleBasedEvaluator.MODEL.equals(row.getModel()), row.getCreatedAt());
    }

    /**
     * 逐题复盘 JSON → 列表。历史评估没有这一列（null）或写坏了，一律降级成空数组：
     * 「查评估」不应该因为一个可选字段挂掉。
     */
    private List<QuestionReview> questionReviews(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                return List.of();
            }
            List<QuestionReview> reviews = new ArrayList<>(node.size());
            for (JsonNode item : node) {
                if (!item.isObject()) {
                    continue;
                }
                reviews.add(new QuestionReview(
                        item.path("index").isNumber() ? item.path("index").asInt() : reviews.size() + 1,
                        item.path("question").asText(""),
                        item.path("quote").asText(""),
                        item.path("score").isNumber() ? item.path("score").asInt() : 0,
                        item.path("comment").asText(""),
                        item.path("suggestion").asText("")));
            }
            return reviews;
        } catch (IOException e) {
            log.warn("[EvaluationService] question_reviews 不是合法 JSON 数组，按空处理：{}", json);
            return List.of();
        }
    }

    /**
     * 数组 → JSON 字符串（数组一律 JSON 存 CLOB，与 {@code interview_session.kps} 同一做法）。
     *
     * <p>走 {@code valueToTree} 而不是 {@code writeValueAsString}：后者抛受检的
     * {@code JsonProcessingException}，而这里装的是纯字符串列表，不可能真的失败，
     * 为一个不会发生的异常写 try-catch 反而把正常路径包进异常处理里。
     */
    private String toJson(List<String> values) {
        return objectMapper.valueToTree(values == null ? List.of() : values).toString();
    }

    /** JSON 字符串 → 数组；历史脏数据（不是数组）降级成空数组，不该让「查评估」挂掉。 */
    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                return List.of();
            }
            List<String> values = new ArrayList<>(node.size());
            for (JsonNode item : node) {
                values.add(item.asText());
            }
            return values;
        } catch (IOException e) {
            log.warn("[EvaluationService] 评估字段不是合法 JSON 数组，按空处理：{}", json);
            return List.of();
        }
    }
}
