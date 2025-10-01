package com.tse.core_application.DummyClasses;

public class User {
    private Long userId;
    private String primaryEmail;
    private String username;

    public User() {
    }

    public User(Long userId, String primaryEmail, String username) {
        this.userId = userId;
        this.primaryEmail = primaryEmail;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPrimaryEmail() {
        return primaryEmail;
    }

    public void setPrimaryEmail(String primaryEmail) {
        this.primaryEmail = primaryEmail;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
