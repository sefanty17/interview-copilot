package cn.edu.zjut.interview.service;

import cn.edu.zjut.interview.domain.InterviewMessage;
import cn.edu.zjut.interview.domain.InterviewSession;
import cn.edu.zjut.interview.domain.Interviewer;
import cn.edu.zjut.interview.domain.SessionImpression;
import cn.edu.zjut.interview.dto.ImpressionView;
import cn.edu.zjut.interview.god.ImpressionEvaluator;
import cn.edu.zjut.interview.llm.LlmClient;
import cn.edu.zjut.interview.repository.InterviewMessageRepository;
import cn.edu.zjut.interview.repository.InterviewSessionRepository;
import cn.edu.zjut.interview.repository.InterviewerRepository;
import cn.edu.zjut.interview.repository.SessionImpressionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 面试印象：每若干个回答后台评估一次，更新「正面六维 + 负面六维」。
 *
 * <p>设计取舍：
 * <ul>
 *   <li>不用神级 agent 的工具调用，直接让模型回一段 JSON——评估是旁路动作，
 *       不该抢主对话的 token 预算，也不该在模型不支持 tool_calls 时整条链路失效。</li>
 *   <li>评估失败一律吞掉（只记 warn）：候选人已经看到回复了，不能因为评估失败报错。</li>
 *   <li>按「场次 + 面试官」存：同一场里换面试官，各自有各自对你的印象。</li>
 * </ul>
 */
@Service
public class ImpressionService {

    private static final Logger log = LoggerFactory.getLogger(ImpressionService.class);

    /** 每几条候选人回答评估一次。太频繁会拖慢对话，太稀疏则印象看不出变化。 */
    private static final int EVAL_EVERY_USER_MESSAGES = 3;

    /** 只把最近这些条（含面试官台词）喂给评估模型。 */
    private static final int RECENT_LINES = 10;

    private final InterviewMessageRepository messageRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewerRepository interviewerRepository;
    private final SessionImpressionRepository impressionRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public ImpressionService(InterviewMessageRepository messageRepository,
                             InterviewSessionRepository sessionRepository,
                             InterviewerRepository interviewerRepository,
                             SessionImpressionRepository impressionRepository,
                             LlmClient llmClient,
                             ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
        this.interviewerRepository = interviewerRepository;
        this.impressionRepository = impressionRepository;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 剧本场次入口：只需场次 id，自行取面试官。
     *
     * <p>剧本对话走引擎（{@code script:input / script:choice}），不经过
     * {@code ChatService}，所以两条链路都要各自触发一次评估。
     */
    public Optional<ImpressionView> evaluateIfDue(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null || session.getInterviewerId() == null) {
            return Optional.empty();
        }
        Interviewer interviewer = interviewerRepository.findById(session.getInterviewerId()).orElse(null);
        return evaluateIfDue(session, interviewer);
    }

    /**
     * 到点就评估一次（第 3、6、9… 条候选人消息）。
     *
     * @return 本次有新结果时返回视图；未到点 / 已评估过 / LLM 失败时返回 {@link Optional#empty()}
     */
    public Optional<ImpressionView> evaluateIfDue(InterviewSession session, Interviewer interviewer) {
        if (session == null || interviewer == null || interviewer.getId() == null) {
            return Optional.empty();
        }
        Long sessionId = session.getId();
        int roleId = interviewer.getId().intValue();
        long userCount = messageRepository.countBySessionIdAndRoleAndMsgType(
                sessionId, InterviewMessage.ROLE_USER, "text");
        if (userCount <= 0) {
            return Optional.empty();
        }

        SessionImpression row = impressionRepository.findBySessionIdAndRoleId(sessionId, roleId).orElse(null);
        int evaluatedCount = row == null || row.getEvaluatedUserCount() == null ? 0 : row.getEvaluatedUserCount();
        // 按「距上次评估又攒够 3 条」判断，而不是用户消息总数能被 3 整除：
        // 中途才接上评估的场次（比如旧场次）也能在下一轮立刻出结果。
        if (userCount - evaluatedCount < EVAL_EVERY_USER_MESSAGES) {
            return Optional.empty();
        }

        ImpressionEvaluator.ImpressionVector beforePositive = positiveOf(row);
        ImpressionEvaluator.NegativeVector beforeNegative = negativeOf(row);
        List<InterviewMessage> recent = recentLines(sessionId);
        if (recent.isEmpty()) {
            return Optional.empty();
        }

        ImpressionEvaluator.Result result;
        try {
            String raw = llmClient.complete(ImpressionEvaluator.messages(
                    new ImpressionEvaluator.InterviewerBrief(interviewer.getName(), interviewer.getDisplayRole()),
                    beforePositive, beforeNegative, recent));
            result = ImpressionEvaluator.parse(raw, beforePositive, beforeNegative, objectMapper);
        } catch (Exception e) {
            log.warn("[Impression] 第 {} 条回答后的印象评估失败，跳过本次：{}", userCount, e.getMessage());
            return Optional.empty();
        }
        if (result == null) {
            log.warn("[Impression] 第 {} 条回答后的印象评估返回无法解析的内容，跳过本次", userCount);
            return Optional.empty();
        }

        SessionImpression saved = row == null ? new SessionImpression() : row;
        saved.setSessionId(sessionId);
        saved.setRoleId(roleId);
        apply(saved, result.positive(), result.negative());
        saved.setReason(result.reason());
        saved.setEvaluatedUserCount((int) userCount);
        saved.setUpdatedAt(LocalDateTime.now());
        impressionRepository.save(saved);
        return Optional.of(toView(saved, result.positiveDeltas(), result.negativeDeltas()));
    }

    /** {@code GET /api/sessions/{id}/impression}：刷新 / 重进房间时恢复雷达图。 */
    public ImpressionView state(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在：" + sessionId));
        int roleId = session.getInterviewerId() == null ? 0 : session.getInterviewerId().intValue();
        SessionImpression row = impressionRepository.findBySessionIdAndRoleId(sessionId, roleId).orElse(null);
        return toView(row, Map.of(), Map.of());
    }

    /** 删场次时清掉这一场的印象行，否则 {@code fk_imp_sess} 会拦住删除。 */
    public void deleteBySessionId(Long sessionId) {
        impressionRepository.deleteBySessionId(sessionId);
    }

    private List<InterviewMessage> recentLines(Long sessionId) {
        // 旁白 / 剧情提示也落成 role=user，喂给模型会把「面试官的描述」当成候选人回答，必须排掉
        List<InterviewMessage> all = messageRepository.findBySessionIdOrderBySeqAsc(sessionId).stream()
                .filter(m -> InterviewMessage.ROLE_ASSISTANT.equals(m.getRole())
                        || (InterviewMessage.ROLE_USER.equals(m.getRole()) && "text".equals(m.getMsgType())))
                .toList();
        int from = Math.max(0, all.size() - RECENT_LINES);
        return all.subList(from, all.size());
    }

    private ImpressionEvaluator.ImpressionVector positiveOf(SessionImpression row) {
        if (row == null) {
            return ImpressionEvaluator.ImpressionVector.initial();
        }
        return new ImpressionEvaluator.ImpressionVector(
                value(row.getProfessionalism()), value(row.getClarity()), value(row.getLogic()),
                value(row.getDepth()), value(row.getCredibility()), value(row.getPotential()));
    }

    private ImpressionEvaluator.NegativeVector negativeOf(SessionImpression row) {
        if (row == null) {
            return ImpressionEvaluator.NegativeVector.initial();
        }
        return new ImpressionEvaluator.NegativeVector(
                value(row.getSkepticism()), value(row.getConfusion()), value(row.getImpatience()),
                value(row.getDisappointment()), value(row.getWariness()), value(row.getReservation()));
    }

    private void apply(SessionImpression row,
                       ImpressionEvaluator.ImpressionVector positive,
                       ImpressionEvaluator.NegativeVector negative) {
        row.setProfessionalism(positive.professionalism());
        row.setClarity(positive.clarity());
        row.setLogic(positive.logic());
        row.setDepth(positive.depth());
        row.setCredibility(positive.credibility());
        row.setPotential(positive.potential());

        row.setSkepticism(negative.skepticism());
        row.setConfusion(negative.confusion());
        row.setImpatience(negative.impatience());
        row.setDisappointment(negative.disappointment());
        row.setWariness(negative.wariness());
        row.setReservation(negative.reservation());
    }

    private ImpressionView toView(SessionImpression row,
                                  Map<String, Integer> positiveDeltas,
                                  Map<String, Integer> negativeDeltas) {
        ImpressionEvaluator.ImpressionVector positive = positiveOf(row);
        ImpressionEvaluator.NegativeVector negative = negativeOf(row);
        Map<String, Integer> positiveMap = new LinkedHashMap<>();
        for (String[] dimension : ImpressionEvaluator.ImpressionVector.DIMENSIONS) {
            positiveMap.put(dimension[0], positive.get(dimension[0]));
        }
        Map<String, Integer> negativeMap = new LinkedHashMap<>();
        for (String[] dimension : ImpressionEvaluator.NegativeVector.DIMENSIONS) {
            negativeMap.put(dimension[0], negative.get(dimension[0]));
        }
        int roleId = row == null || row.getRoleId() == null ? 0 : row.getRoleId();
        int evaluatedCount = row == null || row.getEvaluatedUserCount() == null ? 0 : row.getEvaluatedUserCount();
        boolean evaluated = row != null && evaluatedCount > 0;
        String reason = row == null || row.getReason() == null ? "" : row.getReason();
        return new ImpressionView(roleId, evaluated, evaluatedCount,
                positiveMap, negativeMap, positiveDeltas, negativeDeltas, reason);
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }
}
