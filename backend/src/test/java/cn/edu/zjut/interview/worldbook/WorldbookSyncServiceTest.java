package cn.edu.zjut.interview.worldbook;

import cn.edu.zjut.interview.domain.WorldbookBook;
import cn.edu.zjut.interview.repository.WorldbookBookRepository;
import cn.edu.zjut.interview.repository.WorldbookEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 世界书扫描（B2）：重复扫描幂等、格式错误跳过、条目字段与 YAML 对齐。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorldbookSyncServiceTest {

    @TempDir
    Path tempDir;

    @Autowired
    private WorldbookBookRepository bookRepository;
    @Autowired
    private WorldbookEntryRepository entryRepository;

    private WorldbookSyncService service() {
        return new WorldbookSyncService(bookRepository, entryRepository, tempDir.toString());
    }

    @Test
    void syncIsIdempotentAndKeepsFileOrder() throws Exception {
        Files.writeString(tempDir.resolve("demo.yml"), """
                name: 演示书
                description: 测试用
                entries:
                  - id: first
                    activation: { type: always }
                    content: 常驻
                  - id: second
                    name: 第二条
                    priority: 5
                    activation: { type: keyword, keys: [家庭] }
                    content: 家庭设定
                """, StandardCharsets.UTF_8);

        assertEquals(1, service().syncAll());
        assertEquals(1, service().syncAll());

        WorldbookBook book = bookRepository.findByCode("demo").orElseThrow();
        assertEquals("演示书", book.getName());
        assertEquals("测试用", book.getDescription());
        assertEquals("demo.yml", book.getSourcePath());
        assertTrue(book.getEnabled());
        assertEquals(2, entryRepository.countByBookId(book.getId()));

        var entries = entryRepository.findByBookIdOrderBySortOrderAscIdAsc(book.getId());
        assertEquals("first", entries.get(0).getEntryKey());
        assertEquals("always", entries.get(0).getActivationType());
        assertEquals("[]", entries.get(0).getActivationKeys());
        assertEquals("second", entries.get(1).getEntryKey());
        assertEquals("keyword", entries.get(1).getActivationType());
        assertTrue(entries.get(1).getActivationKeys().contains("家庭"));
        assertEquals(5, entries.get(1).getPriority());
    }

    @Test
    void invalidFileIsSkippedWithoutThrowing() throws Exception {
        Files.writeString(tempDir.resolve("bad.yml"), "entries: [invalid]", StandardCharsets.UTF_8);

        assertEquals(0, service().syncAll());
        assertFalse(bookRepository.findByCode("bad").isPresent());
    }

    @Test
    void missingDirectorySyncsNothing() {
        WorldbookSyncService service = new WorldbookSyncService(
                bookRepository, entryRepository, tempDir.resolve("nope").toString());
        assertEquals(0, service.syncAll());
    }
}
