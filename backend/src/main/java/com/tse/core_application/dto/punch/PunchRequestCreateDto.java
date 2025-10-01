package com.tse.core_application.dto.punch;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO for creating a new punch request.
 */
public class PunchRequestCreateDto {

    @NotNull(message = "entityTypeId is required")
    @JsonProperty("entityTypeId")
    private Integer entityTypeId; // 1=USER, 2=ORG, 4=PROJECT, 5=TEAM

    @NotNull(message = "entityId is required")
    @JsonProperty("entityId")
    private Long entityId;

    @NotNull(message = "requesterAccountId is required")
    @JsonProperty("requesterAccountId")
    private Long requesterAccountId;

    @NotNull(message = "requestedDateTime is required")
    @JsonProperty("requestedDateTime")
    private LocalDateTime requestedDateTime;

    @NotNull(message = "respondWithinMinutes is required")
    @Min(value = 1, message = "respondWithinMinutes must be at least 1")
    @JsonProperty("respondWithinMinutes")
    private Integer respondWithinMinutes;

    public PunchRequestCreateDto() {
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

    public LocalDateTime getRequestedDateTime() {
        return requestedDateTime;
    }

    public void setRequestedDateTime(LocalDateTime requestedDateTime) {
        this.requestedDateTime = requestedDateTime;
    }

    public Integer getRespondWithinMinutes() {
        return respondWithinMinutes;
    }

    public void setRespondWithinMinutes(Integer respondWithinMinutes) {
        this.respondWithinMinutes = respondWithinMinutes;
    }
}
