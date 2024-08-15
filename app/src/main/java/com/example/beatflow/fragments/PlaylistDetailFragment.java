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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.beatflow.Data.Playlist;
import com.example.beatflow.Data.Song;
import com.example.beatflow.R;
import com.example.beatflow.SongAdapter;
import com.example.beatflow.databinding.FragmentPlaylistDetailBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class PlaylistDetailFragment extends Fragment {
    private FragmentPlaylistDetailBinding binding;
    private DatabaseReference databaseReference;
    private String playlistId;
    private Playlist playlist;
    private SongAdapter songAdapter;
    private static RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();

    private FirebaseAuth firebaseAuth;

    public static PlaylistDetailFragment newInstance(String playlistId) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putString("playlistId", playlistId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playlistId = getArguments().getString("playlistId");
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

        // הגדרת מאזין ללחיצה ארוכה על שיר
        songAdapter.setOnSongLongClickListener(song -> showDeleteSongDialog(song));
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
        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference songRef = databaseReference
                .child("users")
                .child(user.getUid())
                .child("playlists")
                .child(playlistId)
                .child("songs")
                .child(song.getId());

        songRef.removeValue().addOnSuccessListener(aVoid -> {
            playlist.getSongs().remove(song);
            songAdapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "Song deleted", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to delete song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
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


    private void setupRecyclerView() {
        Log.d("PlaylistDetailFragment", "Setting up RecyclerView...");
        songAdapter = new SongAdapter(new ArrayList<>());
        binding.songsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.songsRecyclerView.setAdapter(songAdapter);
        Log.d("PlaylistDetailFragment", "RecyclerView setup completed.");
    }


    private void setupAddSongButton() {
        binding.addSongButton.setOnClickListener(v -> showAddSongDialog());
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
            Log.e("AddSong", "Error: Playlist not loaded. Playlist or playlistId is null.");
            Toast.makeText(getContext(), "Error: Playlist not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Log.e("AddSong", "Error: User not logged in");
            Toast.makeText(getContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference playlistRef = databaseReference.child("users").child(user.getUid()).child("playlists").child(playlistId);
        String songId = playlistRef.child("songs").push().getKey();
        if (songId == null) {
            Log.e("AddSong", "Error: Failed to generate song ID");
            Toast.makeText(getContext(), "Error: Failed to generate song ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Song newSong = new Song(songId, songName, artistName);
        Log.d("AddSong", "Attempting to save song to Firebase: " + newSong.getName());

        Map<String, Object> updates = new HashMap<>();
        updates.put("songs/" + songId, newSong.toMap());
        updates.put("songCount", playlist.getSongCount() + 1);

        playlistRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("AddSong", "Song saved successfully in Firebase");

                    if (playlist.getSongs() == null) {
                        Log.d("AddSong", "Playlist songs list is null, initializing...");
                        playlist.setSongs(new ArrayList<>());
                    }

                    playlist.getSongs().add(newSong);
                    playlist.setSongCount(playlist.getSongCount() + 1);
                    Log.d("AddSong", "Song added to local playlist");

                    int newSongPosition = playlist.getSongs().size() - 1;
                    songAdapter.notifyItemInserted(newSongPosition);
                    Log.d("AddSong", "UI updated successfully after adding song.");

                    Toast.makeText(getContext(), "Song added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("AddSong", "Failed to save song in Firebase", e);
                    Toast.makeText(getContext(), "Failed to add song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    private void updateSongCountInDatabase(int songCount) {
        if (playlistId != null) {
            databaseReference.child("playlists").child(playlistId).child("songCount").setValue(songCount)
                    .addOnSuccessListener(aVoid -> Log.d("UpdateSongCount", "Song count updated successfully"))
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to update song count", Toast.LENGTH_SHORT).show();
                        Log.e("UpdateSongCount", "Error updating song count", e);
                    });
        }
    }


    private void loadPlaylistData() {
        showLoading(true);
        Log.d("PlaylistDetailFragment", "Starting to load playlist data for ID: " + playlistId);

        if (playlistId != null) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                Log.d("PlaylistDetailFragment", "User is logged in with UID: " + user.getUid());
                String path = "users/" + user.getUid() + "/playlists/" + playlistId;
                Log.d("PlaylistDetailFragment", "Attempting to load from path: " + path);

                databaseReference.child(path).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Log.d("PlaylistDetailFragment", "Data snapshot received. Exists: " + dataSnapshot.exists());
                        if (dataSnapshot.exists()) {
                            String name = dataSnapshot.child("name").getValue(String.class);
                            String description = dataSnapshot.child("description").getValue(String.class);
                            String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
                            Long songCountLong = dataSnapshot.child("songCount").getValue(Long.class);
                            int songCount = songCountLong != null ? songCountLong.intValue() : 0;

                            List<Song> songs = new ArrayList<>();
                            DataSnapshot songsSnapshot = dataSnapshot.child("songs");
                            if (songsSnapshot.exists()) {
                                for (DataSnapshot songSnapshot : songsSnapshot.getChildren()) {
                                    Song song = songSnapshot.getValue(Song.class);
                                    if (song != null) {
                                        songs.add(song);
                                    }
                                }
                            }

                            playlist = new Playlist(playlistId, name, description, songCount, imageUrl, songs);
                            Log.d("PlaylistDetailFragment", "Playlist loaded successfully: " + playlist.getName());
                            Log.d("PlaylistDetailFragment", "Songs loaded: " + songs.size());

                            binding.addSongButton.setEnabled(true);
                            updateUI();
                        } else {
                            Log.d("PlaylistDetailFragment", "Playlist not found for ID: " + playlistId);
                            Toast.makeText(getContext(), "Playlist not found", Toast.LENGTH_SHORT).show();
                        }
                        showLoading(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("PlaylistDetailFragment", "Database error: " + databaseError.getMessage(), databaseError.toException());
                        Toast.makeText(getContext(), "Failed to load playlist: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
            } else {
                Log.e("PlaylistDetailFragment", "User is not logged in");
                Toast.makeText(getContext(), "User is not logged in", Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        } else {
            Log.e("PlaylistDetailFragment", "Playlist ID is null");
            Toast.makeText(getContext(), "Playlist ID is null", Toast.LENGTH_SHORT).show();
            showLoading(false);
        }
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

            songAdapter.setSongs(playlist.getSongs());
            songAdapter.notifyDataSetChanged();
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
        outState.putParcelableArrayList("songs", new ArrayList<>(playlist.getSongs()));
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            playlistId = savedInstanceState.getString("playlistId");
            List<Song> songs = savedInstanceState.getParcelableArrayList("songs");
            if (songs != null) {
                playlist.setSongs(songs);
                songAdapter.setSongs(songs);
            }
        }
    }



}