package com.example.beatflow.Data;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String id;
    private String name;
    private String imageUrl;
    private String description;
    private List<String> songIds;

    public Playlist() {
        // Empty constructor required for Firestore
    }

    public Playlist(String id, String name, String imageUrl, String description) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.description = description;
        this.songIds = new ArrayList<>();
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSongIds() {
        return songIds;
    }

    public void setSongIds(List<String> songIds) {
        this.songIds = songIds;
    }

    public void addSong(String songId) {
        if (this.songIds == null) {
            this.songIds = new ArrayList<>();
        }
        this.songIds.add(songId);
    }

    public void removeSong(String songId) {
        if (this.songIds != null) {
            this.songIds.remove(songId);
        }
    }

    public int getSongCount() {
        return songIds != null ? songIds.size() : 0;
    }
}