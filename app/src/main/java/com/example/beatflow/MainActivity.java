package com.example.beatflow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import com.example.beatflow.fragments.LoginFragment;
import com.example.beatflow.fragments.ProfileFragment;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();

        if (savedInstanceState == null) {
            loadInitialFragment();
        }
    }

    private void loadInitialFragment() {
        Fragment initialFragment;
        if (mAuth.getCurrentUser() != null) {
            initialFragment = new ProfileFragment();
        } else {
            initialFragment = new LoginFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, initialFragment)
                .commit();
    }

    public void loadFragment(Fragment fragment) {
        long startTime = System.currentTimeMillis();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        long endTime = System.currentTimeMillis();
        Log.d("MainActivity", "Load Fragment time: " + (endTime - startTime) + "ms");
    }
}
