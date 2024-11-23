package com.example.glassesguru;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {
    RelativeLayout about_Layout, terms_and_conditions_Layout, themes_Layout;
    PrefManager prefManager;
    String themeColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        prefManager = new PrefManager(this);

        about_Layout = findViewById(R.id.about_Layout);
        terms_and_conditions_Layout = findViewById(R.id.terms_and_conditions_Layout);

        about_Layout.setOnClickListener(v -> {
            showAboutDialog();
        });

        terms_and_conditions_Layout.setOnClickListener(v -> {
           showTermsAndConditionsDialog();
        });

        themes_Layout = findViewById(R.id.themes_layout);
        themes_Layout.setOnClickListener(v -> {
            showThemesDialog();
        });
    }

    private void showAboutDialog() {
        View aboutDialogView = getLayoutInflater().inflate(R.layout.dialog_text_view, null);
        TextView content_TextView = aboutDialogView.findViewById(R.id.content_TextView);

        content_TextView.setText(Html.fromHtml(getString(R.string.about_content), Html.FROM_HTML_MODE_LEGACY));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(aboutDialogView)
                .setTitle("About")
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog aboutDialog = builder.create();
        aboutDialog.show();
    }

    private void showTermsAndConditionsDialog() {
        View termsAndConditionsView = getLayoutInflater().inflate(R.layout.dialog_terms_and_conditions, null);
        TextView termsAndConditionsEditText = termsAndConditionsView.findViewById(R.id.tv_terms_conditions);
        termsAndConditionsEditText.setText(Html.fromHtml(getString(R.string.terms_and_conditions), Html.FROM_HTML_MODE_LEGACY));

        android.app.AlertDialog.Builder termsAndConditionsDialogBuilder = new android.app.AlertDialog.Builder(this);
        termsAndConditionsDialogBuilder.setView(termsAndConditionsView)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        android.app.AlertDialog termsAndConditionsDialog = termsAndConditionsDialogBuilder.create();
        termsAndConditionsDialog.show();
    }

    private void showThemesDialog() {
        View themeDialogView = getLayoutInflater().inflate(R.layout.dialog_theme_picker, null);
        AlertDialog.Builder themeDialogBuilder = new AlertDialog.Builder(this);
        LinearLayout colorBlackLayout = themeDialogView.findViewById(R.id.color_black_Layout);
        ImageView selectedBlack = themeDialogView.findViewById(R.id.selected_black);
        ImageView selectedSlate = themeDialogView.findViewById(R.id.selected_slate);

        themeColor = prefManager.getThemeColor();

        switch (themeColor) {
            case "dark":
                selectedSlate.setVisibility(View.GONE);
                selectedBlack.setVisibility(View.VISIBLE);
                break;
            case "slate":
                selectedSlate.setVisibility(View.VISIBLE);
                selectedBlack.setVisibility(View.GONE);
                break;
        }

        colorBlackLayout.setOnClickListener(v -> {
            themeColor = "dark";
            selectedSlate.setVisibility(View.GONE);
            selectedBlack.setVisibility(View.VISIBLE);
        });

        LinearLayout colorSlateLayout = themeDialogView.findViewById(R.id.color_slate_Layout);
        colorSlateLayout.setOnClickListener(v -> {
            themeColor = "slate";
            selectedSlate.setVisibility(View.VISIBLE);
            selectedBlack.setVisibility(View.GONE);
        });

        themeDialogBuilder.setView(themeDialogView)
                .setPositiveButton("Okay", (dialog, which) -> {
                    if(prefManager.getThemeColor().equals(themeColor)) {
                        dialog.dismiss();
                    } else {
                        prefManager.setThemeColor(themeColor);
                        Intent intent = new Intent(getApplicationContext(), CameraFaceActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finishAffinity();
                        System.exit(0);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog themeDialog = themeDialogBuilder.create();
        themeDialog.show();
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