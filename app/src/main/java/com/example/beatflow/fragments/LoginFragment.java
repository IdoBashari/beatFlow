package com.example.beatflow.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.beatflow.Data.User;
import com.example.beatflow.MainActivity;

import com.example.beatflow.databinding.FragmentLoginBinding;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private ActivityResultLauncher<Intent> signInLauncher;

    private static final String TAG = "LoginFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signInLauncher = registerForActivityResult(
                new FirebaseAuthUIActivityResultContract(),
                this::onSignInResult
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.loginButton.setOnClickListener(v -> signIn());
    }

    private void signIn() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build();
        signInLauncher.launch(signInIntent);
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        if (result.getResultCode() == -1) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                saveUserData(user);
                navigateToProfileScreen();
            } else {
                Log.e(TAG, "User is null after successful sign-in");
                Toast.makeText(requireContext(), "Sign in failed: User is null", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Sign in failed: Result code " + result.getResultCode());
            Toast.makeText(requireContext(), "Sign in failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserData(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        String displayName = firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail();
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null;

        User user = new User(
                userId,
                (displayName != null && !displayName.isEmpty()) ? displayName : "New User",
                (email != null && !email.isEmpty()) ? email : "No email provided",
                "",
                photoUrl
        );

        userRef.setValue(user)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved successfully for UID: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save user data for UID: " + userId, e));
    }

    private void navigateToProfileScreen() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new ProfileFragment());
        } else {
            Log.e(TAG, "Unable to navigate: Activity is not MainActivity");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}