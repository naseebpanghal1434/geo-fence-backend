package com.tse.core_application.dto.assignment;

public class EntityActionItem {
    private Integer entityTypeId;
    private Long entityId;
    private Boolean makeDefault;

    public EntityActionItem() {
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

    public Boolean getMakeDefault() {
        return makeDefault;
    }

    public void setMakeDefault(Boolean makeDefault) {
        this.makeDefault = makeDefault;
    }
}
