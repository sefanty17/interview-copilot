package cn.edu.zjut.interview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 世界书「书」级记录，对应 DDL {@code worldbook_book}（issue #1 世界书半）。
 *
 * <p>真相源是磁盘上的 {@code game_data/worldbooks/<code>.yml}：启动时由
 * {@code WorldbookSyncService} 扫描入库，这张表只是可查询的视图，删掉重启即重建。
 * 与 {@link Interviewer#getWorldbook()} 不冲突：那边是角色卡自带的一整份 YAML 原文。
 */
@Entity
@Table(name = "worldbook_book")
public class WorldbookBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 书名标识，取自 YAML 文件名（不含扩展名），如 {@code java}。 */
    @Column(name = "code", length = 64, nullable = false, unique = true)
    private String code;

    /** 岗位编码（{@code interview_session.job_code}）：按岗位查书注入；YAML 没写时同步回填文件名。 */
    @Column(name = "job_code", length = 32)
    private String jobCode;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "description", columnDefinition = "CLOB")
    private String description;

    /** 来源文件相对世界书目录的路径，排查「这本是从哪来的」用。 */
    @Column(name = "source_path", length = 500)
    private String sourcePath;

    @Column(name = "enabled")
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getJobCode() {
        return jobCode;
    }

    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
