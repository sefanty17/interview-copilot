package cn.edu.zjut.interview.api;

import cn.edu.zjut.interview.domain.InterviewQuestion;
import cn.edu.zjut.interview.dto.QuestionView;
import cn.edu.zjut.interview.question.QuestionType;
import cn.edu.zjut.interview.repository.InterviewQuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * 岗位题库只读接口（课设指标 ① / issue #1 的「题库」部分）。
 *
 * <pre>
 * GET /api/questions?jobCode=java&amp;type=technical&amp;limit=20&amp;offset=0
 * </pre>
 *
 * <p>为什么要有这个接口：题目原先只写在前端 {@code data/questions.ts}，后端看不到也改不了，
 * 验收要求的「访问 /api/questions 返回不少于 20 题」无从满足。有了它题库才变成可维护的数据。
 *
 * <p>错误语义（都用中文，前端的 {@code readError} 直接展示）：
 * <ul>
 *   <li>缺 {@code jobCode} → 400（不返回整库，避免调用方误拿全量数据）</li>
 *   <li>未知 {@code type} → 400 并列出可选取值</li>
 *   <li>岗位没录题 → 200 + 空数组（「这个岗位还没题」是正常状态，不是接口不存在）</li>
 * </ul>
 *
 * <p>{@code limit} 默认 20、上限 100。默认值取 20 是因为 issue 的验收口径就是
 * 「每个岗位不少于 20 题」，不传参也该拿到够展示的量；上限 100 防止前端一次把整库拉走。
 */
@RestController
@RequestMapping("/api")
public class QuestionController {

    private static final Logger log = LoggerFactory.getLogger(QuestionController.class);

    /** 不传 limit 时的条数：对齐 issue #1 的「每岗位 ≥20 题」验收口径。 */
    private static final int DEFAULT_LIMIT = 20;

    /** 上限：题库真的过千时再改成游标分页。 */
    private static final int MAX_LIMIT = 100;

    private final InterviewQuestionRepository repository;
    private final ObjectMapper objectMapper;

    public QuestionController(InterviewQuestionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * 按岗位取题。
     *
     * @param jobCode 岗位编码（java / frontend），必填
     * @param type    题型，可选；英文 code 或中文标签都认
     * @param limit   返回条数，默认 20、最多 100
     * @param offset  跳过的条数，默认 0；与 limit 配合做简单分页
     */
    @GetMapping("/questions")
    public List<QuestionView> list(
            @RequestParam(name = "jobCode", required = false) String jobCode,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "offset", required = false) Integer offset) {

        if (jobCode == null || jobCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jobCode 不能为空（例：java）");
        }

        QuestionType questionType;
        try {
            questionType = QuestionType.parse(type);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        String code = jobCode.trim();
        // 库列存中文标签，所以按题型过滤时传的是 label 而不是 code
        List<InterviewQuestion> rows = questionType == null
                ? repository.findByJobCodeAndEnabledTrueOrderBySortOrderAscIdAsc(code)
                : repository.findByJobCodeAndTypeAndEnabledTrueOrderBySortOrderAscIdAsc(
                        code, questionType.label());

        int size = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        int from = offset == null || offset < 0 ? 0 : Math.min(offset, rows.size());
        int to = Math.min(from + size, rows.size());

        List<QuestionView> views = new ArrayList<>(to - from);
        for (InterviewQuestion row : rows.subList(from, to)) {
            views.add(QuestionView.of(row, parseFollowUps(row)));
        }
        log.debug("[Question] jobCode={} type={} → {} / {} 题", code,
                questionType == null ? "-" : questionType.code(), views.size(), rows.size());
        return views;
    }

    /**
     * {@code follow_ups} 是 JSON 数组字符串。
     *
     * <p>脏数据只影响这一条（返回空列表并不让它整份 500）：题库是人工维护的，
     * 一条手滑写坏的 JSON 不该让「题库页打不开」。
     */
    private List<String> parseFollowUps(InterviewQuestion question) {
        String json = question.getFollowUps();
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("[Question] code={} 的 follow_ups 不是合法 JSON 数组，按空列表返回：{}",
                    question.getCode(), e.getMessage());
            return List.of();
        }
    }
}
