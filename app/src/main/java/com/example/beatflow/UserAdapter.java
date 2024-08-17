package com.example.beatflow;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.example.beatflow.Data.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private OnUserClickListener clickListener;

    public UserAdapter(List<User> users, OnUserClickListener clickListener) {
        this.users = users;
        this.clickListener = clickListener;
    }

    public void setUsers(List<User> users) {
        this.users = users;
        Log.d("UserAdapter", "Setting " + users.size() + " users");
        notifyDataSetChanged();
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
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
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

            Glide.with(itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.circular_image)
                    .error(R.drawable.error_profile_image)
                    .circleCrop()
                    .into(userImage);

            itemView.setOnClickListener(v -> {
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