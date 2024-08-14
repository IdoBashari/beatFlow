package com.example.beatflow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.example.beatflow.fragments.EmptyHomeFragment;
import com.example.beatflow.fragments.LoginFragment;
import com.example.beatflow.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import androidx.activity.OnBackPressedCallback;
public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

        new Thread(() -> {
            FirebaseApp.initializeApp(this);
            mAuth = FirebaseAuth.getInstance();
            runOnUiThread(() -> {
                setupBottomNavigation();
                if (savedInstanceState == null) {
                    loadInitialFragment();
                }
            });
        }).start();
    }
    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home_search) {
                    loadFragment(new EmptyHomeFragment());
                } else if (itemId == R.id.nav_add_playlist) {
                    openCreatePlaylistDialog();
                } else if (itemId == R.id.nav_profile) {
                    loadFragment(new ProfileFragment());
                }

                return true;
            });
        } else {
            Log.e("MainActivity", "BottomNavigationView not initialized");
        }
    }

    private void loadInitialFragment() {
        if (mAuth.getCurrentUser() != null) {
            loadFragment(new EmptyHomeFragment());
            bottomNav.setVisibility(View.VISIBLE);
        } else {
            loadFragment(new LoginFragment());
            bottomNav.setVisibility(View.GONE);
        }
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openCreatePlaylistDialog() {
        ProfileFragment profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (profileFragment != null) {
            profileFragment.showCreatePlaylistDialog();
        }
    }

    public void showBottomNav() {
        bottomNav.setVisibility(View.VISIBLE);
    }

    public void hideBottomNav() {
        bottomNav.setVisibility(View.GONE);
    }


}

