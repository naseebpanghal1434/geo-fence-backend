package com.tse.core_application.dto.assignment;

import java.util.ArrayList;
import java.util.List;

public class EntityLists {
    private List<AssignedEntity> users = new ArrayList<>();
    private List<AssignedEntity> teams = new ArrayList<>();
    private List<AssignedEntity> projects = new ArrayList<>();
    private List<AssignedEntity> orgs = new ArrayList<>();

    public EntityLists() {
    }

    // Getters and Setters
    public List<AssignedEntity> getUsers() {
        return users;
    }

    public void setUsers(List<AssignedEntity> users) {
        this.users = users;
    }

    public List<AssignedEntity> getTeams() {
        return teams;
    }

    public void setTeams(List<AssignedEntity> teams) {
        this.teams = teams;
    }

    public List<AssignedEntity> getProjects() {
        return projects;
    }

    public void setProjects(List<AssignedEntity> projects) {
        this.projects = projects;
    }

    public List<AssignedEntity> getOrgs() {
        return orgs;
    }

    public void setOrgs(List<AssignedEntity> orgs) {
        this.orgs = orgs;
    }
}
