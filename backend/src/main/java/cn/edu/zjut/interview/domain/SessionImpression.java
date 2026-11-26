package cn.edu.zjut.interview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 面试印象（按「场次 + 面试官」存）。
 *
 * <p>和角色卡的区别：角色卡是全局人设，印象是这一局里面试官对候选人的判断，
 * 换场次重新开始。数值 0-100；负面维度越高，面试官语气越紧。
 */
@Entity
@Table(name = "session_impression")
public class SessionImpression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @Column(name = "professionalism")
    private Integer professionalism;
    @Column(name = "clarity")
    private Integer clarity;
    @Column(name = "logic")
    private Integer logic;
    @Column(name = "depth")
    private Integer depth;
    @Column(name = "credibility")
    private Integer credibility;
    @Column(name = "potential")
    private Integer potential;

    @Column(name = "skepticism")
    private Integer skepticism;
    @Column(name = "confusion")
    private Integer confusion;
    @Column(name = "impatience")
    private Integer impatience;
    @Column(name = "disappointment")
    private Integer disappointment;
    @Column(name = "wariness")
    private Integer wariness;
    @Column(name = "reservation")
    private Integer reservation;

    @Column(name = "reason", length = 512)
    private String reason;

    /** 最近一次评估时的用户消息条数，避免同一轮重复评估。 */
    @Column(name = "evaluated_user_count")
    private Integer evaluatedUserCount;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getProfessionalism() {
        return professionalism;
    }

    public void setProfessionalism(Integer professionalism) {
        this.professionalism = professionalism;
    }

    public Integer getClarity() {
        return clarity;
    }

    public void setClarity(Integer clarity) {
        this.clarity = clarity;
    }

    public Integer getLogic() {
        return logic;
    }

    public void setLogic(Integer logic) {
        this.logic = logic;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Integer getCredibility() {
        return credibility;
    }

    public void setCredibility(Integer credibility) {
        this.credibility = credibility;
    }

    public Integer getPotential() {
        return potential;
    }

    public void setPotential(Integer potential) {
        this.potential = potential;
    }

    public Integer getSkepticism() {
        return skepticism;
    }

    public void setSkepticism(Integer skepticism) {
        this.skepticism = skepticism;
    }

    public Integer getConfusion() {
        return confusion;
    }

    public void setConfusion(Integer confusion) {
        this.confusion = confusion;
    }

    public Integer getImpatience() {
        return impatience;
    }

    public void setImpatience(Integer impatience) {
        this.impatience = impatience;
    }

    public Integer getDisappointment() {
        return disappointment;
    }

    public void setDisappointment(Integer disappointment) {
        this.disappointment = disappointment;
    }

    public Integer getWariness() {
        return wariness;
    }

    public void setWariness(Integer wariness) {
        this.wariness = wariness;
    }

    public Integer getReservation() {
        return reservation;
    }

    public void setReservation(Integer reservation) {
        this.reservation = reservation;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getEvaluatedUserCount() {
        return evaluatedUserCount;
    }

    public void setEvaluatedUserCount(Integer evaluatedUserCount) {
        this.evaluatedUserCount = evaluatedUserCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
