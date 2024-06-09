package com.example.glassesguru;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class LensesOptionActivity extends AppCompatActivity {

    private TextView priceTextView;
    private Button nextButton;

    public enum FragmentType {
        LENSES,
        PRESCRIPTION,
        LENSESTYPE
    }
    private Bundle eyeglassesBundle;

    private FragmentType currentFragment = FragmentType.LENSES;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lenses_option);
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

        // Initially load the LensesFragment
        loadFragment(FragmentType.LENSES);

        priceTextView = findViewById(R.id.priceTextView);

        Intent intent = getIntent();
        String title = intent.getStringExtra("Title");
        String frameType = intent.getStringExtra("FrameType");
        String type = intent.getStringExtra("Type");
        String price = intent.getStringExtra("Price");
        float size = intent.getFloatExtra("Size", 1.0f);
        String description = intent.getStringExtra("Description");
        int color = intent.getIntExtra("Color", Color.BLACK);
        String ID = intent.getStringExtra("ID");

        eyeglassesBundle = new Bundle();
        eyeglassesBundle.putString("Title", title);
        eyeglassesBundle.putString("FrameType", frameType);
        eyeglassesBundle.putString("Type", type);
        eyeglassesBundle.putString("Price", price);
        eyeglassesBundle.putFloat("Size", size);
        eyeglassesBundle.putString("Description", description);
        eyeglassesBundle.putInt("Color", color);
        eyeglassesBundle.putString("ID", ID);

        String pesoSymbol = "₱";
        String formattedPrice = pesoSymbol + price;
        priceTextView.setText(formattedPrice);

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            onBackPressed();
        });

        nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(v -> {
            if (currentFragment == FragmentType.LENSES) {
                loadFragment(FragmentType.PRESCRIPTION);
            } else if (currentFragment == FragmentType.PRESCRIPTION) {
                loadFragment(FragmentType.LENSESTYPE);
            }
        });
    }

    private void loadFragment(FragmentType fragmentType) {
        Fragment fragment = null;
        switch (fragmentType) {
            case LENSES:
                fragment = new LensesFragment();
                currentFragment = FragmentType.LENSES;
                break;
            case PRESCRIPTION:
                fragment = new PrescriptionFragment();
                currentFragment = FragmentType.PRESCRIPTION;
                break;
            case LENSESTYPE:
                fragment = new LensesTypeFragment();
                currentFragment = FragmentType.LENSESTYPE;
                break;
        }

        if (fragment != null) {

            fragment.setArguments(eyeglassesBundle);

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setReorderingAllowed(true);
            transaction.replace(R.id.fragment_container, fragment, null);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 1) {
            fragmentManager.popBackStack();
            if (currentFragment == FragmentType.LENSESTYPE) {
                currentFragment = FragmentType.PRESCRIPTION;
            } else if (currentFragment == FragmentType.PRESCRIPTION) {
                currentFragment = FragmentType.LENSES;
            }
        } else if (currentFragment == FragmentType.LENSES) {
            finish();
        } else if (currentFragment == FragmentType.PRESCRIPTION) {
            loadFragment(FragmentType.LENSES);
        } else if (currentFragment == FragmentType.LENSESTYPE) {
            loadFragment(FragmentType.PRESCRIPTION);
        } else {
            loadFragment(FragmentType.LENSES);
        }
    }
}