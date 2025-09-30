package com.tse.core_application.dto.assignment;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignFenceResult {
    private Long fenceId;
    private AssignmentSummary summary;
    private List<EntityResult> results;
    private OffsetDateTime updatedAt;
    private Long updatedBy;

    public AssignFenceResult() {
    }

    // Getters and Setters
    public Long getFenceId() {
        return fenceId;
    }

    public void setFenceId(Long fenceId) {
        this.fenceId = fenceId;
    }

    public AssignmentSummary getSummary() {
        return summary;
    }

    public void setSummary(AssignmentSummary summary) {
        this.summary = summary;
    }

    public List<EntityResult> getResults() {
        return results;
    }

    public void setResults(List<EntityResult> results) {
        this.results = results;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
