package cn.edu.zjut.interview.service;

import cn.edu.zjut.interview.domain.InterviewQuestion;
import cn.edu.zjut.interview.domain.InterviewSession;
import cn.edu.zjut.interview.dto.FreeStateView;
import cn.edu.zjut.interview.repository.InterviewQuestionRepository;
import cn.edu.zjut.interview.repository.InterviewSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 自由面试（issue #11）：把「随便聊」改成「真面试」。
 *
 * <p>流程：候选人进房后先自述想面什么岗位 → 这里从自述里识别岗位
 * → 按岗位从 {@code interview_question} 随机抽一道没问过的题
 * → 把「当前题目 + 参考要点 + 追问」拼进本轮 system，面试官自然提问/追问
 * → 聊满两轮换下一题 → 结束时仍走原有的评估链路。
 *
 * <p>状态全部存在 {@code interview_session} 的自由面试列上（刷新后照常续上），
 * 剧本场次（{@code script_id} 非空）一行都不碰。
 */
@Service
public class FreeInterviewService {

    /** 识别的岗位编码与前端 {@code data/jobs.ts} 的 Job.id 同一口径。 */
    private static final String JOB_JAVA = "java";
    private static final String JOB_FRONTEND = "frontend";
    private static final String JOB_PYTHON = "python";
    private static final String JOB_GO = "go";
    private static final String JOB_TESTING = "testing";

    /** 岗位专属词：命中才判该岗位，避免「Python 后端」被泛化词「后端」抢成 Java。 */
    private static final List<String> JAVA_KEYWORDS = List.of(
            "java", "spring", "jvm", "微服务", "mysql", "redis", "并发", "mybatis", "hibernate");

    /** 只说了「后端 / 服务端」时才会用到，默认按 Java 后台处理（保持历史行为）。 */
    private static final List<String> JAVA_GENERIC_KEYWORDS = List.of(
            "后端", "服务端", "backend");

    private static final List<String> FRONTEND_KEYWORDS = List.of(
            "前端", "vue", "react", "javascript", "typescript", "node", "浏览器", "css",
            "html", "页面", "组件", "frontend", "web开发");

    private static final List<String> PYTHON_KEYWORDS = List.of(
            "python", "django", "flask", "fastapi", "pandas", "爬虫");

    private static final List<String> GO_KEYWORDS = List.of(
            "go", "golang", "gin", "beego", "grpc");

    private static final List<String> TESTING_KEYWORDS = List.of(
            "测试开发", "测试工程师", "软件测试", "自动化测试", "接口测试", "测试", "qa",
            "selenium", "pytest", "单元测试", "压力测试");

    /** 一道题聊几轮（候选人回答的次数）后换下一题：1 次作答 + 1 次追问。 */
    private static final int TURNS_PER_QUESTION = 2;

    /** 候选人连续这么多条消息都没说清岗位时，回落到设置页选的岗位，避免面试一直卡在确认。 */
    private static final long JOB_FALLBACK_AFTER_MESSAGES = 2;

    private final InterviewQuestionRepository questionRepository;
    private final InterviewSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    public FreeInterviewService(InterviewQuestionRepository questionRepository,
                                InterviewSessionRepository sessionRepository,
                                ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.sessionRepository = sessionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 每条用户消息落库后调用一次，维护「岗位 → 当前题 → 轮次」的状态。
     *
     * @param session          当前场次（方法内会改自由面试列并落库）
     * @param userText         候选人这条消息（用来识别岗位）
     * @param userMessageCount 本场累计用户消息数（1-based）
     */
    public FreeProgress prepare(InterviewSession session, String userText, long userMessageCount) {
        if (session.getScriptId() != null) {
            return FreeProgress.inactive();
        }

        boolean changed = false;
        if (session.getFreeJobCode() == null) {
            String detected = detectJob(userText);
            if (detected == null && userMessageCount >= JOB_FALLBACK_AFTER_MESSAGES) {
                detected = session.getJobCode();
            }
            if (detected == null) {
                // 还是自由面试，只是岗位没识别出来：本轮要注入「先问岗位」的指令块
                return FreeProgress.pending();
            }
            session.setFreeJobCode(detected);
            assignNext(session);
            changed = true;
        } else {
            int turns = session.getFreeQuestionTurns() == null ? 0 : session.getFreeQuestionTurns();
            if (turns >= TURNS_PER_QUESTION) {
                changed = assignNext(session);
            } else {
                session.setFreeQuestionTurns(turns + 1);
            }
        }

        sessionRepository.save(session);
        return progress(session, changed);
    }

    /** {@code GET /api/sessions/{id}/free-state}（刷新恢复抬头用）。 */
    public FreeStateView state(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在：" + sessionId));
        return toView(progress(session, false));
    }

    /**
     * 本轮要追加到 system 的「当前题目」指令块。
     *
     * <p>只改本轮请求副本，不落库：与知识注入 / 世界书同一铁律。
     *
     * @return {@code null} = 剧本场次，不需要这条
     */
    public String promptBlock(FreeProgress progress) {
        if (!progress.active()) {
            return null;
        }
        if (progress.jobCode() == null) {
            return """
                    【自由面试 · 确认岗位】
                    这是本场自由面试的开场，候选人还没说明想面试的岗位/方向。
                    请先用一两句自然的话打个招呼，然后问候选人今天想面什么岗位、哪个方向（例如 Java 后端 / Web 前端）。
                    这一轮不要出技术题，也不要长篇自我介绍。
                    """;
        }
        InterviewQuestion question = progress.question();
        if (question == null) {
            return """
                    【自由面试 · 岗位已确认】
                    本场面试岗位：%s。题库里这个岗位暂时没有可用题目，
                    请围绕这个岗位的常见考察点自由提问，一次只问一个问题。
                    """.formatted(progress.jobName());
        }

        StringBuilder block = new StringBuilder();
        block.append("【自由面试 · 题库出题】\n");
        block.append("本场面试岗位：").append(progress.jobName())
                .append("（job_code=").append(progress.jobCode()).append("）。\n");
        block.append("当前题目（第 ").append(progress.askedCount()).append(" / ")
                .append(progress.questionCount()).append(" 题，知识点 ").append(question.getKp())
                .append("，难度 ").append(question.getDifficulty())
                .append("，类型 ").append(question.getType()).append("）：\n");
        block.append(question.getText()).append("\n");
        if (question.getReference() != null && !question.getReference().isBlank()) {
            block.append("参考要点（只给你自己看，不要说“题库”“要点”这类词）：")
                    .append(question.getReference()).append("\n");
        }
        List<String> followUps = followUps(question);
        if (!followUps.isEmpty()) {
            block.append("可选的追问方向：").append(String.join("；", followUps)).append("\n");
        }
        block.append("""
                要求：
                1. 用你自己的口吻自然地提问，不要照抄题面，不要暴露“题库/参考要点/第几题”这些词。
                2. 先回应候选人上一句回答（认同、指出漏洞或补一句评价），再问问题，像真人面试官一样有来有回。
                3. 每次最多问 1~2 个问题，不要一口气把所有追问抛出来。
                """);
        if (progress.changed()) {
            block.append("""
                    4. 本轮是一道新题：上一题到此为止，不要再追问上一题的内容，
                       用一两句自然的过场（比如「那换个方向」「再问你一个」「这部分先到这儿」）后直接问上面这道题。
                    """);
        } else {
            block.append("""
                    4. 本轮继续围绕上面这道题追问：答得含糊就追没覆盖到的要点，答得完整就顺势深挖一层或换个角度，
                       不要跳到这道题之外的其它话题。
                    """);
        }
        return block.toString();
    }

    /** 题库随机抽题：优先没问过的；全问过就开启新一轮，但避免紧接着重复同一题。 */
    private boolean assignNext(InterviewSession session) {
        String jobCode = session.getFreeJobCode();
        List<InterviewQuestion> pool =
                questionRepository.findByJobCodeAndEnabledTrueOrderBySortOrderAscIdAsc(jobCode);
        if (pool.isEmpty()) {
            session.setFreeCurrentQuestionId(null);
            session.setFreeQuestionTurns(0);
            return false;
        }

        List<Long> asked = askedIds(session);
        List<InterviewQuestion> fresh = pool.stream()
                .filter(q -> !asked.contains(q.getId()))
                .collect(Collectors.toList());
        if (fresh.isEmpty()) {
            asked.clear();
            fresh = new ArrayList<>(pool);
        }
        Long current = session.getFreeCurrentQuestionId();
        if (current != null && fresh.size() > 1) {
            fresh = fresh.stream()
                    .filter(q -> !current.equals(q.getId()))
                    .collect(Collectors.toList());
        }

        InterviewQuestion pick = fresh.get(ThreadLocalRandom.current().nextInt(fresh.size()));
        asked.add(pick.getId());
        session.setFreeAskedQuestionIds(asked.stream().map(String::valueOf).collect(Collectors.joining(",")));
        session.setFreeCurrentQuestionId(pick.getId());
        session.setFreeQuestionTurns(0);
        return true;
    }

    private FreeProgress progress(InterviewSession session, boolean changed) {
        if (session.getScriptId() != null) {
            return FreeProgress.inactive();
        }
        String jobCode = session.getFreeJobCode();
        InterviewQuestion question = session.getFreeCurrentQuestionId() == null
                ? null
                : questionRepository.findById(session.getFreeCurrentQuestionId()).orElse(null);
        int questionCount = jobCode == null ? 0
                : questionRepository.findByJobCodeAndEnabledTrueOrderBySortOrderAscIdAsc(jobCode).size();
        return new FreeProgress(true, changed, jobCode, jobName(jobCode),
                question, askedIds(session).size(), questionCount);
    }

    private FreeStateView toView(FreeProgress progress) {
        InterviewQuestion q = progress.question();
        FreeStateView.QuestionView question = q == null ? null : new FreeStateView.QuestionView(
                q.getId(), q.getKp(), q.getType(), q.getDifficulty(), q.getText());
        return new FreeStateView(progress.active(), progress.jobCode(), progress.jobName(),
                progress.askedCount(), progress.questionCount(), question);
    }

    private List<Long> askedIds(InterviewSession session) {
        String raw = session.getFreeAskedQuestionIds();
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // 脏数据跳过，不用一条坏 id 把整场面试打死
            }
        }
        return ids;
    }

    private List<String> followUps(InterviewQuestion question) {
        String raw = question.getFollowUps();
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 从候选人自述里识别岗位：各岗位关键词计分，分最高者胜；
     * 一个都没命中返回 {@code null}（交给下一轮或兜底）。
     *
     * <p>泛化词「后端 / 服务端 / backend」只在没有任何岗位专属词命中时才按 Java 兜底，
     * 否则「我想面 Python 后端」会被「后端」误判成 Java。
     */
    static String detectJob(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put(JOB_JAVA, score(lower, JAVA_KEYWORDS));
        scores.put(JOB_FRONTEND, score(lower, FRONTEND_KEYWORDS));
        scores.put(JOB_PYTHON, score(lower, PYTHON_KEYWORDS));
        scores.put(JOB_GO, score(lower, GO_KEYWORDS));
        scores.put(JOB_TESTING, score(lower, TESTING_KEYWORDS));

        String best = null;
        int bestScore = 0;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                best = entry.getKey();
                bestScore = entry.getValue();
            }
        }
        if (best != null) {
            return best;
        }
        return score(lower, JAVA_GENERIC_KEYWORDS) > 0 ? JOB_JAVA : null;
    }

    private static int score(String text, List<String> keywords) {
        int score = 0;
        for (String keyword : keywords) {
            if (containsKeyword(text, keyword)) {
                score += keyword.length() >= 3 ? 2 : 1;
            }
        }
        return score;
    }

    /**
     * ASCII 关键词按词边界匹配，中文关键词按包含匹配。
     *
     * <p>没有边界的话 "javascript" 会命中 "java"、"django" 会命中 "go"，
     * 岗位识别会大面积误判。
     */
    private static boolean containsKeyword(String text, String keyword) {
        if (keyword.chars().anyMatch(c -> c > 127)) {
            return text.contains(keyword);
        }
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(keyword) + "(?![a-z0-9])")
                .matcher(text)
                .find();
    }

    static String jobName(String jobCode) {
        if (jobCode == null) {
            return null;
        }
        return switch (jobCode) {
            case JOB_JAVA -> "Java 后端";
            case JOB_FRONTEND -> "Web 前端";
            case JOB_PYTHON -> "Python 后端";
            case JOB_GO -> "Go 后端";
            case JOB_TESTING -> "测试开发";
            default -> jobCode;
        };
    }

    /**
     * 一次 {@link #prepare} 的结果，ChatService 用它拼 prompt 和发 {@code ai:question}。
     *
     * @param changed 本次是否换了岗位 / 换了题目（前端据此刷新抬头，不必每轮都推）
     */
    public record FreeProgress(
            boolean active,
            boolean changed,
            String jobCode,
            String jobName,
            InterviewQuestion question,
            int askedCount,
            int questionCount) {

        static FreeProgress inactive() {
            return new FreeProgress(false, false, null, null, null, 0, 0);
        }

        /** 自由面试但岗位还没识别出来（本轮先让面试官问岗位）。 */
        static FreeProgress pending() {
            return new FreeProgress(true, false, null, null, null, 0, 0);
        }
    }
}
