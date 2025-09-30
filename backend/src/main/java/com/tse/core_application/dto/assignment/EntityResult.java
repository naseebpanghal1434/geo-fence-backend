package com.tse.core_application.dto.assignment;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityResult {
    private Integer entityTypeId;
    private Long entityId;
    private String action;
    private Boolean defaultForEntity;
    private List<Long> fenceIds;
    private String message;

    public EntityResult() {
    }

    public EntityResult(Integer entityTypeId, Long entityId, String action, Boolean defaultForEntity, List<Long> fenceIds, String message) {
        this.entityTypeId = entityTypeId;
        this.entityId = entityId;
        this.action = action;
        this.defaultForEntity = defaultForEntity;
        this.fenceIds = fenceIds;
        this.message = message;
    }

    // Getters and Setters
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Boolean getDefaultForEntity() {
        return defaultForEntity;
    }

    public void setDefaultForEntity(Boolean defaultForEntity) {
        this.defaultForEntity = defaultForEntity;
    }

    public List<Long> getFenceIds() {
        return fenceIds;
    }

    public void setFenceIds(List<Long> fenceIds) {
        this.fenceIds = fenceIds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
