package com.example.beatflow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.beatflow.Data.Playlist;
import java.util.List;
import android.util.Log;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    private List<Playlist> playlists;
    private final OnPlaylistClickListener clickListener;
    private final OnPlaylistLongClickListener longClickListener;

    public PlaylistAdapter(List<Playlist> playlists, OnPlaylistClickListener clickListener, OnPlaylistLongClickListener longClickListener) {
        this.playlists = playlists;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }


    public void removePlaylist(Playlist playlist) {
        int position = playlists.indexOf(playlist);
        if (position != -1) {
            playlists.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        if (playlists != null && position < playlists.size()) {
            Playlist playlist = playlists.get(position);
            if (playlist != null) {
                holder.bind(playlist, clickListener, longClickListener);
            } else {
                Log.e("PlaylistAdapter", "Playlist at position " + position + " is null");
            }
        } else {
            Log.e("PlaylistAdapter", "Invalid position or playlists is null");
        }
    }


    @Override
    public int getItemCount() {
        return playlists != null ? playlists.size() : 0;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
        Log.d("PlaylistAdapter", "Setting " + playlists.size() + " playlists");
        notifyDataSetChanged();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private final ImageView playlistImage;
        private final TextView playlistName;
        private final TextView songCount;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistImage = itemView.findViewById(R.id.playlistImage);
            playlistName = itemView.findViewById(R.id.playlistNameSmall);
            songCount = itemView.findViewById(R.id.songCount);
        }

        void bind(final Playlist playlist, final OnPlaylistClickListener clickListener, final OnPlaylistLongClickListener longClickListener) {
            if (playlist != null) {
                playlistName.setText(playlist.getName());
                songCount.setText(itemView.getContext().getString(R.string.song_count, playlist.getSongCount()));

                if (playlist.getImageUrl() != null && !playlist.getImageUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(playlist.getImageUrl())
                            .placeholder(R.drawable.default_playlist_image)
                            .error(R.drawable.error_profile_image)
                            .into(playlistImage);
                } else {
                    playlistImage.setImageResource(R.drawable.default_playlist_image);
                }

                itemView.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onPlaylistClick(playlist);
                    }
                });

                itemView.setOnLongClickListener(v -> {
                    if (longClickListener != null) {
                        return longClickListener.onPlaylistLongClick(playlist);
                    }
                    return false;
                });
            } else {
                Log.e("PlaylistViewHolder", "Playlist is null");
            }
        }
    }

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public interface OnPlaylistLongClickListener {
        boolean onPlaylistLongClick(Playlist playlist);
    }


}