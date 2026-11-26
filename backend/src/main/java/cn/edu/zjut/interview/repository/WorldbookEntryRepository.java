package cn.edu.zjut.interview.repository;

import cn.edu.zjut.interview.domain.WorldbookEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** {@code worldbook_entry}：世界书条目，随启动扫描整批重建。 */
public interface WorldbookEntryRepository extends JpaRepository<WorldbookEntry, Long> {

    List<WorldbookEntry> findByBookIdOrderBySortOrderAscIdAsc(Long bookId);

    long countByBookId(Long bookId);

    /**
     * 重建某本书的全部条目。
     *
     * <p>批量删除走 JPQL 直接下 DELETE，不经过持久化上下文；调用方
     * （{@code WorldbookSyncService}）已加 {@code @Transactional}，无需在这里再标事务。
     */
    @Modifying
    @Query("delete from WorldbookEntry e where e.bookId = :bookId")
    void deleteByBookId(@Param("bookId") Long bookId);
}
