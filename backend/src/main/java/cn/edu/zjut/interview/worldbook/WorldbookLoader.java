package cn.edu.zjut.interview.worldbook;

import cn.edu.zjut.interview.domain.WorldbookBook;
import cn.edu.zjut.interview.domain.WorldbookEntry;
import cn.edu.zjut.interview.repository.WorldbookBookRepository;
import cn.edu.zjut.interview.repository.WorldbookEntryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 按岗位把 {@code worldbook_book / worldbook_entry} 组装成可注入的 {@link Worldbook}。
 *
 * <p>真相源是磁盘 YAML，两张表由启动同步维护；这里只读不写。数据量很小（当前 2 本 / 8 条），
 * 不做缓存，每次注入前查一次库，改 YAML 重启即生效。
 *
 * <p>放在 worldbook 包内是为了直接使用 {@link Worldbook.Entry} 与
 * {@link Worldbook.Activation} 的包内构造，不对外暴露内部结构。
 */
@Component
public class WorldbookLoader {

    private static final Logger log = LoggerFactory.getLogger(WorldbookLoader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> KEY_LIST = new TypeReference<>() {
    };

    private final WorldbookBookRepository bookRepository;
    private final WorldbookEntryRepository entryRepository;

    public WorldbookLoader(WorldbookBookRepository bookRepository,
                           WorldbookEntryRepository entryRepository) {
        this.bookRepository = bookRepository;
        this.entryRepository = entryRepository;
    }

    /**
     * 取该岗位启用的第一本书（按 sort_order）并还原全部启用条目。
     *
     * @return 无岗位 / 无书 / 无启用条目时返回 null，调用方零开销跳过
     */
    public Worldbook forJob(String jobCode) {
        if (jobCode == null || jobCode.isBlank()) {
            return null;
        }
        List<WorldbookBook> books =
                bookRepository.findByJobCodeAndEnabledTrueOrderBySortOrderAscCodeAsc(jobCode.trim());
        if (books.isEmpty()) {
            return null;
        }
        WorldbookBook book = books.get(0);
        List<Worldbook.Entry> entries = new ArrayList<>();
        for (WorldbookEntry row : entryRepository.findByBookIdOrderBySortOrderAscIdAsc(book.getId())) {
            if (!Boolean.TRUE.equals(row.getEnabled())) {
                continue;
            }
            Worldbook.Activation activation = "always".equalsIgnoreCase(row.getActivationType())
                    ? Worldbook.Activation.always()
                    : Worldbook.Activation.keyword(readKeys(row.getActivationKeys()));
            entries.add(new Worldbook.Entry(
                    row.getEntryKey(),
                    row.getName(),
                    true,
                    row.getPriority() == null ? 0 : row.getPriority(),
                    activation,
                    row.getContent()));
        }
        if (entries.isEmpty()) {
            return null;
        }
        return Worldbook.of(book.getName(), book.getDescription(), book.getJobCode(), entries);
    }

    /** activation_keys 是 JSON 数组字符串；坏数据当空数组，别让整本书失效。 */
    private static List<String> readKeys(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, KEY_LIST);
        } catch (Exception e) {
            log.warn("[WorldbookLoader] 关键词 JSON 解析失败，按空处理：{}", e.getMessage());
            return List.of();
        }
    }
}
