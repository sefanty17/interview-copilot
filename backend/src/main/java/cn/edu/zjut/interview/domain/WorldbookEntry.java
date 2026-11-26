package cn.edu.zjut.interview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 世界书「条目」，对应 DDL {@code worldbook_entry}（issue #1 世界书半）。
 *
 * <p>字段与 {@code Worldbook.Entry} 一一对应；{@code activationType} 只有
 * {@code always} / {@code keyword} 两种，关键词以 JSON 数组字符串存
 * {@code activation_keys}（本项目数组一律 JSON 存 CLOB）。
 */
@Entity
@Table(name = "worldbook_entry")
public class WorldbookEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 所属书，逻辑外键（不建 FK 约束，重建时按 book_id 整批删除）。 */
    @Column(name = "book_id", nullable = false)
    private Long bookId;

    /** YAML 里的 {@code entry.id}，书内唯一。 */
    @Column(name = "entry_key", length = 64, nullable = false)
    private String entryKey;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "enabled")
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "activation_type", length = 16, nullable = false)
    private String activationType;

    /** JSON 数组字符串；always 条目为 {@code []}。 */
    @Column(name = "activation_keys", columnDefinition = "CLOB")
    private String activationKeys;

    @Column(name = "content", columnDefinition = "CLOB")
    private String content;

    /** 文件内顺序，详情接口按它还原 YAML 顺序。 */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getEntryKey() {
        return entryKey;
    }

    public void setEntryKey(String entryKey) {
        this.entryKey = entryKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getActivationType() {
        return activationType;
    }

    public void setActivationType(String activationType) {
        this.activationType = activationType;
    }

    public String getActivationKeys() {
        return activationKeys;
    }

    public void setActivationKeys(String activationKeys) {
        this.activationKeys = activationKeys;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
