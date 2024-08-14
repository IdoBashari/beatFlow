package com.example.beatflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

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

public class PlaylistDetailFragment extends Fragment {
    private FragmentPlaylistDetailBinding binding;
    private DatabaseReference databaseReference;
    private String playlistId;
    private Playlist playlist;
    private SongAdapter songAdapter;

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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        loadPlaylistData();
        setupAddSongButton();
    }

    private void setupRecyclerView() {
        songAdapter = new SongAdapter(new ArrayList<>());
        binding.songsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.songsRecyclerView.setAdapter(songAdapter);
    }

    private void setupAddSongButton() {
        binding.addSongButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Add song functionality to be implemented", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadPlaylistData() {
        databaseReference.child("playlists").child(playlistId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String description = dataSnapshot.child("description").getValue(String.class);

                    List<Song> songs = new ArrayList<>();
                    DataSnapshot songsSnapshot = dataSnapshot.child("songs");
                    if (songsSnapshot.exists()) {
                        for (DataSnapshot songSnapshot : songsSnapshot.getChildren()) {
                            String songId = songSnapshot.getKey();
                            String songName = songSnapshot.child("name").getValue(String.class);
                            String artistName = songSnapshot.child("artist").getValue(String.class);

                            Song song = new Song(songId, songName, artistName);
                            songs.add(song);
                        }
                    }

                    playlist = new Playlist(playlistId, name, description, songs.size(), null, songs);
                    updateUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load playlist: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (playlist != null) {
            binding.playlistName.setText(playlist.getName());
            binding.playlistDescription.setText(playlist.getDescription());
            songAdapter.setSongs(playlist.getSongs());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}