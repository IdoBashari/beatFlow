package com.example.beatflow.Data;

public class User {
    private String id;
    private String name;
    private String email;
    private String description;
    private String profileImageUrl;

    public User() {}

    public User(String id, String name, String email, String description, String profileImageUrl) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}