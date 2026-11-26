package cn.edu.zjut.interview.repository;

import cn.edu.zjut.interview.domain.WorldbookBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** {@code worldbook_book}：世界书目录（真相源是 {@code game_data/worldbooks/*.yml}）。 */
public interface WorldbookBookRepository extends JpaRepository<WorldbookBook, Long> {

    Optional<WorldbookBook> findByCode(String code);

    List<WorldbookBook> findAllByOrderBySortOrderAscCodeAsc();

    List<WorldbookBook> findByJobCodeOrderBySortOrderAscCodeAsc(String jobCode);

    /** 按岗位取启用的书（注入用；多本时按 sort_order 取第一本）。 */
    List<WorldbookBook> findByJobCodeAndEnabledTrueOrderBySortOrderAscCodeAsc(String jobCode);
}
