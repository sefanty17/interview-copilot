package cn.edu.zjut.interview.repository;

import cn.edu.zjut.interview.domain.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    /** 某岗位的可用题库，按种子顺序取（抽题时再在内存里随机打散）。 */
    List<InterviewQuestion> findByJobCodeAndEnabledTrueOrderBySortOrderAscIdAsc(String jobCode);

    List<InterviewQuestion> findByJobCodeOrderBySortOrderAscIdAsc(String jobCode);
}
