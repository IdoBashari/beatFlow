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
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.beatflow.MainActivity;
import com.example.beatflow.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
                result -> {
                    if (result != null) {
                        uploadImageToFirebase(result);
                    } else {
                        Log.e(TAG, "No image selected");
                        Toast.makeText(getContext(), "No image selected", Toast.LENGTH_SHORT).show();
                    }
                });

        checkPermissions();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        profileImage = view.findViewById(R.id.profileImage);
        userName = view.findViewById(R.id.userName);
        userDescription = view.findViewById(R.id.userDescription);
        changeProfileImageButton = view.findViewById(R.id.changeProfileImageButton);
        changeProfileImageButton.setOnClickListener(v -> mGetContent.launch("image/*"));
        Button logoutButton = view.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> logout());
        if (currentUser != null) {
            userName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Guest");
        }
        return view;
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        ((MainActivity) getActivity()).loadFragment(new LoginFragment());
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(getContext(), "Image URI is null", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated");
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        StorageReference imageRef = storageRef.child("profile_images/" + currentUser.getUid() + ".jpg");
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    updateProfileImageUrl(imageUrl);
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload image", e);
                    Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfileImageUrl(String imageUrl) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .update("imageUrl", imageUrl)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Profile image URL updated successfully"))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update profile image URL", e);
                        Toast.makeText(getContext(), "Failed to update profile image", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission granted to read external storage");
            } else {
                Toast.makeText(getContext(), "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
