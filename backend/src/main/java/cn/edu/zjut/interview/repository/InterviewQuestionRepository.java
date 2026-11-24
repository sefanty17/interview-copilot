package cn.edu.zjut.interview.repository;

import cn.edu.zjut.interview.domain.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * {@code interview_question}：岗位题库。
 *
 * <p>两个用途：
 * <ol>
 *   <li>自由面试抽题（{@link FreeInterviewService}）——只要启用中的题，按 sort_order 取</li>
 *   <li>题库只读接口（{@code GET /api/questions}）——支持按题型过滤</li>
 * </ol>
 *
 * <p>注意 {@code type} 列存的是<b>中文标签</b>（「技术知识」），按题型过滤时传的也是中文，
 * 英文 code 到中文标签的转换在 {@code QuestionType} 里做。
 */
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    /** 某岗位的可用题库，按种子顺序取（抽题时再在内存里随机打散）。 */
    List<InterviewQuestion> findByJobCodeAndEnabledTrueOrderBySortOrderAscIdAsc(String jobCode);

    /** 某岗位 + 某题型的可用题目（{@code type} 传中文标签）。 */
    List<InterviewQuestion> findByJobCodeAndTypeAndEnabledTrueOrderBySortOrderAscIdAsc(
            String jobCode, String type);

    /** 全部题目（含停用），后台维护用。 */
    List<InterviewQuestion> findByJobCodeOrderBySortOrderAscIdAsc(String jobCode);
}
