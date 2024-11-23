package com.example.glassesguru;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;

public class PhotoFullscreenActivity extends AppCompatActivity {
    private ImageButton back_button;
    private ImageButton share_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_fullscreen);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ImageView photoImageView = findViewById(R.id.photo_fullscreen_image_view);

        back_button = findViewById(R.id.back_button);
        share_btn = findViewById(R.id.share_btn);

        String photoPath = getIntent().getStringExtra("photoPath");
        if (photoPath != null) {
            File photoFile = new File(photoPath);
            Glide.with(this).load(photoFile).into(photoImageView);
        }

        String photoPathUrl = getIntent().getStringExtra("photoPathUrl");
        if(photoPathUrl != null) {
            Glide.with(this)
                    .load(photoPathUrl)
                    .placeholder(R.drawable.loading_circle) // Placeholder while the image loads
                    .error(R.drawable.ic_error)             // Image displayed on error
                    .into(photoImageView);
        }

        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        share_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharePhoto(photoPath);
            }
        });
    }

    private void sharePhoto(String photoPath) {
        if (photoPath != null) {
            File photoFile = new File(photoPath);
            Uri photoUri = FileProvider.getUriForFile(this,
                    "com.example.glassesguru.provider", photoFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, photoUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Image"));
        } else {
            Toast.makeText(this, "No photo available to share", Toast.LENGTH_SHORT).show();
        }
    }
}
