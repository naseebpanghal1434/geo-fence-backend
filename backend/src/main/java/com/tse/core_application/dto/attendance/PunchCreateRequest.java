package com.tse.core_application.dto.attendance;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Phase 6b: Request body for POST /api/orgs/{orgId}/attendance/punch
 */
public class PunchCreateRequest {

    @JsonProperty("accountId")
    private Long accountId;

    @JsonProperty("eventKind")
    private String eventKind; // "CHECK_IN", "CHECK_OUT", "BREAK_START", "BREAK_END"

    @JsonProperty("lat")
    private Double lat;

    @JsonProperty("lon")
    private Double lon;

    @JsonProperty("accuracyM")
    private Double accuracyM;

    @JsonProperty("clientLocalTs")
    private String clientLocalTs; // ISO-8601 timestamp

    @JsonProperty("clientTz")
    private String clientTz; // e.g., "America/New_York"

    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    public PunchCreateRequest() {
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

    public String getClientLocalTs() {
        return clientLocalTs;
    }

    public void setClientLocalTs(String clientLocalTs) {
        this.clientLocalTs = clientLocalTs;
    }

    public String getClientTz() {
        return clientTz;
    }

    public void setClientTz(String clientTz) {
        this.clientTz = clientTz;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
