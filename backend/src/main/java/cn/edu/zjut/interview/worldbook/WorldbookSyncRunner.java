package cn.edu.zjut.interview.worldbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时把 {@code game_data/worldbooks/} 扫描进 {@code worldbook_book / worldbook_entry}。
 *
 * <p>与 {@code CharacterCardSyncRunner} 同一约定：任何异常只 warn，<b>不影响启动</b>。
 * 世界书只是「可查询视图」，同步失败时旧数据仍然可用。
 */
@Component
public class WorldbookSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorldbookSyncRunner.class);

    private final WorldbookSyncService service;

    public WorldbookSyncRunner(WorldbookSyncService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int count = service.syncAll();
            log.info("[WorldbookSyncRunner] 世界书同步完成：{} 本", count);
        } catch (Exception e) {
            log.warn("[WorldbookSyncRunner] 世界书同步失败（不影响启动）：{}", e.getMessage(), e);
        }
    }
}
