package com.example.glassesguru;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GlassesActivity extends AppCompatActivity implements EyeglassesFragment.OnRadioButtonSelectedListener {
    private TextView priceTextView, notification_count_TextView;
    private ImageView glassesImageView, favoriteImageView;
    private PrefManager prefManager;
    private String functionValue = "";
    Button selectLensesButton;
    private int unseenCount = 0;
    FirebaseUser user;
    String userId;
    String modelBasePath;

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

        Toolbar toolbar = findViewById(R.id.glasses_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

//        WindowInsetsControllerCompat windowInsetsController =
//                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
//        // Configure the behavior of the hidden system bars.
//        windowInsetsController.setSystemBarsBehavior(
//                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//        );
//
//        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        modelBasePath = this.getExternalFilesDir(null).getAbsolutePath() + "/";

        prefManager = new PrefManager(this);
        glassesImageView = findViewById(R.id.glassesImageView);
        favoriteImageView = findViewById(R.id.favoriteImageView);
        priceTextView = findViewById(R.id.priceTextView);

        // Getting intent data
        Intent intent = getIntent();
        String ID = intent.getStringExtra("ID");
        Boolean isDownloaded = intent.getBooleanExtra("IsDownloaded", false);

        int imageResId = intent.getIntExtra("Image", -1);

        if(isDownloaded) {
            File imageFile = new File(modelBasePath, "glasses_" + ID + ".png");

            if(imageFile.exists()) {
                Glide.with(glassesImageView.getContext())
                        .load(imageFile)
                        .placeholder(R.drawable.baseline_image_24)
                        .error(R.drawable.ic_error)
                        .override(Target.SIZE_ORIGINAL)
                        .into(glassesImageView);
            }
        } else {
            glassesImageView.setImageResource(imageResId);
        }



        String title = intent.getStringExtra("Title");
        String frameType = intent.getStringExtra("FrameType");
        String type = intent.getStringExtra("Type");
        String price = intent.getStringExtra("Price");
        float size = intent.getFloatExtra("Size", 1.0f);
        String description = intent.getStringExtra("Description");
        int color = intent.getIntExtra("Color", Color.BLACK);
        int lensesColor = intent.getIntExtra("LensesColor", Color.BLACK);
        int templeColor = intent.getIntExtra("TempleColor", Color.BLACK);
        int templeTipColor = intent.getIntExtra("TempleTipColor", Color.BLACK);



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
        eyeglassesBundle.putInt("TempleColor", templeColor);
        eyeglassesBundle.putInt("TempleTipColor", templeTipColor);
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

        String senderRoom = "1" + prefManager.getUserUID();

        selectLensesButton = findViewById(R.id.selectLensesButton);
        selectLensesButton.setOnClickListener(v -> {

            Intent intentLenses = new Intent(GlassesActivity.this, ChatActivity.class);
            intentLenses.putExtra("Price", price);
            intentLenses.putExtra("Title", title);
            intentLenses.putExtra("Image", imageResId);
            intentLenses.putExtra("FrameType", frameType);
            intentLenses.putExtra("Type", type);
            intentLenses.putExtra("Price", price);
            intentLenses.putExtra("Function", functionValue);
            intentLenses.putExtra("Size", size);
            intentLenses.putExtra("Description", description);
            intentLenses.putExtra("Color", color);
            intentLenses.putExtra("LensesColor", lensesColor);
            intentLenses.putExtra("TempleColor", templeColor);
            intentLenses.putExtra("TempleTipColor", templeTipColor);
            intentLenses.putExtra("ID", ID);
            intentLenses.putExtra("IsDownloaded", isDownloaded);
            updateAllMessagesAsSeen(senderRoom, prefManager.getUserUID());
            startActivity(intentLenses);

        });

        selectLensesButton.setOnLongClickListener(v -> {
            PrefManager prefManager = new PrefManager(GlassesActivity.this);

            if(prefManager.isAdmin()) {

                adminChat(title, price, imageResId, frameType, type, ID, functionValue, size, description, color, lensesColor, templeColor, templeTipColor);

            } else {

                LayoutInflater inflater = getLayoutInflater();
                View emailPasswordView = inflater.inflate(R.layout.dialog_email_password, null);

                EditText emailEditText = emailPasswordView.findViewById(R.id.email_EditText);
                EditText passwordEditText = emailPasswordView.findViewById(R.id.password_EditText);

                AlertDialog.Builder builder = new AlertDialog.Builder(GlassesActivity.this, R.style.DarkDialogTheme);
                builder.setView(emailPasswordView)
                        .setTitle("Login")
                        .setPositiveButton("Login", (dialog, which) -> {
                            if(emailEditText.getText().toString().toLowerCase().trim().equals(prefManager.getEmail()) && passwordEditText.getText().toString().equals(prefManager.getPassword())) {
                                prefManager.setAdmin(true);

                                firstTimeSetupAnonymously(title, price, imageResId, frameType, type, ID, functionValue, size, description, color, lensesColor, templeColor, templeTipColor);

                            } else {
                                Toast.makeText(GlassesActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {

                        });

                AlertDialog dialog = builder.create();
                dialog.show();

            }

            return true;
        });

        getSupportFragmentManager().executePendingTransactions();
        new Handler().postDelayed(() -> initializeFunctionValueFromFragment(), 100);

        notification_count_TextView = findViewById(R.id.notification_count_TextView);

        DatabaseReference dbReferenceSender = FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("chats").child(senderRoom);

        dbReferenceSender.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<MessageModel> messages = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MessageModel message = dataSnapshot.getValue(MessageModel.class);
                    messages.add(message);
                }

                // Sort messages by timestamp
                Collections.sort(messages, new Comparator<MessageModel>() {
                    @Override
                    public int compare(MessageModel m1, MessageModel m2) {
                        return Long.compare(m1.getTimestamp(), m2.getTimestamp());
                    }
                });

                // Count unseen messages that are not from senderId
                unseenCount = countUnseenMessages(messages, prefManager.getUserUID());
                notification_count_TextView.setText(String.valueOf(unseenCount));
                if(unseenCount != 0) {
                    selectLensesButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFC107")));
                    notification_count_TextView.setVisibility(View.VISIBLE);
                } else {
                    selectLensesButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.gray_blue)));
                    notification_count_TextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    @Override
    public void onRadioButtonSelected(String text) {
        functionValue = text;
    }

    public void initializeFunctionValueFromFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof EyeglassesFragment) {
            String text = ((EyeglassesFragment) fragment).getCheckedRadioButtonText();
            if (!text.isEmpty()) {
                functionValue = text;
            }
        }
    }

    private void adminChat(String title, String price, int imageResId, String frameType, String type, String ID, String functionValue, float size, String description, int color, int lensesColor, int templeColor, int templeTipColor) {
        Intent intentLenses = new Intent(GlassesActivity.this, UserListActivity.class);
        intentLenses.putExtra("Price", price);
        intentLenses.putExtra("Title", title);
        intentLenses.putExtra("Image", imageResId);
        intentLenses.putExtra("FrameType", frameType);
        intentLenses.putExtra("Type", type);
        intentLenses.putExtra("Function", functionValue);
        intentLenses.putExtra("Size", size);
        intentLenses.putExtra("Description", description);
        intentLenses.putExtra("Color", color);
        intentLenses.putExtra("LensesColor", lensesColor);
        intentLenses.putExtra("TempleColor", templeColor);
        intentLenses.putExtra("TempleTipColor", templeTipColor);
        intentLenses.putExtra("ID", ID);
        startActivity(intentLenses);
    }

    private void updateAllMessagesAsSeen(String senderRoom, String currentUserId) {
        DatabaseReference dbReferenceSender = FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("chats").child(senderRoom);
        dbReferenceSender.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MessageModel message = dataSnapshot.getValue(MessageModel.class);
                    if (message != null && !message.getSenderId().equals(currentUserId)) {
                        String messageId = dataSnapshot.getKey();
                        dbReferenceSender.child(messageId).child("seen").setValue(true)
                                .addOnSuccessListener(aVoid -> {
                                    // Successfully updated message as seen
                                })
                                .addOnFailureListener(e -> {
                                    // Failed to update message
                                });
                        //FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("chats").child("" + 1)
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private int countUnseenMessages(List<MessageModel> messages, String senderId) {
        int count = 0;
        for (MessageModel message : messages) {
            if (!message.isSeen() && !message.getSenderId().equals(senderId)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void firstTimeSetupAnonymously(String title, String price, int imageResId, String frameType, String type, String ID, String functionValue, float size, String description, int color, int lensesColor, int templeColor, int templeTipColor) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        String adminId = "1";

        // Authenticate the user anonymously
        if(mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
                if(task.isSuccessful()) {
                    user = mAuth.getCurrentUser();
                    userId = user.getUid();
                    long signedInTimestamp = System.currentTimeMillis(); // Current time
                    long createdTimestamp = signedInTimestamp;

                    Log.d("chat", "Anonymous Auth Successful: " + userId);

                    UserModel userModel = new UserModel(userId, signedInTimestamp, createdTimestamp);
                    FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("users").child(userId).setValue(userModel)
                            .addOnSuccessListener(unused -> Log.d("Firebase", "User details saved successfully"));
                } else {
                    Log.d("chat", "Anonymous Auth Failed: " + task.getException().getMessage());
                }
            });

            prefManager.setUserUID(userId);

            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("Price", price);
            intent.putExtra("Title", title);
            intent.putExtra("Image", imageResId);
            intent.putExtra("FrameType", frameType);
            intent.putExtra("Type", type);
            intent.putExtra("Price", price);
            intent.putExtra("Function", functionValue);
            intent.putExtra("Size", size);
            intent.putExtra("Description", description);
            intent.putExtra("Color", color);
            intent.putExtra("LensesColor", lensesColor);
            intent.putExtra("TempleColor", templeColor);
            intent.putExtra("TempleTipColor", templeTipColor);
            intent.putExtra("ID", ID);
            startActivityForResult(intent, ChatActivity.FIRST_TIME_ADMIN);
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
}