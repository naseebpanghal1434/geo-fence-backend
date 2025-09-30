package com.tse.core_application.entity.attendance;

import com.tse.core_application.constants.attendance.EventAction;
import com.tse.core_application.constants.attendance.EventKind;
import com.tse.core_application.constants.attendance.EventSource;
import com.tse.core_application.constants.attendance.IntegrityVerdict;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a punch event (CHECK_IN, CHECK_OUT, BREAK_START, BREAK_END, PUNCHED).
 * Phase 6a: Basic entity structure for foundational infrastructure.
 */
@Entity
@Table(name = "attendance_event")
public class AttendanceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_kind", nullable = false)
    private EventKind eventKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_source", nullable = false)
    private EventSource eventSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_action", nullable = false)
    private EventAction eventAction;

    @Column(name = "ts_utc", nullable = false)
    private OffsetDateTime tsUtc;

    @Column(name = "client_local_ts")
    private OffsetDateTime clientLocalTs;

    @Column(name = "client_tz")
    private String clientTz;

    @Column(name = "fence_id")
    private Long fenceId;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Column(name = "accuracy_m")
    private Double accuracyM;

    @Column(name = "under_range")
    private Boolean underRange;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false)
    private IntegrityVerdict verdict;

    @Column(name = "fail_reason")
    private String failReason;

    @Type(type = "jsonb")
    @Column(name = "flags", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> flags = new HashMap<>();

    @Column(name = "punch_request_id")
    private Long punchRequestId;

    @Column(name = "requester_account_id")
    private Long requesterAccountId;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private OffsetDateTime createdDatetime;

    @PrePersist
    protected void onCreate() {
        if (createdDatetime == null) {
            createdDatetime = OffsetDateTime.now();
        }
        if (tsUtc == null) {
            tsUtc = OffsetDateTime.now();
        }
    }

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

    public EventKind getEventKind() {
        return eventKind;
    }

    public void setEventKind(EventKind eventKind) {
        this.eventKind = eventKind;
    }

    public EventSource getEventSource() {
        return eventSource;
    }

    public void setEventSource(EventSource eventSource) {
        this.eventSource = eventSource;
    }

    public EventAction getEventAction() {
        return eventAction;
    }

    public void setEventAction(EventAction eventAction) {
        this.eventAction = eventAction;
    }

    public OffsetDateTime getTsUtc() {
        return tsUtc;
    }

    public void setTsUtc(OffsetDateTime tsUtc) {
        this.tsUtc = tsUtc;
    }

    public OffsetDateTime getClientLocalTs() {
        return clientLocalTs;
    }

    public void setClientLocalTs(OffsetDateTime clientLocalTs) {
        this.clientLocalTs = clientLocalTs;
    }

    public String getClientTz() {
        return clientTz;
    }

    public void setClientTz(String clientTz) {
        this.clientTz = clientTz;
    }

    public Long getFenceId() {
        return fenceId;
    }

    public void setFenceId(Long fenceId) {
        this.fenceId = fenceId;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getAccuracyM() {
        return accuracyM;
    }

    public void setAccuracyM(Double accuracyM) {
        this.accuracyM = accuracyM;
    }

    public Boolean getUnderRange() {
        return underRange;
    }

    public void setUnderRange(Boolean underRange) {
        this.underRange = underRange;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public IntegrityVerdict getVerdict() {
        return verdict;
    }

    public void setVerdict(IntegrityVerdict verdict) {
        this.verdict = verdict;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public Map<String, Object> getFlags() {
        return flags;
    }

    public void setFlags(Map<String, Object> flags) {
        this.flags = flags;
    }

    public Long getPunchRequestId() {
        return punchRequestId;
    }

    public void setPunchRequestId(Long punchRequestId) {
        this.punchRequestId = punchRequestId;
    }

    public Long getRequesterAccountId() {
        return requesterAccountId;
    }

    public void setRequesterAccountId(Long requesterAccountId) {
        this.requesterAccountId = requesterAccountId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public OffsetDateTime getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(OffsetDateTime createdDatetime) {
        this.createdDatetime = createdDatetime;
    }
}
