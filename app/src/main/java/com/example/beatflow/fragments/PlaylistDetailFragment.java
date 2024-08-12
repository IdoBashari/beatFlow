package com.example.beatflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.beatflow.R;
import com.example.beatflow.SongAdapter;
import com.example.beatflow.Data.Playlist;
import com.example.beatflow.Data.Song;
import com.example.beatflow.databinding.FragmentPlaylistDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.util.Log;

public class PlaylistDetailFragment extends Fragment {
    private static final String ARG_PLAYLIST_ID = "playlist_id";
    private FragmentPlaylistDetailBinding binding;
    private String playlistId;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private SongAdapter songAdapter;

    public static PlaylistDetailFragment newInstance(String playlistId) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLAYLIST_ID, playlistId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playlistId = getArguments().getString(ARG_PLAYLIST_ID);
            Log.d("PlaylistDetailFragment", "playlistId: " + playlistId);
        }
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupSongRecyclerView();
        loadPlaylistDetails();
        binding.addSongButton.setOnClickListener(v -> showAddSongDialog());
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("PlaylistDetailFragment", "Fragment started");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("PlaylistDetailFragment", "Fragment resumed");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("PlaylistDetailFragment", "Fragment paused");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("PlaylistDetailFragment", "Fragment stopped");
    }

    private void setupSongRecyclerView() {
        songAdapter = new SongAdapter(new ArrayList<>());
        binding.songsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.songsRecyclerView.setAdapter(songAdapter);
    }

    private void showAddSongDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_song, null);
        EditText songNameInput = dialogView.findViewById(R.id.song_name_input);
        EditText artistNameInput = dialogView.findViewById(R.id.artist_name_input);

        builder.setView(dialogView)
                .setPositiveButton("Add", (dialog, id) -> {
                    String songName = songNameInput.getText().toString();
                    String artistName = artistNameInput.getText().toString();
                    addSongToPlaylist(songName, artistName);
                })
                .setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void addSongToPlaylist(String songName, String artistName) {
        String songId = UUID.randomUUID().toString();
        Song newSong = new Song(songId, songName, artistName);

        if (firebaseAuth.getCurrentUser() != null) {
            if (playlistId != null && songId != null) {
                DatabaseReference playlistRef = databaseReference.child("users")
                        .child(firebaseAuth.getCurrentUser().getUid())
                        .child("playlists")
                        .child(playlistId);

                playlistRef.child("songs").child(songId).setValue(newSong)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(requireContext(), "Song added successfully", Toast.LENGTH_SHORT).show();
                            Log.d("PlaylistDetailFragment", "Added song: " + songName + " to playlist: " + playlistId);
                            loadPlaylistDetails();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("PlaylistDetailFragment", "Failed to add song: " + e.getMessage());
                            Toast.makeText(requireContext(), "Failed to add song", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Log.e("PlaylistDetailFragment", "Invalid playlistId or songId");
                Toast.makeText(requireContext(), "Invalid playlist or song ID", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("PlaylistDetailFragment", "User not logged in");
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPlaylistDetails() {
        if (firebaseAuth.getCurrentUser() != null && playlistId != null) {
            DatabaseReference playlistRef = databaseReference.child("users")
                    .child(firebaseAuth.getCurrentUser().getUid())
                    .child("playlists")
                    .child(playlistId);

            playlistRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Playlist playlist = dataSnapshot.getValue(Playlist.class);
                        if (playlist != null) {
                            updateUI(playlist);
                            loadSongs(dataSnapshot);
                            Log.d("PlaylistDetailFragment", "Loaded playlist: " + playlist.getName());
                        }
                    } else {
                        Log.e("PlaylistDetailFragment", "Playlist not found for ID: " + playlistId);
                        Toast.makeText(getContext(), "Playlist not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("PlaylistDetailFragment", "Error loading playlist: " + databaseError.getMessage());
                    Toast.makeText(getContext(), "Error loading playlist", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateUI(Playlist playlist) {
        binding.playlistName.setText(playlist.getName());
        binding.playlistDescription.setText(playlist.getDescription());
    }

    private void loadSongs(DataSnapshot playlistSnapshot) {
        List<Song> songs = new ArrayList<>();
        DataSnapshot songsSnapshot = playlistSnapshot.child("songs");
        if (songsSnapshot.exists()) {
            for (DataSnapshot songSnapshot : songsSnapshot.getChildren()) {
                Song song = songSnapshot.getValue(Song.class);
                if (song != null) {
                    songs.add(song);
                    Log.d("PlaylistDetailFragment", "Loaded song: " + song.getName());
                }
            }
        } else {
            Log.e("PlaylistDetailFragment", "No songs found for playlist ID: " + playlistId);
            Toast.makeText(getContext(), "No songs found", Toast.LENGTH_SHORT).show();
        }
        songAdapter.setSongs(songs);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        Log.d("PlaylistDetailFragment", "View destroyed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("PlaylistDetailFragment", "Fragment destroyed");
    }
}
