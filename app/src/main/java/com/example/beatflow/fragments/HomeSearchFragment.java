package com.example.beatflow.fragments;

import android.os.Bundle;
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


import com.example.beatflow.PlaylistAdapter;
import com.example.beatflow.R;
import com.example.beatflow.UserAdapter;
import com.example.beatflow.Data.Playlist;
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
import androidx.recyclerview.widget.DividerItemDecoration;

public class HomeSearchFragment extends Fragment {

    private FragmentHomeSearchBinding binding;
    private PlaylistAdapter playlistAdapter;
    private UserAdapter userAdapter;
    private DatabaseReference databaseRef;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        databaseRef = FirebaseDatabase.getInstance().getReference();

        setupRecyclerViews();
        setupSearchView();
        loadRecommendations();
    }

    private void setupRecyclerViews() {
        // Initialize PlaylistAdapter
        playlistAdapter = new PlaylistAdapter(
                new ArrayList<>(),
                this::onPlaylistClick,
                playlist -> {
                    // Handle long click if needed
                    Log.d("HomeSearchFragment", "Playlist long clicked: " + playlist.getName());
                    return true;
                }
        );

        // Initialize UserAdapter
        userAdapter = new UserAdapter(new ArrayList<>(), this::onUserClick);

        // Setup RecyclerView for Playlists
        binding.recyclerViewPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewPlaylists.setAdapter(playlistAdapter);
        binding.recyclerViewPlaylists.setHasFixedSize(true);

        // Setup RecyclerView for Users
        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewUsers.setAdapter(userAdapter);
        binding.recyclerViewUsers.setHasFixedSize(true);

        // Add item decoration for better visual separation (optional)
        int verticalSpaceHeight = getResources().getDimensionPixelSize(R.dimen.item_vertical_space);
        DividerItemDecoration itemDecoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        binding.recyclerViewPlaylists.addItemDecoration(itemDecoration);
        binding.recyclerViewUsers.addItemDecoration(itemDecoration);

        Log.d("HomeSearchFragment", "RecyclerViews setup completed");
    }



    private void setupSearchView() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 3) {
                    performSearch(s.toString());
                } else {
                    clearSearchResults();
                }
            }
        });

        binding.radioGroupSearchType.setOnCheckedChangeListener((group, checkedId) -> {
            performSearch(binding.editTextSearch.getText().toString());
        });
    }

    private void performSearch(String query) {
        if (query.length() < 3) {
            clearSearchResults();
            return;
        }

        if (binding.radioButtonUsers.isChecked()) {
            searchUsers(query);
        } else {
            searchPlaylists(query);
        }
    }

    private void searchUsers(String query) {
        Query searchQuery = databaseRef.child("users").orderByChild("nameLowerCase").startAt(query.toLowerCase()).endAt(query.toLowerCase() + "\uf8ff");
        searchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        user.setId(userSnapshot.getKey());
                        users.add(user);
                        Log.d("HomeSearchFragment", "Found user: " + user.getName() + " with ID: " + user.getId());
                    }
                }
                userAdapter.setUsers(users);
                updateEmptyView(users.isEmpty());
                Log.d("HomeSearchFragment", "Total users found: " + users.size());

                // עדכון ה-UI באופן מפורש
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        binding.recyclerViewUsers.setVisibility(View.VISIBLE);
                        binding.recyclerViewPlaylists.setVisibility(View.GONE);
                        userAdapter.notifyDataSetChanged();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("HomeSearchFragment", "Error searching users: " + databaseError.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error searching users. Please try again.", Toast.LENGTH_SHORT).show();
                        updateEmptyView(true);
                    });
                }
            }
        });
    }

    private void searchPlaylists(String query) {
        Query searchQuery = databaseRef.child("playlists").orderByChild("nameLowerCase").startAt(query.toLowerCase()).endAt(query.toLowerCase() + "\uf8ff");
        searchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Playlist> playlists = new ArrayList<>();
                for (DataSnapshot playlistSnapshot : dataSnapshot.getChildren()) {
                    Playlist playlist = playlistSnapshot.getValue(Playlist.class);
                    if (playlist != null) {
                        playlist.setId(playlistSnapshot.getKey());
                        playlists.add(playlist);
                        Log.d("HomeSearchFragment", "Found playlist: " + playlist.getName() + " with ID: " + playlist.getId());
                    }
                }
                playlistAdapter.setPlaylists(playlists);
                updateEmptyView(playlists.isEmpty());
                Log.d("HomeSearchFragment", "Total playlists found: " + playlists.size());

                // עדכון ה-UI באופן מפורש
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        binding.recyclerViewPlaylists.setVisibility(View.VISIBLE);
                        binding.recyclerViewUsers.setVisibility(View.GONE);
                        playlistAdapter.notifyDataSetChanged();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("HomeSearchFragment", "Error searching playlists: " + databaseError.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error searching playlists. Please try again.", Toast.LENGTH_SHORT).show();
                        updateEmptyView(true);
                    });
                }
            }
        });
    }

    private void clearSearchResults() {
        userAdapter.setUsers(new ArrayList<>());
        playlistAdapter.setPlaylists(new ArrayList<>());
        updateEmptyView(true);
    }

    private void updateEmptyView(boolean isEmpty) {
        binding.textViewNoResults.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        if (binding.radioButtonUsers.isChecked()) {
            binding.recyclerViewUsers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.recyclerViewPlaylists.setVisibility(View.GONE);
        } else {
            binding.recyclerViewPlaylists.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.recyclerViewUsers.setVisibility(View.GONE);
        }

        Log.d("HomeSearchFragment", "Updating empty view. isEmpty: " + isEmpty +
                ", Users checked: " + binding.radioButtonUsers.isChecked() +
                ", Playlists checked: " + binding.radioButtonPlaylists.isChecked() +
                ", Users visibility: " + binding.recyclerViewUsers.getVisibility() +
                ", Playlists visibility: " + binding.recyclerViewPlaylists.getVisibility());
    }

    private void onUserClick(User user) {
        Log.d("HomeSearchFragment", "Clicked on user: " + user.getName() + " with ID: " + user.getId());
        Toast.makeText(getContext(), "Clicked on user: " + user.getName(), Toast.LENGTH_SHORT).show();
        UserProfileFragment userProfileFragment = UserProfileFragment.newInstance(user.getId());
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, userProfileFragment)
                .addToBackStack(null)
                .commit();
    }

    private void onPlaylistClick(Playlist playlist) {
        Log.d("HomeSearchFragment", "Clicked on playlist: " + playlist.getName() + " with ID: " + playlist.getId());
        Toast.makeText(getContext(), "Clicked on playlist: " + playlist.getName(), Toast.LENGTH_SHORT).show();
        PlaylistDetailFragment playlistDetailFragment = PlaylistDetailFragment.newInstance(playlist.getCreatorId(), playlist.getId());
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, playlistDetailFragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadRecommendations() {
        loadRecommendedUsers();
        loadRecommendedPlaylists();
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
                userAdapter.setUsers(recommendedUsers);
                binding.recyclerViewUsers.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error loading recommended users: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecommendedPlaylists() {
        databaseRef.child("playlists").limitToFirst(5).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Playlist> recommendedPlaylists = new ArrayList<>();
                for (DataSnapshot playlistSnapshot : dataSnapshot.getChildren()) {
                    Playlist playlist = playlistSnapshot.getValue(Playlist.class);
                    if (playlist != null) {
                        playlist.setId(playlistSnapshot.getKey());
                        recommendedPlaylists.add(playlist);
                    }
                }
                playlistAdapter.setPlaylists(recommendedPlaylists);
                binding.recyclerViewPlaylists.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error loading recommended playlists: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}