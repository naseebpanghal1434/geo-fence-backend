package com.tse.core_application.dto.assignment;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignedEntitiesResponse {
    private Long fenceId;
    private EntityLists assigned;
    private EntityLists unassigned;
    private EntityCounts count;

    public AssignedEntitiesResponse() {
    }

    // Getters and Setters
    public Long getFenceId() {
        return fenceId;
    }

    public void setFenceId(Long fenceId) {
        this.fenceId = fenceId;
    }

    public EntityLists getAssigned() {
        return assigned;
    }

    public void setAssigned(EntityLists assigned) {
        this.assigned = assigned;
    }

    public EntityLists getUnassigned() {
        return unassigned;
    }

    public void setUnassigned(EntityLists unassigned) {
        this.unassigned = unassigned;
    }

    public EntityCounts getCount() {
        return count;
    }

    public void setCount(EntityCounts count) {
        this.count = count;
    }
}
