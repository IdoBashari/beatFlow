package com.example.beatflow;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beatflow.Data.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private OnUserClickListener clickListener;
    private int itemCount;

    public UserAdapter(List<User> users, OnUserClickListener clickListener) {
        this.users = new ArrayList<>(users);
        this.clickListener = clickListener;
        this.itemCount = users.size();
    }

    public void updateUsers(List<User> newUsers) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new UserDiffCallback(this.users, newUsers));
        this.users = new ArrayList<>(newUsers);
        diffResult.dispatchUpdatesTo(this);
        Log.d("UserAdapter", " " + this.users.size());
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, clickListener);
        Log.d("UserAdapter", "Binding user at position " + position + ": " + user.getName() + ", ID: " + user.getId());
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ImageView userImage;
        private final TextView userName;
        private final TextView userDescription;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
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

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onUserClick(user);
                }
            });

            Log.d("UserViewHolder", "Bound user: " + user.getName() + ", ID: " + user.getId());
        }
    }

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private class UserDiffCallback extends DiffUtil.Callback {
        private final List<User> oldUsers;
        private final List<User> newUsers;

        UserDiffCallback(List<User> oldUsers, List<User> newUsers) {
            this.oldUsers = oldUsers;
            this.newUsers = newUsers;
        }

        @Override
        public int getOldListSize() {
            return oldUsers.size();
        }

        @Override
        public int getNewListSize() {
            return newUsers.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldUsers.get(oldItemPosition).getId().equals(newUsers.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldUsers.get(oldItemPosition).equals(newUsers.get(newItemPosition));
        }
    }
}