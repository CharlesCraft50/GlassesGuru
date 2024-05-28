package com.example.glassesguru;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class PhotoManagerActivity extends AppCompatActivity implements PhotoAdapter.OnPhotoClickListener {

    private RecyclerView recyclerView;
    private List<File> photoList;
    private PhotoAdapter photoAdapter;
    private File currentPhoto;
    private LinearLayout no_image_Layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_manager);

        Toolbar toolbar = findViewById(R.id.photo_manager_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        no_image_Layout = findViewById(R.id.no_image_Layout);
        recyclerView = findViewById(R.id.photo_recycler_view);
        photoList = new ArrayList<>();
        photoAdapter = new PhotoAdapter(photoList, this);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(photoAdapter);

        loadPhotos();
        if(photoList.size() == 1) {
            no_image_Layout.setVisibility(View.VISIBLE);
        }
    }

    private void loadPhotos() {
        // Retrieve the list of saved photos from your app's directory
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");

        // Ensure directory exists
        if (!directory.exists() || !directory.isDirectory()) {
            Toast.makeText(this, "Photo directory not found", Toast.LENGTH_SHORT).show();
            return;
        }

        File[] files = directory.listFiles();

        if (files == null || files.length == 0) {
            Toast.makeText(this, "No photos found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sort photos by date (latest first)
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                long lastModified1 = file1.lastModified();
                long lastModified2 = file2.lastModified();
                // Sort in descending order (latest first)
                return Long.compare(lastModified2, lastModified1);
            }
        });

        // Clear the existing photo list
        photoList.clear();

        // Add the retrieved photo files to the photoList
        photoList.addAll(Arrays.asList(files));

        // Notify the adapter that the data set has changed
        photoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPhotoClick(int position) {
        // Open the selected photo in full-screen mode
        currentPhoto = photoList.get(position);
        Intent intent = new Intent(this, PhotoFullscreenActivity.class);
        intent.putExtra("photoPath", currentPhoto.getAbsolutePath());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(int position) {
        // Delete the selected photo
        File photoToDelete = photoList.get(position);
        if (photoToDelete.delete()) {
            photoList.remove(position);
            photoAdapter.notifyItemRemoved(position);
            Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();

            // Decrement the position for subsequent items
            for (int i = position; i < photoList.size(); i++) {
                photoAdapter.notifyItemChanged(i); // Update the item views
            }

            // Check if the photo list has only one item after deletion
            if (photoList.size() == 1) {
                no_image_Layout.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, "Failed to delete photo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();  // Ensure the activity finishes and goes back to the previous screen
    }
}