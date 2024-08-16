package com.example.beatflow.Data;

import java.util.List;

public class Playlist {
    private String id;
    private String name;
    private String description;
    private int songCount;
    private String imageUrl;
    private List<Song> songs;
    private String creatorId;  // New field

    public Playlist() {
        // Empty constructor required for Firebase
    }

    public Playlist(String id, String name, String description, int songCount, String imageUrl, List<Song> songs, String creatorId) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Playlist ID cannot be null or empty");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.songCount = songCount;
        this.imageUrl = imageUrl;
        this.songs = songs;
        this.creatorId = creatorId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }
}