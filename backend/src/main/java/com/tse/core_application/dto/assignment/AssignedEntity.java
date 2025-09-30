package com.tse.core_application.dto.assignment;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignedEntity {
    private Long entityId;
    private String name;
    private Boolean defaultForEntity;
    private List<Long> fenceIds;

    public AssignedEntity() {
    }

    public AssignedEntity(Long entityId, String name, Boolean defaultForEntity, List<Long> fenceIds) {
        this.entityId = entityId;
        this.name = name;
        this.defaultForEntity = defaultForEntity;
        this.fenceIds = fenceIds;
    }

    // Getters and Setters
    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
