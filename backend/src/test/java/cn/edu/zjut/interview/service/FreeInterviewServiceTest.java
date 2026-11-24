package cn.edu.zjut.interview.service;

import cn.edu.zjut.interview.domain.InterviewScript;
import cn.edu.zjut.interview.domain.InterviewSession;
import cn.edu.zjut.interview.domain.Interviewer;
import cn.edu.zjut.interview.dto.FreeStateView;
import cn.edu.zjut.interview.repository.InterviewQuestionRepository;
import cn.edu.zjut.interview.repository.InterviewScriptRepository;
import cn.edu.zjut.interview.repository.InterviewSessionRepository;
import cn.edu.zjut.interview.repository.InterviewerRepository;
import cn.edu.zjut.interview.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自由面试（issue #11）验收：岗位识别、随机抽题不重复、剧本场次不被影响。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FreeInterviewServiceTest {

    @Autowired
    private FreeInterviewService freeInterviewService;
    @Autowired
    private InterviewSessionRepository sessionRepository;
    @Autowired
    private InterviewQuestionRepository questionRepository;
    @Autowired
    private InterviewScriptRepository scriptRepository;
    @Autowired
    private InterviewerRepository interviewerRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void detectsJobFromSelfIntroductionAndAssignsFirstQuestion() {
        InterviewSession session = freeSession(null);

        FreeInterviewService.FreeProgress progress =
                freeInterviewService.prepare(session, "我想面 Web 前端，平时写 Vue", 1);

        assertTrue(progress.active());
        assertEquals("frontend", progress.jobCode());
        assertEquals("Web 前端", progress.jobName());
        assertNotNull(progress.question());
        // 不写死题量：题库会随 questions-h2.sql 扩题，断言「与该岗位实际条数一致」即可
        assertEquals(bankSize("frontend"), progress.questionCount());
        assertEquals(1, progress.askedCount());
    }

    @Test
    void keepsAskingUntilJobIsClear() {
        InterviewSession session = freeSession(null);

        FreeInterviewService.FreeProgress progress = freeInterviewService.prepare(session, "你好", 1);
        assertTrue(progress.active());
        assertNull(progress.jobCode(), "第一句没说岗位时先不抽题");
        assertNull(progress.question());

        // 第二句仍没说清 → 回落到设置页岗位，面试不会一直卡在确认
        progress = freeInterviewService.prepare(session, "都可以，你看着来", 2);
        assertEquals("java", progress.jobCode());
        assertNotNull(progress.question());
    }

    @Test
    void rotatesThroughWholeBankWithoutRepeating() {
        InterviewSession session = freeSession(null);
        FreeInterviewService.FreeProgress progress = freeInterviewService.prepare(session, "Java 后端开发", 1);
        assertEquals("java", progress.jobCode());

        int bankSize = bankSize("java");
        Set<Long> seen = new HashSet<>();
        seen.add(progress.question().getId());
        // 每 TURNS_PER_QUESTION+1 次调用换一题，聊到覆盖整份题池为止（留足上限防死循环）
        int guard = 0;
        int round = 1;
        while (seen.size() < bankSize && guard++ < bankSize * 10) {
            progress = freeInterviewService.prepare(session, "继续", ++round);
            if (progress.question() != null) {
                seen.add(progress.question().getId());
            }
        }
        assertEquals(bankSize, seen.size(), "一轮内不应重复出题，且应覆盖整份题池");
    }

    @Test
    void scriptSessionIsNotTouched() {
        InterviewScript script = scriptRepository.findAll().stream().findFirst().orElseThrow();
        InterviewSession session = freeSession(script.getId());

        FreeInterviewService.FreeProgress progress = freeInterviewService.prepare(session, "我想面前端", 1);

        assertFalse(progress.active());
        assertNull(session.getFreeJobCode(), "剧本场次不应写自由面试状态");
        assertEquals(0, session.getFreeQuestionTurns());
    }

    @Test
    void stateExposesCurrentQuestionForFrontend() {
        InterviewSession session = freeSession(null);
        freeInterviewService.prepare(session, "我想面 Java 后端", 1);

        FreeStateView view = freeInterviewService.state(session.getId());

        assertTrue(view.free());
        assertEquals("java", view.jobCode());
        assertEquals("Java 后端", view.jobName());
        assertEquals(1, view.askedCount());
        assertEquals(bankSize("java"), view.questionCount());
        assertNotNull(view.question());
        assertEquals(session.getFreeCurrentQuestionId(), view.question().id());
        assertFalse(view.question().text().isEmpty());
    }

    @Test
    void jobDetectionPrefersExplicitFrontendWords() {
        assertEquals("frontend", FreeInterviewService.detectJob("前端，React 和 TypeScript"));
        assertEquals("java", FreeInterviewService.detectJob("Java 后端，Spring 和 JVM"));
        assertNull(FreeInterviewService.detectJob("随便聊聊"));
    }

    @Test
    void jobDetectionCoversPythonGoAndTesting() {
        // 「后端」是泛化词，不能把 Python 抢成 Java
        assertEquals("python", FreeInterviewService.detectJob("我想面 Python 后端，写过 Django"));
        assertEquals("go", FreeInterviewService.detectJob("我想面 Go 后端"));
        assertEquals("go", FreeInterviewService.detectJob("用 Golang 做过服务"));
        assertEquals("testing", FreeInterviewService.detectJob("想做测试开发，会写接口测试"));
        // 泛化词兜底保持原行为：只说「后端」仍按 Java
        assertEquals("java", FreeInterviewService.detectJob("我想做后端"));
    }

    @Test
    void jobDetectionUsesAsciiWordBoundaries() {
        // JavaScript 里的 java、django 里的 go 都不应该被当成关键词命中
        assertEquals("frontend", FreeInterviewService.detectJob("平时写 JavaScript 和 React"));
        assertEquals("python", FreeInterviewService.detectJob("Django 项目"));
    }

    /** 该岗位题库的实际条数：测试不写死数字，扩题时不用改用例。 */
    private int bankSize(String jobCode) {
        return questionRepository.findByJobCodeAndEnabledTrueOrderBySortOrderAscIdAsc(jobCode).size();
    }

    private InterviewSession freeSession(Long scriptId) {
        Interviewer interviewer = interviewerRepository.findByCode("strict").orElseThrow();
        InterviewSession session = new InterviewSession();
        session.setTitle("自由面试测试");
        session.setUserId(userProfileRepository.findFirstByOrderByIdAsc().orElseThrow().getId());
        session.setInterviewerId(interviewer.getId());
        session.setScriptId(scriptId);
        session.setJobCode("java");
        session.setStatus("running");
        return sessionRepository.save(session);
    }
}
