package com.example.beatflow.Data;

public class Playlist {
    private String id;
    private String name;
    private String description;
    private int songCount;
    private String imageUrl;

    public Playlist() {
    }

    public Playlist(String id, String name, String description, int songCount, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.songCount = songCount;
        this.imageUrl = imageUrl;
    }

    public Playlist(String id, String name, String description, int songCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.songCount = songCount;
        this.imageUrl = null;
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
}
