package cn.edu.zjut.interview.repository;

import cn.edu.zjut.interview.domain.SessionImpression;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionImpressionRepository extends JpaRepository<SessionImpression, Long> {

    /** 一个场次里每个面试官一条印象记录。 */
    Optional<SessionImpression> findBySessionIdAndRoleId(Long sessionId, Integer roleId);

    /** 删场次时先清印象，否则外键 fk_imp_sess 拦住删除。 */
    void deleteBySessionId(Long sessionId);
}
