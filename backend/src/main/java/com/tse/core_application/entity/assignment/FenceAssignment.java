package com.tse.core_application.entity.assignment;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "fence_assignment")
public class FenceAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "fence_id", nullable = false)
    private Long fenceId;

    @Column(name = "entity_type_id", nullable = false)
    private Integer entityTypeId;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_datetime", nullable = false)
    private OffsetDateTime createdDatetime;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_datetime")
    private OffsetDateTime updatedDatetime;

    @PrePersist
    protected void onCreate() {
        createdDatetime = OffsetDateTime.now();
        updatedDatetime = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDatetime = OffsetDateTime.now();
    }

    // Constructors
    public FenceAssignment() {
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

    public Long getFenceId() {
        return fenceId;
    }

    public void setFenceId(Long fenceId) {
        this.fenceId = fenceId;
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

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(OffsetDateTime createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getUpdatedDatetime() {
        return updatedDatetime;
    }

    public void setUpdatedDatetime(OffsetDateTime updatedDatetime) {
        this.updatedDatetime = updatedDatetime;
    }
}
