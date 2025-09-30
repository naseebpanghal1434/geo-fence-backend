package com.tse.core_application.entity.punch;

import javax.persistence.*;
import java.time.OffsetDateTime;

/**
 * Represents a punch request created by a manager/admin targeting a user, team, project, or org.
 * The request is "pending" while now is within the active window [requestedDateTime, expiresAt).
 */
@Entity
@Table(name = "punch_request")
public class PunchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "entity_type_id", nullable = false)
    private Integer entityTypeId; // 1=USER, 2=ORG, 4=PROJECT, 5=TEAM

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "requester_account_id", nullable = false)
    private Long requesterAccountId;

    @Column(name = "requested_datetime", nullable = false)
    private OffsetDateTime requestedDatetime;

    @Column(name = "respond_within_minutes", nullable = false)
    private Integer respondWithinMinutes;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private State state = State.PENDING;

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private OffsetDateTime createdDatetime;

    @Column(name = "updated_datetime")
    private OffsetDateTime updatedDatetime;

    @PrePersist
    protected void onCreate() {
        createdDatetime = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDatetime = OffsetDateTime.now();
    }

    public enum State {
        PENDING,
        FULFILLED,
        EXPIRED,
        CANCELLED
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

    public OffsetDateTime getRequestedDatetime() {
        return requestedDatetime;
    }

    public void setRequestedDatetime(OffsetDateTime requestedDatetime) {
        this.requestedDatetime = requestedDatetime;
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

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public OffsetDateTime getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(OffsetDateTime createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    public OffsetDateTime getUpdatedDatetime() {
        return updatedDatetime;
    }

    public void setUpdatedDatetime(OffsetDateTime updatedDatetime) {
        this.updatedDatetime = updatedDatetime;
    }
}
