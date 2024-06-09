package com.example.glassesguru;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class GlassesActivity extends AppCompatActivity {
    private TextView priceTextView;
    private ImageView glassesImageView, favoriteImageView;
    private PrefManager prefManager;
    Button selectLensesButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_glasses);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        // Configure the behavior of the hidden system bars.
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        prefManager = new PrefManager(this);
        glassesImageView = findViewById(R.id.glassesImageView);
        favoriteImageView = findViewById(R.id.favoriteImageView);
        priceTextView = findViewById(R.id.priceTextView);

        // Getting intent data
        Intent intent = getIntent();
        int imageResId = intent.getIntExtra("Image", -1);
        glassesImageView.setImageResource(imageResId);

        String ID = intent.getStringExtra("ID");
        String title = intent.getStringExtra("Title");
        String frameType = intent.getStringExtra("FrameType");
        String type = intent.getStringExtra("Type");
        String price = intent.getStringExtra("Price");
        float size = intent.getFloatExtra("Size", 1.0f);
        String description = intent.getStringExtra("Description");
        int color = intent.getIntExtra("Color", Color.BLACK);
        int lensesColor = intent.getIntExtra("LensesColor", Color.BLACK);



        String pesoSymbol = "₱";
        String formattedPrice = pesoSymbol + price;
        priceTextView.setText(formattedPrice);

        EyeglassesFragment eyeglassesFragment = new EyeglassesFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setReorderingAllowed(true);
        transaction.replace(R.id.fragment_container, eyeglassesFragment, null);
        transaction.commit();

        Bundle eyeglassesBundle = new Bundle();
        eyeglassesBundle.putString("Title", title);
        eyeglassesBundle.putString("FrameType", frameType);
        eyeglassesBundle.putString("Type", type);
        eyeglassesBundle.putString("Price", price);
        eyeglassesBundle.putFloat("Size", size);
        eyeglassesBundle.putString("Description", description);
        eyeglassesBundle.putInt("Color", color);
        eyeglassesBundle.putInt("LensesColor", lensesColor);
        eyeglassesBundle.putString("ID", ID);

        eyeglassesFragment.setArguments(eyeglassesBundle);

        if(prefManager.isFavorite(ID)) {
            favoriteImageView.setImageResource(R.drawable.ic_favorite_filled);
        }

        favoriteImageView.setOnClickListener(v -> {
            if (prefManager != null) {
                if (prefManager.isFavorite(ID)) {
                    favoriteImageView.setImageResource(R.drawable.ic_favorite_outline);
                    prefManager.removeFromFavorites(ID);
                } else {
                    favoriteImageView.setImageResource(R.drawable.ic_favorite_filled);
                    prefManager.addToFavorites(ID);
                }
            }
        });

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        selectLensesButton = findViewById(R.id.selectLensesButton);
        selectLensesButton.setOnClickListener(v -> {
            Intent intentLenses = new Intent(GlassesActivity.this, LensesOptionActivity.class);
            intentLenses.putExtra("Price", price);
            intentLenses.putExtra("Title", title);
            intentLenses.putExtra("FrameType", frameType);
            intentLenses.putExtra("Type", type);
            intentLenses.putExtra("Price", price);
            intentLenses.putExtra("Size", size);
            intentLenses.putExtra("Description", description);
            intentLenses.putExtra("Color", color);
            intentLenses.putExtra("LensesColor", lensesColor);
            intentLenses.putExtra("ID", ID);
            startActivity(intentLenses);
        });
    }
}