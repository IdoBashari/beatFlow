package com.example.beatflow;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.example.beatflow.fragments.HomeSearchFragment;
import com.example.beatflow.fragments.LoginFragment;
import com.example.beatflow.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends FragmentActivity {
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNav;
    private String currentFragmentTag;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);

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
            showProgressBar();
            FirebaseApp.initializeApp(this);
            mAuth = FirebaseAuth.getInstance();
            runOnUiThread(() -> {
                setupBottomNavigation();
                if (savedInstanceState == null) {
                    loadInitialFragment();
                }
                hideProgressBar();
            });
        }).start();
    }
    private void showProgressBar() {
        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
    }

    private void hideProgressBar() {
        runOnUiThread(() -> progressBar.setVisibility(View.GONE));
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

        // הוספת אנימציות מותאמות
        transaction.setCustomAnimations(
                R.anim.slide_in,        // Enter
                R.anim.slide_out,       // Exit
                R.anim.slide_in_left,   // Pop Enter
                R.anim.slide_out_right  // Pop Exit
        );

        transaction.replace(R.id.fragment_container, fragment, tag);
        if (tag != null) {
            transaction.addToBackStack(tag);
        }
        transaction.commit();
        currentFragmentTag = tag;

        Log.d("MainActivity", "Fragment loaded with tag: " + tag);
    }

    private void openCreatePlaylistDialog() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof ProfileFragment) {
            ((ProfileFragment) currentFragment).showCreatePlaylistDialog();
            Log.d("MainActivity", "Create playlist dialog opened from ProfileFragment.");
        } else {
            ProfileFragment profileFragment = new ProfileFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, profileFragment)
                    .addToBackStack(null)
                    .commit();

            getSupportFragmentManager().executePendingTransactions();

            profileFragment.showCreatePlaylistDialog();
            Log.d("MainActivity", "ProfileFragment loaded, and create playlist dialog opened.");
        }
    }
    public void showMessage(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
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