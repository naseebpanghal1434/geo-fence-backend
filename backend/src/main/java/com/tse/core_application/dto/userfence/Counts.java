package com.tse.core_application.dto.userfence;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Aggregated counts of fence sources by entity type.
 */
public class Counts {

    @JsonProperty("total")
    private int total;

    @JsonProperty("user")
    private int user;

    @JsonProperty("team")
    private int team;

    @JsonProperty("project")
    private int project;

    @JsonProperty("org")
    private int org;

    public Counts() {
    }

    public Counts(int total, int user, int team, int project, int org) {
        this.total = total;
        this.user = user;
        this.team = team;
        this.project = project;
        this.org = org;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getUser() {
        return user;
    }

    public void setUser(int user) {
        this.user = user;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public int getProject() {
        return project;
    }

    public void setProject(int project) {
        this.project = project;
    }

    public int getOrg() {
        return org;
    }

    public void setOrg(int org) {
        this.org = org;
    }
}
