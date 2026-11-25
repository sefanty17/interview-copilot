package cn.edu.zjut.interview.api;

import cn.edu.zjut.interview.domain.WorldbookBook;
import cn.edu.zjut.interview.dto.worldbook.WorldbookDetail;
import cn.edu.zjut.interview.dto.worldbook.WorldbookEntryView;
import cn.edu.zjut.interview.dto.worldbook.WorldbookSummary;
import cn.edu.zjut.interview.repository.WorldbookBookRepository;
import cn.edu.zjut.interview.repository.WorldbookEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 世界书只读接口（issue #1 世界书半）。
 *
 * <p>数据来自启动扫描的 {@code worldbook_book / worldbook_entry}，真相源是
 * {@code game_data/worldbooks/*.yml}。列表只给目录信息，详情才带条目正文；
 * 找不到一律 404，返回体与 {@code InterviewerController} 一致：
 * {@code {"error":"not_found","code":"..."}}。
 */
@RestController
@RequestMapping("/api/worldbooks")
public class WorldbookController {

    private final WorldbookBookRepository bookRepository;
    private final WorldbookEntryRepository entryRepository;

    public WorldbookController(WorldbookBookRepository bookRepository,
                               WorldbookEntryRepository entryRepository) {
        this.bookRepository = bookRepository;
        this.entryRepository = entryRepository;
    }

    @GetMapping
    public List<WorldbookSummary> list() {
        return bookRepository.findAllByOrderBySortOrderAscCodeAsc().stream()
                .map(book -> WorldbookSummary.of(book, entryRepository.countByBookId(book.getId())))
                .toList();
    }

    @GetMapping("/{code}")
    public ResponseEntity<Object> detail(@PathVariable String code) {
        WorldbookBook book = bookRepository.findByCode(code).orElse(null);
        if (book == null) {
            return notFound(code);
        }
        List<WorldbookEntryView> entries = entryRepository
                .findByBookIdOrderBySortOrderAscIdAsc(book.getId()).stream()
                .map(WorldbookEntryView::from)
                .toList();
        return ResponseEntity.ok(WorldbookDetail.of(book, entries));
    }

    private static ResponseEntity<Object> notFound(String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "not_found");
        body.put("code", code);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
