package com.example.beatflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.beatflow.R;
import com.example.beatflow.databinding.FragmentPlaylistDetailBinding;
import com.example.beatflow.Data.Playlist;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

public class PlaylistDetailFragment extends Fragment {
    private static final String ARG_PLAYLIST_ID = "playlist_id";
    private FragmentPlaylistDetailBinding binding;
    private String playlistId;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

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
        }
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadPlaylistDetails();
    }

    private void loadPlaylistDetails() {
        if (firebaseAuth.getCurrentUser() != null && playlistId != null) {
            String userId = firebaseAuth.getCurrentUser().getUid();
            firestore.collection("users").document(userId)
                    .collection("playlists").document(playlistId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Playlist playlist = documentSnapshot.toObject(Playlist.class);
                        if (playlist != null) {
                            updateUI(playlist);
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error loading playlist", Toast.LENGTH_SHORT).show());
        }
    }

    private void updateUI(Playlist playlist) {
        binding.playlistName.setText(playlist.getName());
        binding.playlistDescription.setText(playlist.getDescription());
        // TODO: Load and display playlist image
        // TODO: Load and display songs in the playlist
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}