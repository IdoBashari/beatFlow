package com.example.beatflow.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.bumptech.glide.Glide;
import com.example.beatflow.MainActivity;
import com.example.beatflow.PlaylistAdapter;
import com.example.beatflow.R;
import com.example.beatflow.Data.Playlist;
import com.example.beatflow.Data.User;
import com.example.beatflow.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.UUID;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private PlaylistAdapter playlistAdapter;
    private User currentUser;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::uploadProfileImage
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initFirebase();
        setupUserInfo();
        setupButtons();
        setupPlaylistRecyclerView();
        loadPlaylists();
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    private void setupUserInfo() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            firestore.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            binding.userName.setText(currentUser.getName());
                            binding.userDescription.setText(currentUser.getDescription());
                            if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
                                Glide.with(this)
                                        .load(currentUser.getProfileImageUrl())
                                        .placeholder(R.drawable.ic_person)
                                        .into(binding.profileImage);
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ProfileFragment", "Error loading user data", e);
                    });
        }
    }

    private void setupButtons() {
        binding.changeProfileImageButton.setOnClickListener(v -> openImageChooser());
        binding.createPlaylistButton.setOnClickListener(v -> showCreatePlaylistDialog());
        binding.logoutButton.setOnClickListener(v -> logout());
        binding.fab.setOnClickListener(v -> showEditProfileDialog());
    }

    private void openImageChooser() {
        imagePickerLauncher.launch("image/*");
    }

    private void uploadProfileImage(Uri imageUri) {
        if (imageUri != null && firebaseAuth.getCurrentUser() != null) {
            String userId = firebaseAuth.getCurrentUser().getUid();
            StorageReference imageRef = storageRef.child("profile_images/" + userId + ".jpg");

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> updateUserProfileImage(uri.toString())))
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), getString(R.string.error_uploading_image) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ProfileFragment", "Error uploading image", e);
                    });
        }
    }

    private void updateUserProfileImage(String imageUrl) {
        if (firebaseAuth.getCurrentUser() != null) {
            String userId = firebaseAuth.getCurrentUser().getUid();
            firestore.collection("users").document(userId)
                    .update("profileImageUrl", imageUrl)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), getString(R.string.profile_image_updated), Toast.LENGTH_SHORT).show();
                        Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_person)
                                .into(binding.profileImage);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), getString(R.string.error_updating_profile_image) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ProfileFragment", "Error updating profile image", e);
                    });
        }
    }

    private void setupPlaylistRecyclerView() {
        playlistAdapter = new PlaylistAdapter(new ArrayList<>(), this::onPlaylistClick);
        binding.playlistsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.playlistsRecyclerView.setAdapter(playlistAdapter);
    }

    private void loadPlaylists() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();
        firestore.collection("users").document(userId)
                .collection("playlists")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<Playlist> playlists = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Playlist playlist = document.toObject(Playlist.class);
                        if (playlist != null) {
                            playlists.add(playlist);
                        }
                    }
                    playlistAdapter.setPlaylists(playlists);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), getString(R.string.error_loading_playlists) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ProfileFragment", "Error loading playlists", e);
                });
    }

    private void onPlaylistClick(Playlist playlist) {
        if (getActivity() instanceof MainActivity) {
            PlaylistDetailFragment playlistDetailFragment = PlaylistDetailFragment.newInstance(playlist.getId());
            ((MainActivity) getActivity()).loadFragment(playlistDetailFragment);
        }
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.create_new_playlist);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);
        final EditText nameInput = dialogView.findViewById(R.id.playlist_name_input);
        final EditText descriptionInput = dialogView.findViewById(R.id.playlist_description_input);

        builder.setView(dialogView);

        builder.setPositiveButton(R.string.create, (dialog, which) -> {
            String playlistName = nameInput.getText().toString();
            String playlistDescription = descriptionInput.getText().toString();
            if (!playlistName.isEmpty()) {
                createPlaylist(playlistName, playlistDescription);
            } else {
                Toast.makeText(getContext(), "Playlist name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createPlaylist(String playlistName, String playlistDescription) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();
        String playlistId = UUID.randomUUID().toString();

        Playlist newPlaylist = new Playlist(playlistId, playlistName, "", playlistDescription);

        firestore.collection("users").document(userId)
                .collection("playlists").document(playlistId)
                .set(newPlaylist)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), getString(R.string.playlist_created, playlistName), Toast.LENGTH_SHORT).show();
                    loadPlaylists();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), getString(R.string.error_creating_playlist) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ProfileFragment", "Error creating playlist", e);
                });
    }

    private void logout() {
        firebaseAuth.signOut();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new LoginFragment());
        }
    }

    private void showEditProfileDialog() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User data not loaded yet. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.edit_profile);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        final EditText nameInput = dialogView.findViewById(R.id.profile_name_input);
        final EditText descriptionInput = dialogView.findViewById(R.id.profile_description_input);

        nameInput.setText(currentUser.getName());
        descriptionInput.setText(currentUser.getDescription());

        builder.setView(dialogView);

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String newName = nameInput.getText().toString();
            String newDescription = descriptionInput.getText().toString();
            updateUserProfile(newName, newDescription);
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateUserProfile(String newName, String newDescription) {
        if (firebaseAuth.getCurrentUser() != null) {
            String userId = firebaseAuth.getCurrentUser().getUid();
            firestore.collection("users").document(userId)
                    .update("name", newName, "description", newDescription)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
                        currentUser.setName(newName);
                        currentUser.setDescription(newDescription);
                        binding.userName.setText(newName);
                        binding.userDescription.setText(newDescription);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), getString(R.string.error_updating_profile) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ProfileFragment", "Error updating profile", e);
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}