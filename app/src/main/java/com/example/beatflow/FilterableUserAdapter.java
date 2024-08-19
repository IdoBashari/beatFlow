package com.example.beatflow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beatflow.Data.User;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FilterableUserAdapter extends RecyclerView.Adapter<FilterableUserAdapter.UserViewHolder> implements Filterable {

    private List<User> allUsers;
    private List<User> filteredUsers;
    private OnUserClickListener clickListener;

    public FilterableUserAdapter(List<User> users, OnUserClickListener clickListener) {
        this.allUsers = new ArrayList<>(users);
        this.filteredUsers = new ArrayList<>(users);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = filteredUsers.get(position);
        holder.bind(user, clickListener);
    }

    @Override
    public int getItemCount() {
        return filteredUsers.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String filterPattern = constraint.toString().toLowerCase().trim();

                List<User> filteredList;
                if (filterPattern.isEmpty()) {
                    filteredList = new ArrayList<>(allUsers);
                } else {
                    filteredList = allUsers.stream()
                            .filter(user -> user.getName().toLowerCase().contains(filterPattern))
                            .collect(Collectors.toList());
                }

                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredUsers = (List<User>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public void updateUsers(List<User> newUsers) {
        allUsers = new ArrayList<>(newUsers);
        filteredUsers = new ArrayList<>(newUsers);
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ShapeableImageView userImage;
        private final TextView userName;
        private final TextView userDescription;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            userImage = itemView.findViewById(R.id.userImage);
            userName = itemView.findViewById(R.id.userName);
            userDescription = itemView.findViewById(R.id.userDescription);
        }

        void bind(final User user, final OnUserClickListener clickListener) {
            userName.setText(user.getName());
            userDescription.setText(user.getDescription());

            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.circular_image)
                        .error(R.drawable.error_profile_image)
                        .circleCrop()
                        .into(userImage);
            } else {
                userImage.setImageResource(R.drawable.error_profile_image);
            }

            cardView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onUserClick(user);
                }
            });
        }
    }

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

}