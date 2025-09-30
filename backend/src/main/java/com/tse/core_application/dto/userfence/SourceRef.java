package com.tse.core_application.dto.userfence;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the source of a fence assignment (entity that contributed the fence).
 */
public class SourceRef {

    @JsonProperty("entityTypeId")
    private Integer entityTypeId;

    @JsonProperty("entityId")
    private Long entityId;

    @JsonProperty("defaultForEntity")
    private Boolean defaultForEntity;

    public SourceRef() {
    }

    public SourceRef(Integer entityTypeId, Long entityId, Boolean defaultForEntity) {
        this.entityTypeId = entityTypeId;
        this.entityId = entityId;
        this.defaultForEntity = defaultForEntity;
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

    public Boolean getDefaultForEntity() {
        return defaultForEntity;
    }

    public void setDefaultForEntity(Boolean defaultForEntity) {
        this.defaultForEntity = defaultForEntity;
    }
}
