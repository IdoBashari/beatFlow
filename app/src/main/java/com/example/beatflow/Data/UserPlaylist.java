package com.example.beatflow.Data;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class UserPlaylist {
    private String userId;
    private String playlistId;
    private long timestamp;


    public UserPlaylist() {
    }

    public UserPlaylist(String userId, String playlistId, long timestamp) {
        this.userId = userId;
        this.playlistId = playlistId;
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("playlistId", playlistId);
        result.put("timestamp", timestamp);
        return result;
    }
}