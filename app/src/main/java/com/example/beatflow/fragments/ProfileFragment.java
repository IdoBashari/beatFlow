package com.example.beatflow.fragments;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private PlaylistAdapter playlistAdapter;
    private User currentUser;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                launchImagePicker();
            } else {
                Toast.makeText(requireContext(), "Permission denied. Cannot choose profile image.", Toast.LENGTH_SHORT).show();
            }
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        uploadProfileImage(imageUri);
                    }
                }
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
        setupPlaylistRecyclerView();
        loadPlaylists();
        setupEditProfileButton();
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

    private void setupEditProfileButton() {
        binding.fab.setOnClickListener(v -> showEditOptionsDialog());
    }

    private void showEditOptionsDialog() {
        CharSequence options[] = new CharSequence[]{"Change Profile Image", "Edit Profile", "Logout"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Choose option");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                openImageChooser();
            } else if (which == 1) {
                showEditProfileDialog();
            } else if (which == 2) {
                performLogout();
            }
        });
        builder.show();
    }

    private void openImageChooser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfileImage(Uri imageUri) {
        if (imageUri != null && firebaseAuth.getCurrentUser() != null) {
            binding.profileImageProgressBar.setVisibility(View.VISIBLE);
            StorageReference fileRef = storageRef.child("users/" + firebaseAuth.getCurrentUser().getUid() + "/profile.jpg");
            fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                databaseReference.child("users").child(firebaseAuth.getCurrentUser().getUid()).child("profileImageUrl").setValue(imageUrl)
                        .addOnSuccessListener(aVoid -> {
                            loadProfileImage(imageUrl);
                            binding.profileImageProgressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Profile image updated successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            binding.profileImageProgressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Failed to update profile image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            })).addOnFailureListener(e -> {
                binding.profileImageProgressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showEditProfileDialog() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User data not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        EditText nameEdit = dialogView.findViewById(R.id.edit_name);
        EditText descriptionEdit = dialogView.findViewById(R.id.edit_description);

        nameEdit.setText(currentUser.getName());
        descriptionEdit.setText(currentUser.getDescription());

        builder.setView(dialogView)
                .setPositiveButton("Save", (dialog, id) -> {
                    String newName = nameEdit.getText().toString().trim();
                    String newDescription = descriptionEdit.getText().toString().trim();
                    if (newName.isEmpty() || newDescription.isEmpty()) {
                        Toast.makeText(requireContext(), "Name and description cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }
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

    private void performLogout() {
        firebaseAuth.signOut();
        startActivity(new Intent(getContext(), MainActivity.class));
        getActivity().finish();
    }

    private void setupPlaylistRecyclerView() {
        playlistAdapter = new PlaylistAdapter(new ArrayList<>(), this::handlePlaylistSelection);
        binding.playlistsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.playlistsRecyclerView.setAdapter(playlistAdapter);
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
                            playlist.setId(snapshot.getKey());
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
