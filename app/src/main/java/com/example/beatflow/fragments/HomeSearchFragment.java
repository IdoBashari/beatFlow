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

import com.example.beatflow.Data.Song;
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
        playlistAdapter = new PlaylistAdapter(
                new ArrayList<>(),
                new PlaylistAdapter.OnPlaylistClickListener() {
                    @Override
                    public void onPlaylistClick(Playlist playlist) {
                        HomeSearchFragment.this.onPlaylistClick(playlist);
                    }
                },
                new PlaylistAdapter.OnPlaylistLongClickListener() {
                    @Override
                    public boolean onPlaylistLongClick(Playlist playlist) {
                        return false; // או כל לוגיקה אחרת שתרצה ללחיצה ארוכה
                    }
                }
        );
        userAdapter = new UserAdapter(new ArrayList<>(), this::onUserClick);

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
        } else {
            searchPlaylists(query);
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
                        user.setId(userSnapshot.getKey());
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
        Query searchQuery = databaseRef.child("users").orderByChild("playlists/name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limitToFirst(10);

        searchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Playlist> playlists = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot playlistsSnapshot = userSnapshot.child("playlists");
                    for (DataSnapshot playlistSnapshot : playlistsSnapshot.getChildren()) {
                        String id = playlistSnapshot.getKey();
                        String name = playlistSnapshot.child("name").getValue(String.class);
                        String description = playlistSnapshot.child("description").getValue(String.class);
                        Long songCountLong = playlistSnapshot.child("songCount").getValue(Long.class);
                        int songCount = songCountLong != null ? songCountLong.intValue() : 0;
                        String imageUrl = playlistSnapshot.child("imageUrl").getValue(String.class);
                        String creatorId = userSnapshot.getKey();
                        List<Song> songs = new ArrayList<>(); // יש להוסיף לוגיקה לטעינת השירים אם נדרש

                        if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                            Playlist playlist = new Playlist(id, name, description, songCount, imageUrl, songs, creatorId);
                            playlists.add(playlist);
                        }
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
        binding.textViewNoResults.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerViewUsers.setVisibility(binding.radioButtonUsers.isChecked() && !isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerViewPlaylists.setVisibility(binding.radioButtonPlaylists.isChecked() && !isEmpty ? View.VISIBLE : View.GONE);
    }

    private void onUserClick(User user) {
        UserProfileFragment userProfileFragment = UserProfileFragment.newInstance(user.getId());
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, userProfileFragment)
                .addToBackStack(null)
                .commit();
    }

    private void onPlaylistClick(Playlist playlist) {
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
        databaseRef.child("users").limitToFirst(5).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Playlist> recommendedPlaylists = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot playlistsSnapshot = userSnapshot.child("playlists");
                    for (DataSnapshot playlistSnapshot : playlistsSnapshot.getChildren()) {
                        Playlist playlist = playlistSnapshot.getValue(Playlist.class);
                        if (playlist != null) {
                            playlist.setId(playlistSnapshot.getKey());
                            playlist.setCreatorId(userSnapshot.getKey());
                            recommendedPlaylists.add(playlist);
                            if (recommendedPlaylists.size() >= 5) break;
                        }
                    }
                    if (recommendedPlaylists.size() >= 5) break;
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