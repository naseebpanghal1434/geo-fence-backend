package com.tse.core_application.dto.punch;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for viewing a punch request with computed fields.
 */
public class PunchRequestViewDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("orgId")
    private Long orgId;

    @JsonProperty("entityTypeId")
    private Integer entityTypeId;

    @JsonProperty("entityId")
    private Long entityId;

    @JsonProperty("requesterAccountId")
    private Long requesterAccountId;

    @JsonProperty("requestedDateTime")
    private OffsetDateTime requestedDateTime;

    @JsonProperty("respondWithinMinutes")
    private Integer respondWithinMinutes;

    @JsonProperty("expiresAt")
    private OffsetDateTime expiresAt;

    @JsonProperty("state")
    private String state; // PENDING/FULFILLED/EXPIRED/CANCELLED

    @JsonProperty("activeNow")
    private Boolean activeNow; // computed: now in [requestedDateTime, expiresAt)

    @JsonProperty("secondsRemaining")
    private Long secondsRemaining; // if activeNow, else 0

    @JsonProperty("appliesToAccountIds")
    private List<Long> appliesToAccountIds = new ArrayList<>(); // optional: which accounts this applies to

    public PunchRequestViewDto() {
    }

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

    public Integer getEntityTypeId() {
        return entityTypeId;
    }

    public void setEntityTypeId(Integer entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Long getRequesterAccountId() {
        return requesterAccountId;
    }

    public void setRequesterAccountId(Long requesterAccountId) {
        this.requesterAccountId = requesterAccountId;
    }

    public OffsetDateTime getRequestedDateTime() {
        return requestedDateTime;
    }

    public void setRequestedDateTime(OffsetDateTime requestedDateTime) {
        this.requestedDateTime = requestedDateTime;
    }

    public Integer getRespondWithinMinutes() {
        return respondWithinMinutes;
    }

    public void setRespondWithinMinutes(Integer respondWithinMinutes) {
        this.respondWithinMinutes = respondWithinMinutes;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Boolean getActiveNow() {
        return activeNow;
    }

    public void setActiveNow(Boolean activeNow) {
        this.activeNow = activeNow;
    }

    public Long getSecondsRemaining() {
        return secondsRemaining;
    }

    public void setSecondsRemaining(Long secondsRemaining) {
        this.secondsRemaining = secondsRemaining;
    }

    public List<Long> getAppliesToAccountIds() {
        return appliesToAccountIds;
    }

    public void setAppliesToAccountIds(List<Long> appliesToAccountIds) {
        this.appliesToAccountIds = appliesToAccountIds;
    }
}
