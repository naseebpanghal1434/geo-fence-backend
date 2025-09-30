package com.tse.core_application.dto.assignment;

import javax.validation.constraints.NotNull;
import java.util.List;

public class AssignFenceRequest {

    @NotNull
    private Long fenceId;

    private List<EntityActionItem> add;

    private List<EntityActionItem> remove;

    private Long updatedBy;

    public AssignFenceRequest() {
    }

    // Getters and Setters
    public Long getFenceId() {
        return fenceId;
    }

    public void setFenceId(Long fenceId) {
        this.fenceId = fenceId;
    }

    public List<EntityActionItem> getAdd() {
        return add;
    }

    public void setAdd(List<EntityActionItem> add) {
        this.add = add;
    }

    public List<EntityActionItem> getRemove() {
        return remove;
    }

    public void setRemove(List<EntityActionItem> remove) {
        this.remove = remove;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
