package cn.edu.zjut.interview.repository;

import cn.edu.zjut.interview.domain.WorldbookBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** {@code worldbook_book}：世界书目录（真相源是 {@code game_data/worldbooks/*.yml}）。 */
public interface WorldbookBookRepository extends JpaRepository<WorldbookBook, Long> {

    Optional<WorldbookBook> findByCode(String code);

    List<WorldbookBook> findAllByOrderBySortOrderAscCodeAsc();
}
