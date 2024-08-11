package com.example.beatflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import com.example.beatflow.MainActivity;
import com.example.beatflow.R;
import com.google.firebase.auth.FirebaseAuth;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Button logoutButton = view.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> logout());

        Button profileButton = view.findViewById(R.id.profile_button);
        profileButton.setOnClickListener(v -> openProfile());

        return view;
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        ((MainActivity) getActivity()).loadFragment(new LoginFragment());
    }

    private void openProfile() {
        ((MainActivity) getActivity()).loadFragment(new ProfileFragment());
    }
}