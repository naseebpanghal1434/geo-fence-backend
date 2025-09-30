package com.tse.core_application.entity.attendance;

import com.tse.core_application.constants.attendance.AttendanceStatus;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Daily attendance rollup for an account.
 * Phase 6a: Basic entity structure.
 */
@Entity
@Table(name = "attendance_day")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class AttendanceDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "date_key", nullable = false)
    private LocalDate dateKey;

    @Column(name = "first_in_utc")
    private OffsetDateTime firstInUtc;

    @Column(name = "last_out_utc")
    private OffsetDateTime lastOutUtc;

    @Column(name = "worked_seconds", nullable = false)
    private Integer workedSeconds = 0;

    @Column(name = "break_seconds", nullable = false)
    private Integer breakSeconds = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Type(type = "jsonb")
    @Column(name = "anomalies", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> anomalies = new HashMap<>();

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public LocalDate getDateKey() {
        return dateKey;
    }

    public void setDateKey(LocalDate dateKey) {
        this.dateKey = dateKey;
    }

    public OffsetDateTime getFirstInUtc() {
        return firstInUtc;
    }

    public void setFirstInUtc(OffsetDateTime firstInUtc) {
        this.firstInUtc = firstInUtc;
    }

    public OffsetDateTime getLastOutUtc() {
        return lastOutUtc;
    }

    public void setLastOutUtc(OffsetDateTime lastOutUtc) {
        this.lastOutUtc = lastOutUtc;
    }

    public Integer getWorkedSeconds() {
        return workedSeconds;
    }

    public void setWorkedSeconds(Integer workedSeconds) {
        this.workedSeconds = workedSeconds;
    }

    public Integer getBreakSeconds() {
        return breakSeconds;
    }

    public void setBreakSeconds(Integer breakSeconds) {
        this.breakSeconds = breakSeconds;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public Map<String, Object> getAnomalies() {
        return anomalies;
    }

    public void setAnomalies(Map<String, Object> anomalies) {
        this.anomalies = anomalies;
    }
}
