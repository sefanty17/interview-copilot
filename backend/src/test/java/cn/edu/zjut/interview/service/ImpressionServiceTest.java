package cn.edu.zjut.interview.service;

import cn.edu.zjut.interview.domain.InterviewMessage;
import cn.edu.zjut.interview.domain.InterviewSession;
import cn.edu.zjut.interview.domain.Interviewer;
import cn.edu.zjut.interview.dto.CreateSessionRequest;
import cn.edu.zjut.interview.dto.ImpressionView;
import cn.edu.zjut.interview.llm.LlmClient;
import cn.edu.zjut.interview.repository.InterviewMessageRepository;
import cn.edu.zjut.interview.repository.InterviewSessionRepository;
import cn.edu.zjut.interview.repository.InterviewerRepository;
import cn.edu.zjut.interview.repository.SessionImpressionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * 面试印象落库验收：到点评估 → 写 session_impression → 推给前端 → 刷新可恢复。
 *
 * <p>LLM 用替身，绝不真调网络。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImpressionServiceTest {

    @Autowired
    private ImpressionService impressionService;
    @Autowired
    private ChatService chatService;
    @Autowired
    private InterviewSessionRepository sessionRepository;
    @Autowired
    private InterviewMessageRepository messageRepository;
    @Autowired
    private InterviewerRepository interviewerRepository;
    @Autowired
    private SessionImpressionRepository impressionRepository;

    @MockBean
    private LlmClient llmClient;

    @Test
    void evaluatesAtThirdAnswerAndPersists() {
        when(llmClient.complete(anyList())).thenReturn("""
                {"positive":{"professionalism":4,"depth":3},
                 "negative":{"impatience":2},
                 "reason":"原理讲得比较透，但铺垫太长"}""");

        Interviewer interviewer = interviewerRepository.findByCode("strict").orElseThrow();
        InterviewSession session = createSession(interviewer, 3);

        Optional<ImpressionView> first = impressionService.evaluateIfDue(session, interviewer);
        assertTrue(first.isPresent(), "第 3 条回答后应该产生第一次评估");
        ImpressionView view = first.get();
        assertEquals(4, view.positive().get("professionalism"));
        assertEquals(3, view.positive().get("depth"));
        assertEquals(0, view.positive().get("clarity"));
        assertEquals(2, view.negative().get("impatience"));
        assertEquals(4, view.positiveDeltas().get("professionalism"));
        assertEquals("原理讲得比较透，但铺垫太长", view.reason());
        assertTrue(view.evaluated());

        int roleId = interviewer.getId().intValue();
        var row = impressionRepository.findBySessionIdAndRoleId(session.getId(), roleId).orElseThrow();
        assertEquals(4, row.getProfessionalism());
        assertEquals(2, row.getImpatience());
        assertEquals(3, row.getEvaluatedUserCount());

        // 同一条消息数不能重复评估
        assertTrue(impressionService.evaluateIfDue(session, interviewer).isEmpty());

        // 未到下一档（4、5 条）也不评估
        appendUserMessages(session, 4, 5);
        assertTrue(impressionService.evaluateIfDue(session, interviewer).isEmpty());

        // 刷新恢复：state 读回同一份
        ImpressionView state = impressionService.state(session.getId());
        assertEquals(4, state.positive().get("professionalism"));
        assertEquals(2, state.negative().get("impatience"));
    }

    @Test
    void skipsEvaluationWhenModelReturnsGarbage() {
        when(llmClient.complete(anyList())).thenReturn("抱歉，我无法评估");

        Interviewer interviewer = interviewerRepository.findByCode("strict").orElseThrow();
        InterviewSession session = createSession(interviewer, 3);

        assertTrue(impressionService.evaluateIfDue(session, interviewer).isEmpty(), "解析不了就不调整");
        assertTrue(impressionRepository.findBySessionIdAndRoleId(
                session.getId(), interviewer.getId().intValue()).isEmpty(), "失败不落库");
    }

    @Test
    void stateReturnsZerosBeforeAnyEvaluation() {
        Interviewer interviewer = interviewerRepository.findByCode("strict").orElseThrow();
        InterviewSession session = createSession(interviewer, 1);

        ImpressionView view = impressionService.state(session.getId());
        assertFalse(view.evaluated());
        assertEquals(0, view.positive().get("potential"));
        assertEquals(0, view.negative().get("reservation"));
        assertEquals(6, view.positive().size());
        assertEquals(6, view.negative().size());
    }

    /**
     * 剧本链路：旁白 / 剧情提示也落成 role=user，但不能算成候选人回答；
     * 且「距上次评估攒够 3 条」就触发，不需要总数正好被 3 整除。
     */
    @Test
    void ignoresNarrationAndCatchesUpAfterMidSession() {
        when(llmClient.complete(anyList())).thenReturn("""
                {"positive":{"clarity":2},"negative":{},"reason":"表达清楚"}""");

        Interviewer interviewer = interviewerRepository.findByCode("strict").orElseThrow();
        InterviewSession session = createSession(interviewer, 3);
        appendNarration(session, 10, 12);

        assertTrue(impressionService.evaluateIfDue(session.getId()).isPresent(),
                "3 条真实回答就该评估，旁白不参与计数");

        appendNarration(session, 13, 15);
        assertTrue(impressionService.evaluateIfDue(session.getId()).isEmpty(),
                "只新增旁白时不应再次评估");
    }

    /** 建一个场次并落 {@code totalUserMessages} 条候选人消息。 */
    private InterviewSession createSession(Interviewer interviewer, int totalUserMessages) {
        Long sessionId = chatService.createSession(
                new CreateSessionRequest(interviewer.getCode(), "java", null)).sessionId();
        InterviewSession session = sessionRepository.findById(sessionId).orElseThrow();
        appendUserMessages(session, 1, totalUserMessages);
        return session;
    }

    private void appendUserMessages(InterviewSession session, int fromSeq, int toSeq) {
        for (int seq = fromSeq; seq <= toSeq; seq++) {
            InterviewMessage message = new InterviewMessage();
            message.setSessionId(session.getId());
            message.setSeq(seq);
            message.setRole(InterviewMessage.ROLE_USER);
            message.setContent("第 " + seq + " 条回答：我用线程池做过异步任务");
            message.setSenderRoleId(0);
            messageRepository.save(message);
        }
    }

    /** 引擎把旁白 / 剧情提示写成 role=user + msgType=narration，印象评估必须排掉。 */
    private void appendNarration(InterviewSession session, int fromSeq, int toSeq) {
        for (int seq = fromSeq; seq <= toSeq; seq++) {
            InterviewMessage message = new InterviewMessage();
            message.setSessionId(session.getId());
            message.setSeq(seq);
            message.setRole(InterviewMessage.ROLE_USER);
            message.setMsgType("narration");
            message.setContent("风雪抬眼看你。");
            messageRepository.save(message);
        }
    }
}
