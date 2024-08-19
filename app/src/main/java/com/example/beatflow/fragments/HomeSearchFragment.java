package com.example.beatflow.fragments;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.beatflow.FilterableUserAdapter;
import com.example.beatflow.MainActivity;
import com.example.beatflow.R;
import com.example.beatflow.UserAdapter;
import com.example.beatflow.Data.User;
import com.example.beatflow.databinding.FragmentHomeSearchBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HomeSearchFragment extends Fragment {

    private FragmentHomeSearchBinding binding;
    private UserAdapter userAdapter;
    private DatabaseReference databaseRef;
    private List<User> allUsers = new ArrayList<>();
    private Handler searchHandler = new Handler();
    private static final long SEARCH_DELAY_MS = 300;
    private SwipeRefreshLayout swipeRefreshLayout;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }



    @Override

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        databaseRef = FirebaseDatabase.getInstance().getReference();
        userAdapter = new UserAdapter(new ArrayList<>(), this::onUserClick);

        setupRecyclerView();
        setupSearchView();
        loadAllUsers();
        setupSwipeRefresh();
    }

    private void setupRecyclerView() {
        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewUsers.setAdapter(userAdapter);
        binding.recyclerViewUsers.setHasFixedSize(true);

        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.item_spacing);
        binding.recyclerViewUsers.addItemDecoration(new SpacingItemDecoration(spacingInPixels));
    }
    private void setupSwipeRefresh() {
        swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_purple, R.color.dark_purple, R.color.light_purple);
    }
    private void refreshData() {
        loadAllUsers();
    }


    private void setupSearchView() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                userAdapter.getFilter().filter(s);
            }
        });
    }
    private void loadAllUsers() {
        showLoading(true);
        databaseRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        user.setId(userSnapshot.getKey());
                        users.add(user);
                    }
                }
                updateSearchResults(users);
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showLoading(false);
                showErrorMessage("Failed to load users. Please try again.");
            }
        });
    }
    private void showErrorMessage(String message) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showMessage(message);
        }
    }

    private void performSearch(String query) {
        String lowercaseQuery = query.toLowerCase();
        if (lowercaseQuery.isEmpty()) {
            updateSearchResults(allUsers);
        } else {
            List<User> filteredUsers = allUsers.stream()
                    .filter(user -> user.getName().toLowerCase().contains(lowercaseQuery))
                    .collect(Collectors.toList());
            updateSearchResults(filteredUsers);
        }
    }

    private void searchUsers(String query) {
        binding.recyclerViewUsers.setVisibility(View.VISIBLE);
        binding.progressBarLoading.setVisibility(View.VISIBLE);
        binding.textViewNoResults.setVisibility(View.GONE);

        Query searchQuery = databaseRef.child("users").orderByChild("name");
        searchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null && user.getName().toLowerCase().contains(query.toLowerCase())) {
                        user.setId(userSnapshot.getKey());
                        users.add(user);
                    }
                }
                updateSearchResults(users);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                updateSearchResults(new ArrayList<>());
                Log.e("HomeSearchFragment", "  : " + databaseError.getMessage());
            }
        });
    }

    private void updateSearchResults(List<User> users) {
        userAdapter.updateUsers(users);
        binding.searchResultsCount.setText(getString(R.string.search_results_count, users.size()));
        binding.searchResultsCount.setVisibility(View.VISIBLE);
        updateEmptyView(users.isEmpty());
    }

    private void clearSearchResults() {
        updateSearchResults(new ArrayList<>());
    }

    private void updateEmptyView(boolean isEmpty) {
        binding.textViewNoResults.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerViewUsers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void onUserClick(User user) {
        if (user.getId() != null && !user.getId().isEmpty()) {
            UserProfileFragment userProfileFragment = UserProfileFragment.newInstance(user.getId());
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in,
                            R.anim.slide_out,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                    )
                    .replace(R.id.fragment_container, userProfileFragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            Toast.makeText(getContext(), "Error loading user profile. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRecommendations() {
        showLoading(true);
        loadRecommendedUsers();
    }

    private void showLoading(boolean isLoading) {
        binding.progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.recyclerViewUsers.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void loadRecommendedUsers() {
        databaseRef.child("users").limitToFirst(5).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<User> recommendedUsers = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        user.setId(userSnapshot.getKey());
                        recommendedUsers.add(user);
                    }
                }
                updateSearchResults(recommendedUsers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error loading recommended users: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private class SpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spacing;

        SpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.bottom = spacing;
        }
    }
}