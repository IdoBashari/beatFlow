package com.example.beatflow.fragments;



import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beatflow.Data.Song;
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
import java.util.List;
import java.util.UUID;

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

    private static RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();


    private Uri selectedImageUri = null;
    private AlertDialog currentDialog;



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedPlaylistImageUri = result.getData().getData();
                        if (currentDialog != null && currentDialog.isShowing()) {
                            ImageView playlistImagePreview = currentDialog.findViewById(R.id.playlist_image_preview);
                            if (playlistImagePreview != null) {
                                playlistImagePreview.setImageURI(selectedPlaylistImageUri);
                            }
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        initFirebase();
        binding.playlistsRecyclerView.setRecycledViewPool(sharedPool);
        return binding.getRoot();
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUserInfo();
        setupPlaylistRecyclerView();
        loadPlaylists();
        setupEditProfileButton();

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).popBackStack();
                        }
                    }
                });
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
            new Thread(() -> {
                databaseReference.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String name = dataSnapshot.child("name").getValue(String.class);
                            String description = dataSnapshot.child("description").getValue(String.class);
                            String profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);

                            currentUser = new User(uid, name, user.getEmail(), description, profileImageUrl);

                            requireActivity().runOnUiThread(() -> {
                                if (name != null) binding.userName.setText(name);
                                if (description != null) binding.userDescription.setText(description);
                                if (profileImageUrl != null) loadProfileImage(profileImageUrl);
                            });
                        } else {
                            Log.w("ProfileFragment", "No user data found for UID: " + uid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show();
                        });
                        Log.e("ProfileFragment", "Error loading user data", databaseError.toException());
                    }
                });
            }).start();
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

    private Uri selectedPlaylistImageUri = null;

    public void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);
        EditText playlistNameInput = dialogView.findViewById(R.id.playlist_name_input);
        EditText playlistDescriptionInput = dialogView.findViewById(R.id.playlist_description_input);
        Button selectImageButton = dialogView.findViewById(R.id.select_image_button);
        ImageView playlistImagePreview = dialogView.findViewById(R.id.playlist_image_preview);

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        builder.setView(dialogView)
                .setPositiveButton("Create", (dialog, id) -> {
                    String playlistName = playlistNameInput.getText().toString();
                    String playlistDescription = playlistDescriptionInput.getText().toString();
                    createNewPlaylist(playlistName, playlistDescription, selectedPlaylistImageUri);
                })
                .setNegativeButton("Cancel", null);

        currentDialog = builder.create();
        currentDialog.show();
    }

    private void createNewPlaylist(String name, String description, Uri imageUri) {
        String playlistId = UUID.randomUUID().toString();

        if (imageUri != null) {
            StorageReference imageRef = storageRef.child("playlists/" + playlistId + ".jpg");
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            savePlaylistToDatabase(playlistId, name, description, imageUrl);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        savePlaylistToDatabase(playlistId, name, description, null);
                    });
        } else {
            savePlaylistToDatabase(playlistId, name, description, null);
        }
    }

    private void savePlaylistToDatabase(String playlistId, String name, String description, String imageUrl) {
        Playlist newPlaylist = new Playlist(playlistId, name, description, 0, imageUrl, new ArrayList<>(), firebaseAuth.getCurrentUser().getUid());

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            DatabaseReference playlistRef = databaseReference.child("users")
                    .child(user.getUid())
                    .child("playlists")
                    .child(playlistId);

            playlistRef.setValue(newPlaylist)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Playlist created successfully", Toast.LENGTH_SHORT).show();
                        loadPlaylists();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProfileFragment", "Failed to create playlist: " + e.getMessage());
                        Toast.makeText(requireContext(), "Failed to create playlist", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e("ProfileFragment", "User not logged in");
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }



    private void showEditOptionsDialog() {
        Context context = getContext();
        if (context != null) {
            CharSequence[] options = new CharSequence[]{"Change Profile Image", "Edit Profile", "Logout"};
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Choose option");
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    openImageChooser();
                } else if (which == 1) {
                    showEditProfileDialog();
                } else if (which == 2) {

                    firebaseAuth.signOut();

                    Intent intent = new Intent(context, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            });
            builder.show();
        } else {
            Log.e("ProfileFragment", "Context is null in showEditOptionsDialog");
        }
    }

    private void openImageChooser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
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



    private void setupPlaylistRecyclerView() {
        playlistAdapter = new PlaylistAdapter(
                new ArrayList<>(),
                new PlaylistAdapter.OnPlaylistClickListener() {
                    @Override
                    public void onPlaylistClick(Playlist playlist) {
                        handlePlaylistSelection(playlist);
                    }
                },
                new PlaylistAdapter.OnPlaylistLongClickListener() {
                    @Override
                    public boolean onPlaylistLongClick(Playlist playlist) {
                        return handlePlaylistLongClick(playlist);
                    }
                }
        );
        binding.playlistsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.playlistsRecyclerView.setAdapter(playlistAdapter);
    }

    private boolean handlePlaylistLongClick(Playlist playlist) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete this playlist?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deletePlaylist(playlist);
                })
                .setNegativeButton("Cancel", null)
                .show();
        return true;
    }
    private void deletePlaylist(Playlist playlist) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference playlistRef = databaseReference
                .child("users")
                .child(user.getUid())
                .child("playlists")
                .child(playlist.getId());

        playlistRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    playlistAdapter.removePlaylist(playlist);
                    playlistAdapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to delete playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }





    private void loadPlaylists() {
        binding.playlistsProgressBar.setVisibility(View.VISIBLE);
        binding.playlistsRecyclerView.setVisibility(View.GONE);
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            databaseReference.child("users").child(user.getUid()).child("playlists").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    ArrayList<Playlist> playlists = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String id = snapshot.getKey();
                        String name = snapshot.child("name").getValue(String.class);
                        String description = snapshot.child("description").getValue(String.class);
                        Long songCountLong = snapshot.child("songCount").getValue(Long.class);
                        int songCount = songCountLong != null ? songCountLong.intValue() : 0;
                        String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                        String creatorId = snapshot.child("creatorId").getValue(String.class);

                        List<Song> songs = new ArrayList<>();
                        DataSnapshot songsSnapshot = snapshot.child("songs");
                        if (songsSnapshot.exists()) {
                            for (DataSnapshot songSnapshot : songsSnapshot.getChildren()) {
                                Song song = songSnapshot.getValue(Song.class);
                                if (song != null) {
                                    songs.add(song);
                                }
                            }
                        }

                        Playlist playlist = new Playlist(id, name, description, songCount, imageUrl, songs, creatorId);
                        playlists.add(playlist);
                    }
                    requireActivity().runOnUiThread(() -> {
                        playlistAdapter.setPlaylists(playlists);
                        binding.playlistsProgressBar.setVisibility(View.GONE);
                        binding.playlistsRecyclerView.setVisibility(View.VISIBLE);
                        binding.noPlaylistsText.setVisibility(playlists.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    requireActivity().runOnUiThread(() -> {
                        binding.playlistsProgressBar.setVisibility(View.GONE);
                        binding.playlistsRecyclerView.setVisibility(View.GONE);
                        binding.noPlaylistsText.setVisibility(View.VISIBLE);
                        binding.noPlaylistsText.setText("Failed to load playlists");
                        Toast.makeText(requireContext(), "Failed to load playlists: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            binding.playlistsProgressBar.setVisibility(View.GONE);
            binding.noPlaylistsText.setVisibility(View.VISIBLE);
            binding.noPlaylistsText.setText("User not logged in");
        }
    }


    private void handlePlaylistSelection(Playlist playlist) {
        PlaylistDetailFragment detailFragment = PlaylistDetailFragment.newInstance(playlist.getCreatorId(), playlist.getId());
        ((MainActivity) requireActivity()).loadFragment(detailFragment, "playlist_detail");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}