package com.tse.core_application.dto.assignment;

public class AssignmentSummary {
    private int added = 0;
    private int removed = 0;
    private int updatedDefault = 0;
    private int noops = 0;
    private int errors = 0;

    public AssignmentSummary() {
    }

    public void incrementAdded() {
        added++;
    }

    public void incrementRemoved() {
        removed++;
    }

    public void incrementUpdatedDefault() {
        updatedDefault++;
    }

    public void incrementNoops() {
        noops++;
    }

    public void incrementErrors() {
        errors++;
    }

    // Getters and Setters
    public int getAdded() {
        return added;
    }

    public void setAdded(int added) {
        this.added = added;
    }

    public int getRemoved() {
        return removed;
    }

    public void setRemoved(int removed) {
        this.removed = removed;
    }

    public int getUpdatedDefault() {
        return updatedDefault;
    }

    public void setUpdatedDefault(int updatedDefault) {
        this.updatedDefault = updatedDefault;
    }

    public int getNoops() {
        return noops;
    }

    public void setNoops(int noops) {
        this.noops = noops;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }
}
