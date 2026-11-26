package cn.edu.zjut.interview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 面试题库，对应 DDL {@code interview_question}（本项目扩展）。
 *
 * <p>自由面试不再用写死的人设随便聊：面试官先问候选人想面什么岗位，
 * 再从这里按 {@code job_code} 随机抽题，围绕题目自然交流。
 */
@Entity
@Table(name = "interview_question")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 稳定业务编码（种子数据用它做幂等 MERGE 的 KEY）。 */
    @Column(name = "code", length = 32, nullable = false, unique = true)
    private String code;

    @Column(name = "job_code", length = 32, nullable = false)
    private String jobCode;

    /** 知识点编码，与前端 {@code data/jobs.ts} 的 knowledgePoints.id 同一口径。 */
    @Column(name = "kp", length = 32)
    private String kp;

    @Column(name = "type", length = 32)
    private String type;

    @Column(name = "difficulty", length = 16)
    private String difficulty;

    @Column(name = "text", columnDefinition = "CLOB", nullable = false)
    private String text;

    /** 参考要点：只进 prompt，不给候选人和前端看。 */
    @Column(name = "reference", columnDefinition = "CLOB")
    private String reference;

    /** 备选追问，JSON 数组字符串。 */
    @Column(name = "follow_ups", columnDefinition = "CLOB")
    private String followUps;

    @Column(name = "enabled")
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

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

    public String getKp() {
        return kp;
    }

    public void setKp(String kp) {
        this.kp = kp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getFollowUps() {
        return followUps;
    }

    public void setFollowUps(String followUps) {
        this.followUps = followUps;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
