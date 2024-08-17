package com.example.beatflow.Data;


import java.util.HashMap;

import java.util.Map;

public class Playlist {
    private String id;
    private String name;
    private String description;
    private int songCount;
    private String imageUrl;
    private Map<String, Song> songs;
    private String creatorId;
    private String nameLowerCase;

    public Playlist() {

    }

    public Playlist(String id, String name, String description, String imageUrl, String creatorId) {
        this.id = id;
        setName(name);
        this.description = description;
        this.songCount = 0;
        this.imageUrl = imageUrl;
        this.songs = new HashMap<>();
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
        this.nameLowerCase = name.toLowerCase();
    }

    public String getNameLowerCase() {
        return nameLowerCase;
    }

    public void setNameLowerCase(String nameLowerCase) {
        this.nameLowerCase = nameLowerCase;
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

    public Map<String, Song> getSongs() {
        return songs;
    }

    public void setSongs(Map<String, Song> songs) {
        this.songs = songs;
        this.songCount = songs != null ? songs.size() : 0;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public void addSong(Song song) {
        if (this.songs == null) {
            this.songs = new HashMap<>();
        }
        this.songs.put(song.getId(), song);
        this.songCount = this.songs.size();
    }

    public void removeSong(String songId) {
        if (this.songs != null && this.songs.containsKey(songId)) {
            this.songs.remove(songId);
            this.songCount = this.songs.size();
        }
    }
}