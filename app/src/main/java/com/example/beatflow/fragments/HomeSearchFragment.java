package com.example.beatflow.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.beatflow.PlaylistAdapter;
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
    }

    private void setupRecyclerViews() {
        playlistAdapter = new PlaylistAdapter(new ArrayList<>(), playlist -> {
            // TODO: Handle playlist click
        }, playlist -> {
            // TODO: Handle playlist long click
            return true;
        });

        userAdapter = new UserAdapter(new ArrayList<>(), user -> {
            // TODO: Handle user click
        });

        binding.recyclerViewPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewPlaylists.setAdapter(playlistAdapter);

        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewUsers.setAdapter(userAdapter);
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
            binding.recyclerViewUsers.setVisibility(View.VISIBLE);
            binding.recyclerViewPlaylists.setVisibility(View.GONE);
        } else {
            searchPlaylists(query);
            binding.recyclerViewUsers.setVisibility(View.GONE);
            binding.recyclerViewPlaylists.setVisibility(View.VISIBLE);
        }
    }

    private void searchUsers(String query) {
        Query searchQuery = databaseRef.child("users").orderByChild("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limitToFirst(10);

        searchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        users.add(user);
                    }
                }
                userAdapter.setUsers(users);
                updateEmptyView(users.isEmpty());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error searching users: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchPlaylists(String query) {
        Query searchQuery = databaseRef.child("playlists").orderByChild("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limitToFirst(10);

        searchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Playlist> playlists = new ArrayList<>();
                for (DataSnapshot playlistSnapshot : dataSnapshot.getChildren()) {
                    Playlist playlist = playlistSnapshot.getValue(Playlist.class);
                    if (playlist != null) {
                        playlists.add(playlist);
                    }
                }
                playlistAdapter.setPlaylists(playlists);
                updateEmptyView(playlists.isEmpty());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error searching playlists: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearSearchResults() {
        userAdapter.setUsers(new ArrayList<>());
        playlistAdapter.setPlaylists(new ArrayList<>());
        updateEmptyView(true);
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            binding.textViewNoResults.setVisibility(View.VISIBLE);
            binding.recyclerViewUsers.setVisibility(View.GONE);
            binding.recyclerViewPlaylists.setVisibility(View.GONE);
        } else {
            binding.textViewNoResults.setVisibility(View.GONE);
            if (binding.radioButtonUsers.isChecked()) {
                binding.recyclerViewUsers.setVisibility(View.VISIBLE);
                binding.recyclerViewPlaylists.setVisibility(View.GONE);
            } else {
                binding.recyclerViewUsers.setVisibility(View.GONE);
                binding.recyclerViewPlaylists.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}