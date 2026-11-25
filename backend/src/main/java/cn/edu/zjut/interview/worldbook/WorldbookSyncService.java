package cn.edu.zjut.interview.worldbook;

import cn.edu.zjut.interview.domain.WorldbookBook;
import cn.edu.zjut.interview.domain.WorldbookEntry;
import cn.edu.zjut.interview.repository.WorldbookBookRepository;
import cn.edu.zjut.interview.repository.WorldbookEntryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 世界书目录 → 数据库的启动同步（issue #1 世界书半）。
 *
 * <p>真相源是磁盘上的 {@code game_data/worldbooks/<code>.yml}，表只是可查询的视图。
 * 每本书 {code} 取自文件名；书内条目整批重建（先删后插），字段与
 * {@link Worldbook.Entry} 一一对应。
 *
 * <p>解析失败 / 空文件 / 非法 YAML 一律 warn + 跳过，<b>不中断启动</b>：
 * 一本写坏的书不该让整个后端起不来（与角色卡同步同一原则）。
 */
@Service
public class WorldbookSyncService {

    private static final Logger log = LoggerFactory.getLogger(WorldbookSyncService.class);

    /** 世界书条目里的数组（关键词）统一 JSON 存 CLOB，与项目其它数组字段一致。 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WorldbookBookRepository bookRepository;
    private final WorldbookEntryRepository entryRepository;

    /** 世界书根目录：{@code app.worldbooks-dir} 解析后的绝对路径。 */
    private final Path root;

    public WorldbookSyncService(WorldbookBookRepository bookRepository,
                                WorldbookEntryRepository entryRepository,
                                @Value("${app.worldbooks-dir}") String worldbooksDir) {
        this.bookRepository = bookRepository;
        this.entryRepository = entryRepository;
        this.root = resolveRoot(worldbooksDir);
        log.info("[WorldbookSyncService] 世界书目录 = {}", root);
    }

    /**
     * 扫描目录下全部 {@code .yml/.yaml} 并整份重建。
     *
     * @return 成功入库的书本数（解析失败的跳过，不计入）
     */
    @Transactional
    public int syncAll() {
        List<Path> files = listYamlFiles();
        int synced = 0;
        for (int i = 0; i < files.size(); i++) {
            Path file = files.get(i);
            Worldbook book = read(file);
            if (book == null) {
                log.warn("[WorldbookSyncService] {} 不是合法世界书（空或格式错误），跳过", file.getFileName());
                continue;
            }
            saveBook(file, book, i);
            synced++;
        }
        return synced;
    }

    private Worldbook read(Path file) {
        try {
            return Worldbook.parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("[WorldbookSyncService] 读取 {} 失败，跳过：{}", file.getFileName(), e.toString());
            return null;
        }
    }

    /** 一本书一个事务边界内不需要单独事务：整批同步共用一个事务。 */
    private void saveBook(Path file, Worldbook book, int order) {
        String code = stripExtension(file.getFileName().toString());
        WorldbookBook row = bookRepository.findByCode(code).orElseGet(WorldbookBook::new);
        boolean isNew = row.getId() == null;
        row.setCode(code);
        row.setName(book.name() == null || book.name().isBlank() ? code : book.name());
        row.setDescription(book.description());
        row.setSourcePath(relativePath(file));
        row.setSortOrder(order);
        row.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            // 已有书保留库里的人工开关，不因重启被重新打开
            row.setEnabled(Boolean.TRUE);
        }
        row = bookRepository.save(row);
        // 自增 id 要先刷出，条目删除/插入才有 book_id 可用
        bookRepository.flush();

        entryRepository.flush();
        entryRepository.deleteByBookId(row.getId());
        entryRepository.flush();

        List<Worldbook.Entry> entries = dedupeByKey(book.entries());
        for (int i = 0; i < entries.size(); i++) {
            Worldbook.Entry src = entries.get(i);
            WorldbookEntry entry = new WorldbookEntry();
            entry.setBookId(row.getId());
            entry.setEntryKey(src.id());
            entry.setName(src.name());
            entry.setEnabled(src.enabled());
            entry.setPriority(src.priority());
            entry.setActivationType(src.always() ? "always" : "keyword");
            entry.setActivationKeys(writeKeys(src.activationKeys()));
            entry.setContent(src.content());
            entry.setSortOrder(i);
            entryRepository.save(entry);
        }
        entryRepository.flush();
        log.info("[WorldbookSyncService] 已同步世界书 {}（{} 条）", code, entries.size());
    }

    /** 同一本书内 entry.id 重复会撞唯一约束：保留最后一条并 warn，别让手滑写坏启动。 */
    private static List<Worldbook.Entry> dedupeByKey(List<Worldbook.Entry> entries) {
        Map<String, Worldbook.Entry> unique = new LinkedHashMap<>();
        for (Worldbook.Entry entry : entries) {
            if (unique.put(entry.id(), entry) != null) {
                log.warn("[WorldbookSyncService] 条目 id 重复，后者覆盖前者：{}", entry.id());
            }
        }
        return List.copyOf(unique.values());
    }

    private static String writeKeys(List<String> keys) {
        try {
            return MAPPER.writeValueAsString(keys);
        } catch (JsonProcessingException e) {
            // String 列表不可能序列化失败；兜底成空数组，别让整本跳过
            return "[]";
        }
    }

    private String relativePath(Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        try {
            return root.relativize(absolute).toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            // 不在根目录下（理论上不会）：退回文件名，至少能看出是哪本
            return file.getFileName().toString();
        }
    }

    private List<Path> listYamlFiles() {
        if (!Files.isDirectory(root)) {
            log.warn("[WorldbookSyncService] 世界书目录不存在: {}", root);
            return List.of();
        }
        try (Stream<Path> children = Files.list(root)) {
            return children
                    .filter(Files::isRegularFile)
                    .filter(WorldbookSyncService::isYaml)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            log.warn("[WorldbookSyncService] 读取世界书目录失败 {}：{}", root, e.getMessage());
            return List.of();
        }
    }

    private static boolean isYaml(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /** 与 CharacterCardService 同一套目录解析：先按字面量，再退到上一级（两种工作目录都能命中）。 */
    private static Path resolveRoot(String worldbooksDir) {
        Path direct = Path.of(worldbooksDir).toAbsolutePath().normalize();
        if (Files.isDirectory(direct)) {
            return direct;
        }
        Path fromParent = Path.of("..").resolve(worldbooksDir).toAbsolutePath().normalize();
        if (Files.isDirectory(fromParent)) {
            return fromParent;
        }
        log.warn("[WorldbookSyncService] 世界书目录 {} 不存在（也未在上一级找到），按字面量使用", direct);
        return direct;
    }
}
