package com.example.beatflow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.example.beatflow.fragments.HomeSearchFragment;
import com.example.beatflow.fragments.LoginFragment;
import com.example.beatflow.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNav;
    private String currentFragmentTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
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
    public void popBackStack() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            finish();
        }
    }
    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home_search) {
                    loadFragment(new HomeSearchFragment(), "home");
                } else if (itemId == R.id.nav_add_playlist) {
                    openCreatePlaylistDialog();
                } else if (itemId == R.id.nav_profile) {
                    loadFragment(new ProfileFragment(), "profile");
                }

                return true;
            });
        } else {
            Log.e("MainActivity", "BottomNavigationView not initialized");
        }
    }

    private void loadInitialFragment() {
        if (mAuth.getCurrentUser() != null) {
            loadFragment(new HomeSearchFragment(), "home");
            bottomNav.setVisibility(View.VISIBLE);
        } else {
            loadFragment(new LoginFragment(), "login");
            bottomNav.setVisibility(View.GONE);
        }
    }

    public void loadFragment(Fragment fragment) {
        loadFragment(fragment, null);
    }

    public void loadFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);
        if (tag != null) {
            transaction.addToBackStack(tag);
        }
        transaction.commit();
        currentFragmentTag = tag;
    }



    private void openCreatePlaylistDialog() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof ProfileFragment) {
            ((ProfileFragment) currentFragment).showCreatePlaylistDialog();
        } else {
            // Load ProfileFragment and then show dialog
            ProfileFragment profileFragment = new ProfileFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, profileFragment)
                    .addToBackStack(null)
                    .commit();

            // Wait for the fragment transaction to complete
            getSupportFragmentManager().executePendingTransactions();

            profileFragment.showCreatePlaylistDialog();
        }
    }

    public void showBottomNav() {
        bottomNav.setVisibility(View.VISIBLE);
    }

    public void hideBottomNav() {
        bottomNav.setVisibility(View.GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentFragmentTag", currentFragmentTag);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            currentFragmentTag = savedInstanceState.getString("currentFragmentTag");
            if (currentFragmentTag != null) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
                if (currentFragment != null) {
                    loadFragment(currentFragment, currentFragmentTag);
                }
            }
        }
    }



}

