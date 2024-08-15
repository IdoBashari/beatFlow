package com.example.beatflow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.beatflow.Data.Song;
import java.util.List;
import android.util.Log;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private List<Song> songs;

    public SongAdapter(List<Song> songs) {
        this.songs = songs;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        if (songs != null && position < songs.size()) {
            Song song = songs.get(position);
            holder.bind(song, position + 1);
            Log.d("SongAdapter", "Binding song: " + song.getName() + " by " + song.getArtist() + " at position " + position);
        } else {
            Log.d("SongAdapter", "No song to bind at position " + position);
        }
    }


    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        private TextView songNumber;
        private TextView songName;
        private TextView artistName;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songNumber = itemView.findViewById(R.id.song_number);
            songName = itemView.findViewById(R.id.song_name);
            artistName = itemView.findViewById(R.id.artist_name);
        }

        void bind(Song song, int number) {
            songNumber.setText(String.format("%d.", number));
            songName.setText(song.getName());
            artistName.setText(song.getArtist());
        }
    }
}