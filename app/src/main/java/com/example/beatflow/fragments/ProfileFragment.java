package com.example.beatflow.fragments;

import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.UUID;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
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
        initFirebase();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUserInfo();
        setupButtons();
        setupPlaylistRecyclerView();
        loadPlaylists();
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    private void setupUserInfo() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            databaseReference.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    currentUser = dataSnapshot.getValue(User.class);
                    if (currentUser != null) {
                        binding.userName.setText(currentUser.getName());
                        binding.userDescription.setText(currentUser.getDescription());
                        loadProfileImage(currentUser.getProfileImageUrl());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(requireContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.error_profile_image)
                    .into(binding.profileImage);
        } else {
            binding.profileImage.setImageResource(R.drawable.ic_person);
        }
    }

    private void setupButtons() {
        binding.changeProfileImageButton.setOnClickListener(v -> openImageChooser());
        binding.createPlaylistButton.setOnClickListener(v -> showCreatePlaylistDialog());
        binding.logoutButton.setOnClickListener(v -> logout());
        binding.editProfileButton.setOnClickListener(v -> showEditProfileDialog());
    }

    private void openImageChooser() {
        imagePickerLauncher.launch("image/*");
    }

    private void uploadProfileImage(Uri imageUri) {
        if (imageUri != null && firebaseAuth.getCurrentUser() != null) {
            StorageReference fileRef = storageRef.child("users/" + firebaseAuth.getCurrentUser().getUid() + "/profile.jpg");
            fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                databaseReference.child("users").child(firebaseAuth.getCurrentUser().getUid()).child("profileImageUrl").setValue(imageUrl);
                loadProfileImage(imageUrl);
            })).addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to upload image.", Toast.LENGTH_SHORT).show());
        }
    }

    private void setupPlaylistRecyclerView() {
        playlistAdapter = new PlaylistAdapter(new ArrayList<>(), this::handlePlaylistSelection);
        binding.playlistsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.playlistsRecyclerView.setAdapter(playlistAdapter);
        playlistAdapter.setOnPlaylistLongClickListener(this::showDeletePlaylistDialog);
    }

    private void loadPlaylists() {
        binding.playlistsProgressBar.setVisibility(View.VISIBLE);
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            databaseReference.child("users").child(user.getUid()).child("playlists").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    ArrayList<Playlist> playlists = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Playlist playlist = snapshot.getValue(Playlist.class);
                        if (playlist != null) {
                            playlists.add(playlist);
                        }
                    }
                    playlistAdapter.setPlaylists(playlists);
                    binding.playlistsProgressBar.setVisibility(View.GONE);
                    binding.noPlaylistsText.setVisibility(playlists.isEmpty() ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    binding.playlistsProgressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load playlists: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void handlePlaylistSelection(Playlist playlist) {
        PlaylistDetailFragment detailFragment = PlaylistDetailFragment.newInstance(playlist.getId());
        ((MainActivity) requireActivity()).loadFragment(detailFragment);
    }

    private boolean showDeletePlaylistDialog(Playlist playlist) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete this playlist?")
                .setPositiveButton("Yes", (dialog, which) -> deletePlaylist(playlist))
                .setNegativeButton("No", null)
                .show();
        return true;
    }

    private void deletePlaylist(Playlist playlist) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            databaseReference.child("users").child(user.getUid())
                    .child("playlists").child(playlist.getId()).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Playlist deleted successfully", Toast.LENGTH_SHORT).show();
                        loadPlaylists();
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to delete playlist", Toast.LENGTH_SHORT).show());
        }
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);
        EditText nameInput = dialogView.findViewById(R.id.playlist_name_input);
        EditText descriptionInput = dialogView.findViewById(R.id.playlist_description_input);

        builder.setView(dialogView)
                .setPositiveButton("Create", (dialog, id) -> {
                    String name = nameInput.getText().toString();
                    String description = descriptionInput.getText().toString();
                    createPlaylist(name, description);
                })
                .setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void createPlaylist(String name, String description) {
        String playlistId = UUID.randomUUID().toString();
        Playlist newPlaylist = new Playlist(playlistId, name, description, 0);
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            databaseReference.child("users").child(user.getUid())
                    .child("playlists").child(playlistId).setValue(newPlaylist)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Playlist created successfully", Toast.LENGTH_SHORT).show();
                        loadPlaylists();
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to create playlist", Toast.LENGTH_SHORT).show());
        }
    }

    private void logout() {
        firebaseAuth.signOut();
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        EditText nameEdit = dialogView.findViewById(R.id.edit_name);
        EditText descriptionEdit = dialogView.findViewById(R.id.edit_description);

        nameEdit.setText(currentUser.getName());
        descriptionEdit.setText(currentUser.getDescription());

        builder.setView(dialogView)
                .setPositiveButton("Save", (dialog, id) -> {
                    String newName = nameEdit.getText().toString();
                    String newDescription = descriptionEdit.getText().toString();
                    updateUserProfile(newName, newDescription);
                })
                .setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void updateUserProfile(String name, String description) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = databaseReference.child("users").child(user.getUid());
            userRef.child("name").setValue(name);
            userRef.child("description").setValue(description)
                    .addOnSuccessListener(aVoid -> {
                        binding.userName.setText(name);
                        binding.userDescription.setText(description);
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
