package com.tse.core_application.dto.assignment;

public class EntityCounts {
    private int users = 0;
    private int teams = 0;
    private int projects = 0;
    private int orgs = 0;

    public EntityCounts() {
    }

    public EntityCounts(int users, int teams, int projects, int orgs) {
        this.users = users;
        this.teams = teams;
        this.projects = projects;
        this.orgs = orgs;
    }

    // Getters and Setters
    public int getUsers() {
        return users;
    }

    public void setUsers(int users) {
        this.users = users;
    }

    public int getTeams() {
        return teams;
    }

    public void setTeams(int teams) {
        this.teams = teams;
    }

    public int getProjects() {
        return projects;
    }

    public void setProjects(int projects) {
        this.projects = projects;
    }

    public int getOrgs() {
        return orgs;
    }

    public void setOrgs(int orgs) {
        this.orgs = orgs;
    }
}
