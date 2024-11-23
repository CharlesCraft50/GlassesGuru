package com.example.glassesguru;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<File> photoList;
    private OnPhotoClickListener listener;

    public interface OnPhotoClickListener {
        void onPhotoClick(int position);
        void onDeleteClick(int position);
    }

    public PhotoAdapter(List<File> photoList, OnPhotoClickListener listener) {
        this.photoList = photoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        File photo = photoList.get(position);
        Glide.with(holder.itemView.getContext())
                .load(photo)
                .into(holder.photoImageView);

        holder.deleteButton.setOnClickListener(view -> listener.onDeleteClick(position));
        holder.itemView.setOnClickListener(view -> listener.onPhotoClick(position));
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView photoImageView;
        ImageButton deleteButton;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            photoImageView = itemView.findViewById(R.id.photo_image_view);
            deleteButton = itemView.findViewById(R.id.close_recommendation_Button);
        }
    }
}

