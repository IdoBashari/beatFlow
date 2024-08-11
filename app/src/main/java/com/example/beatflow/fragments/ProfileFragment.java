package com.example.beatflow.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.beatflow.MainActivity;
import com.example.beatflow.PlaylistAdapter;
import com.example.beatflow.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private ImageView profileImage;
    private TextView userName;
    private TextView userDescription;
    private Button changeProfileImageButton;
    private Button logoutButton;
    private RecyclerView playlistsRecyclerView;
    private ActivityResultLauncher<String> mGetContent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadImageToFirebase(uri);
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initializeViews(view);
        setupClickListeners();
        loadUserProfile();
        setupPlaylistsRecyclerView(view);

        return view;
    }

    private void initializeViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        userName = view.findViewById(R.id.userName);
        userDescription = view.findViewById(R.id.userDescription);
        changeProfileImageButton = view.findViewById(R.id.changeProfileImageButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        playlistsRecyclerView = view.findViewById(R.id.playlistsRecyclerView);
    }

    private void setupClickListeners() {
        if (changeProfileImageButton != null) {
            changeProfileImageButton.setOnClickListener(v -> openImageChooser());
        }
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }
    }

    private void loadUserProfile() {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String name = document.getString("name");
                            String description = document.getString("description");
                            String imageUrl = document.getString("imageUrl");

                            if (userName != null) {
                                userName.setText(name != null ? name : "Unknown User");
                            }
                            if (userDescription != null) {
                                userDescription.setText(description != null ? description : "No description");
                            }

                            if (imageUrl != null && !imageUrl.isEmpty() && profileImage != null) {
                                // Load image using Glide or Picasso
                                // Glide.with(this).load(imageUrl).into(profileImage);
                            }
                        } else {
                            createNewUserProfile();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show());
        }
    }

    private void createNewUserProfile() {
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            Map<String, Object> user = new HashMap<>();
            user.put("name", name != null ? name : "New User");
            user.put("email", email);
            user.put("description", "I love music!");

            db.collection("users").document(currentUser.getUid()).set(user)
                    .addOnSuccessListener(aVoid -> loadUserProfile())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to create profile", Toast.LENGTH_SHORT).show());
        }
    }

    private void setupPlaylistsRecyclerView(View view) {
        if (playlistsRecyclerView != null) {
            playlistsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            List<String> dummyPlaylists = Arrays.asList("Rock Classics", "Chill Vibes", "Workout Mix");
            PlaylistAdapter adapter = new PlaylistAdapter(dummyPlaylists);
            playlistsRecyclerView.setAdapter(adapter);
        } else {
            Log.e(TAG, "PlaylistsRecyclerView is null");
        }
    }

    private void openImageChooser() {
        mGetContent.launch("image/*");
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (currentUser != null) {
            StorageReference imageRef = storageRef.child("profile_images/" + currentUser.getUid() + ".jpg");
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        updateProfileImageUrl(imageUrl);
                        // Load image using Glide or Picasso
                        // if (profileImage != null) {
                        //     Glide.with(this).load(imageUrl).into(profileImage);
                        // }
                    }))
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show());
        }
    }

    private void updateProfileImageUrl(String imageUrl) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .update("imageUrl", imageUrl)
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update profile image", Toast.LENGTH_SHORT).show());
        }
    }

    private void logout() {
        mAuth.signOut();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new LoginFragment());
        }
    }
}