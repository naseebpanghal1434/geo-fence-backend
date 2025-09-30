package com.tse.core_application.dto.attendance;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Phase 6b: Response for POST /api/orgs/{orgId}/attendance/punch
 */
public class PunchResponse {

    @JsonProperty("eventId")
    private Long eventId;

    @JsonProperty("accountId")
    private Long accountId;

    @JsonProperty("eventKind")
    private String eventKind;

    @JsonProperty("eventSource")
    private String eventSource;

    @JsonProperty("tsUtc")
    private String tsUtc; // ISO-8601 timestamp

    @JsonProperty("fenceId")
    private Long fenceId;

    @JsonProperty("underRange")
    private Boolean underRange;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("verdict")
    private String verdict; // "PASS", "WARN", "FAIL"

    @JsonProperty("failReason")
    private String failReason;

    @JsonProperty("flags")
    private Map<String, Object> flags;

    public PunchResponse() {
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getEventKind() {
        return eventKind;
    }

    public void setEventKind(String eventKind) {
        this.eventKind = eventKind;
    }

    public String getEventSource() {
        return eventSource;
    }

    public void setEventSource(String eventSource) {
        this.eventSource = eventSource;
    }

    public String getTsUtc() {
        return tsUtc;
    }

    public void setTsUtc(String tsUtc) {
        this.tsUtc = tsUtc;
    }

    public Long getFenceId() {
        return fenceId;
    }

    public void setFenceId(Long fenceId) {
        this.fenceId = fenceId;
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

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
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
}
