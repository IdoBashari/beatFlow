package com.example.beatflow.fragments;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.beatflow.Data.Playlist;
import com.example.beatflow.Data.Song;
import com.example.beatflow.R;
import com.example.beatflow.SongAdapter;
import com.example.beatflow.databinding.FragmentPlaylistDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistDetailFragment extends Fragment {
    private FragmentPlaylistDetailBinding binding;
    private DatabaseReference databaseReference;
    private String playlistId;
    private Playlist playlist;
    private SongAdapter songAdapter;
    private static RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();

    private FirebaseAuth firebaseAuth;
    private String creatorId;

    private boolean isCreator;

    public static PlaylistDetailFragment newInstance(String creatorId, String playlistId) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putString("creatorId", creatorId);
        args.putString("playlistId", playlistId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            creatorId = getArguments().getString("creatorId");
            playlistId = getArguments().getString("playlistId");
            isCreator = getArguments().getBoolean("isCreator", false);
        }
        databaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false);
        binding.songsRecyclerView.setRecycledViewPool(sharedPool);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        checkNetworkAndLoadPlaylist();
        binding.addSongButton.setEnabled(false);
        setupAddSongButton();

        songAdapter.setOnSongLongClickListener(this::showDeleteSongDialog);
    }

    private void setupRecyclerView() {
        Log.d("PlaylistDetailFragment", "Setting up RecyclerView...");
        songAdapter = new SongAdapter(new ArrayList<>());
        binding.songsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.songsRecyclerView.setAdapter(songAdapter);
        Log.d("PlaylistDetailFragment", "RecyclerView setup completed.");
    }

    private void checkNetworkAndLoadPlaylist() {
        if (isNetworkAvailable()) {
            checkUserAndLoadPlaylist();
        } else {
            Log.e("PlaylistDetailFragment", "No internet connection");
            Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
            showLoading(false);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkUserAndLoadPlaylist() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Log.d("PlaylistDetailFragment", "User is not logged in. Redirecting to login.");
            // Navigate to login screen
            return;
        }
        loadPlaylistData();
    }

    private void loadPlaylistData() {
        showLoading(true);
        Log.d("PlaylistDetailFragment", "Starting to load playlist data for ID: " + playlistId);

        if (playlistId != null) {
            DatabaseReference playlistRef = databaseReference.child("playlists").child(playlistId);
            playlistRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.d("PlaylistDetailFragment", "Data snapshot received. Exists: " + dataSnapshot.exists());
                    if (dataSnapshot.exists()) {
                        playlist = dataSnapshot.getValue(Playlist.class);
                        if (playlist != null) {
                            playlist.setId(playlistId);
                            Log.d("PlaylistDetailFragment", "Playlist loaded successfully: " + playlist.getName());
                            binding.addSongButton.setEnabled(true);
                            updateUI();
                            loadSongs();
                        } else {
                            Log.e("PlaylistDetailFragment", "Failed to parse playlist data");
                            Toast.makeText(getContext(), "Failed to load playlist data", Toast.LENGTH_SHORT).show();
                            requireActivity().onBackPressed();
                        }
                    } else {
                        Log.e("PlaylistDetailFragment", "Playlist not found for ID: " + playlistId);
                        Toast.makeText(getContext(), "Playlist not found", Toast.LENGTH_SHORT).show();
                        requireActivity().onBackPressed();
                    }
                    showLoading(false);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("PlaylistDetailFragment", "Database error: " + databaseError.getMessage(), databaseError.toException());
                    Toast.makeText(getContext(), "Failed to load playlist: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    requireActivity().onBackPressed();
                }
            });
        } else {
            Log.e("PlaylistDetailFragment", "Playlist ID is null");
            Toast.makeText(getContext(), "Invalid playlist ID", Toast.LENGTH_SHORT).show();
            showLoading(false);
            requireActivity().onBackPressed();
        }
    }

    private void loadSongs() {
        if (playlist != null && playlistId != null) {
            DatabaseReference songsRef = databaseReference.child("playlists").child(playlistId).child("songs");
            songsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Map<String, Song> songsMap = new HashMap<>();
                    for (DataSnapshot songSnapshot : dataSnapshot.getChildren()) {
                        Song song = songSnapshot.getValue(Song.class);
                        if (song != null) {
                            song.setId(songSnapshot.getKey());
                            songsMap.put(song.getId(), song);
                        }
                    }
                    playlist.setSongs(songsMap);
                    songAdapter.setSongs(new ArrayList<>(songsMap.values()));
                    songAdapter.notifyDataSetChanged();
                    Log.d("PlaylistDetailFragment", "Songs loaded: " + songsMap.size());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("PlaylistDetailFragment", "Error loading songs: " + databaseError.getMessage());
                    Toast.makeText(getContext(), "Failed to load songs", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void setupAddSongButton() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && user.getUid().equals(creatorId)) {
            binding.addSongButton.setVisibility(View.VISIBLE);
            binding.addSongButton.setOnClickListener(v -> showAddSongDialog());
        } else {
            binding.addSongButton.setVisibility(View.GONE);
        }
    }

    private void showAddSongDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_song, null);
        EditText songNameInput = dialogView.findViewById(R.id.song_name_input);
        EditText artistNameInput = dialogView.findViewById(R.id.artist_name_input);

        builder.setView(dialogView)
                .setPositiveButton("Add", (dialog, id) -> {
                    String songName = songNameInput.getText().toString().trim();
                    String artistName = artistNameInput.getText().toString().trim();
                    addNewSong(songName, artistName);
                })
                .setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void addNewSong(String songName, String artistName) {
        Log.d("AddSong", "Adding new song: " + songName + " by " + artistName);

        if (songName.isEmpty() || artistName.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (playlist == null || playlistId == null) {
            Log.e("AddSong", "Error: Playlist not loaded");
            Toast.makeText(getContext(), "Error: Playlist not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Log.e("AddSong", "Error: User not logged in");
            Toast.makeText(getContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference playlistRef = databaseReference.child("playlists").child(playlistId);
        String songId = playlistRef.child("songs").push().getKey();
        if (songId == null) {
            Log.e("AddSong", "Error: Failed to generate song ID");
            Toast.makeText(getContext(), "Error: Failed to generate song ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Song newSong = new Song(songId, songName, artistName);
        Log.d("AddSong", "Attempting to save song to Firebase: " + newSong.getName());

        Map<String, Object> updates = new HashMap<>();
        updates.put("songs/" + songId, newSong);
        updates.put("songCount", playlist.getSongCount() + 1);

        playlistRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("AddSong", "Song saved successfully in Firebase");
                    if (playlist.getSongs() == null) {
                        playlist.setSongs(new HashMap<>());
                    }
                    playlist.getSongs().put(newSong.getId(), newSong);
                    playlist.setSongCount(playlist.getSongCount() + 1);
                    songAdapter.setSongs(new ArrayList<>(playlist.getSongs().values()));
                    songAdapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Song added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("AddSong", "Failed to save song in Firebase", e);
                    Toast.makeText(getContext(), "Failed to add song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteSongDialog(Song song) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Song")
                .setMessage("Are you sure you want to delete this song?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSongFromPlaylist(song))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSongFromPlaylist(Song song) {
        if (playlist == null || playlistId == null) {
            Toast.makeText(getContext(), "Playlist not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || !user.getUid().equals(creatorId)) {
            Toast.makeText(getContext(), "Only the playlist creator can delete songs", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference songRef = databaseReference
                .child("playlists")
                .child(playlistId)
                .child("songs")
                .child(song.getId());

        songRef.removeValue().addOnSuccessListener(aVoid -> {
            playlist.getSongs().remove(song.getId());
            playlist.setSongCount(playlist.getSongCount() - 1);
            songAdapter.setSongs(new ArrayList<>(playlist.getSongs().values()));
            songAdapter.notifyDataSetChanged();
            updatePlaylistSongCount();
            Toast.makeText(getContext(), "Song deleted", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to delete song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updatePlaylistSongCount() {
        DatabaseReference playlistRef = databaseReference.child("playlists").child(playlistId);
        playlistRef.child("songCount").setValue(playlist.getSongCount());
    }

    private void updateUI() {
        if (playlist != null && binding != null) {
            binding.playlistName.setText(playlist.getName());
            binding.playlistDescription.setText(playlist.getDescription());

            if (playlist.getImageUrl() != null && !playlist.getImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(playlist.getImageUrl())
                        .placeholder(R.drawable.default_playlist_image)
                        .error(R.drawable.default_playlist_image)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.playlistImage);
            } else {
                binding.playlistImage.setImageResource(R.drawable.default_playlist_image);
            }
        }
    }

    private void showLoading(boolean isLoading) {
        binding.loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("playlistId", playlistId);
        if (playlist != null && playlist.getSongs() != null) {
            outState.putSerializable("songs", new HashMap<>(playlist.getSongs()));
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            playlistId = savedInstanceState.getString("playlistId");
            Map<String, Song> songs = (Map<String, Song>) savedInstanceState.getSerializable("songs");
            if (songs != null && playlist != null) {
                playlist.setSongs(songs);
                songAdapter.setSongs(new ArrayList<>(songs.values()));
            }
        }
    }
}