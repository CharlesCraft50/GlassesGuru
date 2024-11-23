package com.example.glassesguru;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.content.res.Resources;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {
    String receiverId, senderRoom, receiverRoom;
    String receiverName = "GlassesGuru Support";
    String senderId, senderName;
    String userId;
    DatabaseReference dbReferenceSender, dbReferenceReceiver, userReference;
    ImageView send_message_ImageView, select_image_ImageView;
    EditText message_EditText;
    RecyclerView chat_RecyclerView;
    MessageAdapter messageAdapter;
    FirebaseUser user;
    String send_details_message;
    private static final int PICK_IMAGE_REQUEST = 1;
    public static final int FIRST_TIME_ADMIN = 1001;
    Uri selectedImageUri;
    ConstraintLayout selected_image_layout, send_message_layout;
    Intent intentData;
    PrefManager prefManager;
    private String nameValue, otherReasonValue;
    CoordinatorLayout mainLayout;
    private boolean isInitialAdjustment = true;
    FirebaseAuth mAuth;
    String adminId = "1";
    String messageApointment;
    String firstMessage;
    ConstraintLayout welcome_message_container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        send_message_layout = findViewById(R.id.send_message_layout);
        mainLayout = findViewById(R.id.main);

        // Add a keyboard visibility listener
        KeyboardVisibilityEvent.setEventListener(
                this,
                isVisible -> {
                    // Adjust the send message layout position
                    adjustSendMessageLayout(isVisible);
                }
        );
        // Observe the root view's layout changes
        mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                adjustSendMessageLayout(KeyboardVisibilityEvent.isKeyboardVisible(ChatActivity.this));
            }
        });

        prefManager = new PrefManager(this);

        send_message_ImageView = findViewById(R.id.send_message_ImageView);
        select_image_ImageView = findViewById(R.id.select_image_ImageView);
        messageAdapter = new MessageAdapter(ChatActivity.this, this, prefManager);
        chat_RecyclerView = findViewById(R.id.chat_RecyclerView);
        message_EditText = findViewById(R.id.message_EditText);
        welcome_message_container = findViewById(R.id.welcome_message_container);

        selected_image_layout = findViewById(R.id.selected_image_layout);

        chat_RecyclerView.setAdapter(messageAdapter);
        chat_RecyclerView.setLayoutManager(new LinearLayoutManager(this));

        intentData = getIntent();

        Button send_details_Button = findViewById(R.id.send_details_Button);
        Button appointment_Button = findViewById(R.id.appointment_Button);

        send_details_message = "<send details>\n" +
                "Image: " + intentData.getIntExtra("Image", -1) + "\n" +
                "Title: " + intentData.getStringExtra("Title") + "\n" +
                "FrameType: " + intentData.getStringExtra("FrameType") + "\n" +
                "Price: " + intentData.getStringExtra("Price") + "\n" +
                "FrameColor: " + intentData.getIntExtra("Color", Color.BLACK) + "\n" +
                "LensesColor: " + intentData.getIntExtra("LensesColor", Color.BLACK) + "\n" +
                "TempleColor: " + intentData.getIntExtra("TempleColor", Color.BLACK) + "\n" +
                "TempleTip: " + intentData.getIntExtra("TempleTipColor", Color.BLACK) + "\n" +
                "Function: " + intentData.getStringExtra("Function") + "\n" +
                "Size: " + intentData.getFloatExtra("Size", 1.0f) + "\n" +
                "ID: " + intentData.getStringExtra("ID") + "\n" +
                "IsDownloaded: " + intentData.getBooleanExtra("IsDownloaded", false) + "\n";

        if(prefManager.isNewUser() && !prefManager.isAdmin()) {
            welcome_message_container.setVisibility(View.VISIBLE);

            select_image_ImageView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            });

            send_message_ImageView.setOnClickListener(v -> {
                if(message_EditText.getText().toString().length() > 0) {
                    firstMessage = message_EditText.getText().toString();
                    anonymousSignIn(0);
                } else {
                    if(selectedImageUri != null) {
                        anonymousSignIn(-1);
                        InvokeSendMessage();
                    } else {
                        Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            send_details_Button.setOnClickListener(v -> {
                anonymousSignIn(1);
            });

            appointment_Button.setOnClickListener(v -> {
                showApointmentDialog(true);
            });
        } else {

            send_details_Button.setOnClickListener(v -> {
                SendMessage(send_details_message, null);
            });

            appointment_Button.setOnClickListener(v -> {
                showApointmentDialog(false);
            });

            mAuth = FirebaseAuth.getInstance();

            // Authenticate the user anonymously

            if(mAuth.getCurrentUser() == null) {
                mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
                    if(task.isSuccessful()) {
                        user = mAuth.getCurrentUser();
                        userId = user.getUid();
                        long signedInTimestamp = System.currentTimeMillis(); // Current time
                        long createdTimestamp = signedInTimestamp;

                        Log.d("chat", "Anonymous Auth Successful: " + userId);



                        if(intentData.getStringExtra("UserUID") != null) {
                            setupChat("1", intentData.getStringExtra("UserUID"));
                            messageAdapter.setAdmin();
                        } else {
                            setupChat(userId, adminId);
                        }

                        UserModel userModel = new UserModel(userId, signedInTimestamp, createdTimestamp);
                        FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("users").child(userId).setValue(userModel)
                                .addOnSuccessListener(unused -> Log.d("Firebase", "User details saved successfully"));
                    } else {
                        Log.d("chat", "Anonymous Auth Failed: " + task.getException().getMessage());
                    }
                });
            } else {
                user = mAuth.getCurrentUser();
                userId = user.getUid();
                long signedInTimestamp = System.currentTimeMillis();

                Log.d("chat", "User Auth Successful: " + userId);

                if(intentData.getStringExtra("UserUID") != null) {
                    setupChat("1", intentData.getStringExtra("UserUID"));
                    messageAdapter.setAdmin();
                } else {
                    setupChat(userId, adminId);
                }

                // Update the signedInTimestamp
                DatabaseReference userRef = FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("users").child(userId);
                userRef.child("signedInTimestamp").setValue(signedInTimestamp)
                        .addOnSuccessListener(unused -> Log.d("Firebase", "Signed In timestamp updated successfully"))
                        .addOnFailureListener(e -> Log.e("Firebase", "Failed to update Signed In timestamp: " + e.getMessage()));
            }

            prefManager.setUserUID(userId);
        }

        if(prefManager.isFirstTermsAndConditions() && !prefManager.isAdmin()) {
            showTermsAndConditionsDialog();
        }

    }

    private boolean isSigningIn = false; // Add this flag to prevent multiple sign-in attempts

    private void anonymousSignIn(int type) {
        mAuth = FirebaseAuth.getInstance();

        if (isSigningIn) {
            return; // Prevent further execution if already signing in
        }

        isSigningIn = true; // Set the flag to true as we are starting the sign-in process

        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
                isSigningIn = false; // Reset the flag once sign-in is complete

                if (task.isSuccessful()) {
                    // Handle successful sign-in
                    user = mAuth.getCurrentUser();
                    userId = user.getUid();
                    long signedInTimestamp = System.currentTimeMillis();
                    long createdTimestamp = signedInTimestamp;

                    Log.d("chat", "Anonymous Auth Successful: " + userId);

                    if (intentData.getStringExtra("UserUID") != null) {
                        setupChat("1", intentData.getStringExtra("UserUID"));
                        messageAdapter.setAdmin();
                    } else {
                        setupChat(userId, adminId);
                    }

                    UserModel userModel = new UserModel(userId, signedInTimestamp, createdTimestamp);
                    FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("users").child(userId).setValue(userModel)
                            .addOnSuccessListener(unused -> Log.d("Firebase", "User details saved successfully"));

                    // Recreate the activity after signing in
                    recreate();

                    if (type == 0) {
                        SendMessage(firstMessage, null);
                    } else if (type == 1) {
                        SendMessage(send_details_message, null);
                    } else if (type == 2) {
                        SendMessage(messageApointment, null);
                    }

                } else {
                    Log.d("chat", "Anonymous Auth Failed: " + task.getException().getMessage());
                }
            });
        }

        prefManager.setNewUser(false);
        prefManager.setUserUID(userId);
    }

    private void showApointmentDialog(boolean isNewUser) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_calendar, null);

        DatePicker date_picker = dialogView.findViewById(R.id.date_picker);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setTitle("Select Date")
                .setPositiveButton("Next", (dialog, which) -> {
                    int day = date_picker.getDayOfMonth();
                    int month = date_picker.getMonth();
                    int year = date_picker.getYear();
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(year, month, day);
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                    String selectedDate = sdf.format(calendar.getTime());

                    showTimePickerDialog(selectedDate, isNewUser);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {

                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showTimePickerDialog(String selectedDate, boolean isNewUser) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_time_picker, null);

        TimePicker time_picker = dialogView.findViewById(R.id.timePicker);
        time_picker.setIs24HourView(false); // Set to 12-hour format

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setTitle("Select Time")
                .setPositiveButton("Next", null)  // Set null to override later
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Handle cancel action if needed
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the default onClick behavior of the PositiveButton
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int hourOfDay = time_picker.getHour();
            int minute = time_picker.getMinute();

            // Convert hour and minute to a time string with AM/PM
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault()); // Format for AM/PM
            String selectedTime = sdf.format(calendar.getTime());

            // Show the next dialog
            showNameDialog(selectedDate, selectedTime, isNewUser);
            dialog.dismiss();
        });
    }

    private void showNameDialog(String selectedDate, String selectedTime, boolean isNewUser) {
        View enterNameView = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
        TextView textView = enterNameView.findViewById(R.id.text_view_TextView);
        textView.setText("");
        EditText editText = enterNameView.findViewById(R.id.edit_text_EditText);
        editText.setHint("Enter Name");

        AlertDialog.Builder nameDialogBuilder = new AlertDialog.Builder(this);
        nameDialogBuilder.setView(enterNameView)
                .setTitle("Enter Name")
                .setPositiveButton("Next", null)  // Set null to override later
                .setNegativeButton("Cancel", (dialog1, which1) -> {
                    // Handle cancel action if needed
                });

        AlertDialog nameDialog = nameDialogBuilder.create();
        nameDialog.show();

        // Override the default onClick behavior of the PositiveButton
        nameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            nameValue = editText.getText().toString();

            if (nameValue.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            } else {
                showReasonDialog(selectedDate, selectedTime, nameValue, isNewUser);
                nameDialog.dismiss();
            }
        });
    }

    private void showReasonDialog(String selectedDate, String selectedTime, String nameValue, boolean isNewUser) {
        View reasonView = getLayoutInflater().inflate(R.layout.dialog_reason, null);
        EditText otherReasonEditText = reasonView.findViewById(R.id.other_reason_EditText);
        Button eye_exam_button = reasonView.findViewById(R.id.eye_exam_button);
        Button consultation_button = reasonView.findViewById(R.id.consultation_button);

        AlertDialog.Builder reasonDialogBuilder = new AlertDialog.Builder(this);
        reasonDialogBuilder.setView(reasonView)
                .setTitle("Reason for Appointment")
                .setPositiveButton("Submit", null)  // Set null to override later
                .setNegativeButton("Cancel", (dialog1, which1) -> {
                    // Handle cancel action if needed
                });

        AlertDialog reasonDialog = reasonDialogBuilder.create();
        reasonDialog.show();

        eye_exam_button.setOnClickListener(v -> {
            if(isNewUser) {
                otherReasonValue = "Eye Exam";
                messageApointment = "<appointment>" + "\n" +
                        "Name: " + nameValue + "\n" +
                        "Date: " + selectedDate + "\n" +
                        "Time: " + selectedTime + "\n" +
                        "Reason: " + otherReasonValue;
                recreate();
                anonymousSignIn(2);
            } else {
                otherReasonValue = "Eye Exam";
                String message = "<appointment>" + "\n" +
                        "Name: " + nameValue + "\n" +
                        "Date: " + selectedDate + "\n" +
                        "Time: " + selectedTime + "\n" +
                        "Reason: " + otherReasonValue;
                SendMessage(message, null);
            }


            reasonDialog.dismiss();
        });

        consultation_button.setOnClickListener(v -> {
            if(isNewUser) {
                otherReasonValue = "Consultation";
                messageApointment = "<appointment>" + "\n" +
                        "Name: " + nameValue + "\n" +
                        "Date: " + selectedDate + "\n" +
                        "Time: " + selectedTime + "\n" +
                        "Reason: " + otherReasonValue;
                recreate();
                anonymousSignIn(2);
            } else {
                otherReasonValue = "Consultation";
                String message = "<appointment>" + "\n" +
                        "Name: " + nameValue + "\n" +
                        "Date: " + selectedDate + "\n" +
                        "Time: " + selectedTime + "\n" +
                        "Reason: " + otherReasonValue;
                SendMessage(message, null);
            }

            reasonDialog.dismiss();
        });

        // Override the default onClick behavior of the PositiveButton
        reasonDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            otherReasonValue = otherReasonEditText.getText().toString();

            if (otherReasonValue.isEmpty()) {
                Toast.makeText(this, "Please enter a reason or select an option", Toast.LENGTH_SHORT).show();
            } else {
                if(isNewUser) {
                    messageApointment = "<appointment>" + "\n" +
                            "Name: " + nameValue + "\n" +
                            "Date: " + selectedDate + "\n" +
                            "Time: " + selectedTime + "\n" +
                            "Reason: " + otherReasonValue;
                    recreate();
                    anonymousSignIn(2);
                } else {
                    String message = "<appointment>" + "\n" +
                            "Name: " + nameValue + "\n" +
                            "Date: " + selectedDate + "\n" +
                            "Time: " + selectedTime + "\n" +
                            "Reason: " + otherReasonValue;
                    SendMessage(message, null);
                }
                reasonDialog.dismiss();
            }
        });
    }

    private void showTermsAndConditionsDialog() {
        View termsAndConditionsView = getLayoutInflater().inflate(R.layout.dialog_terms_and_conditions, null);
        TextView termsAndConditionsEditText = termsAndConditionsView.findViewById(R.id.tv_terms_conditions);
        termsAndConditionsEditText.setText(Html.fromHtml(getString(R.string.terms_and_conditions), Html.FROM_HTML_MODE_LEGACY));

        AlertDialog.Builder termsAndConditionsDialogBuilder = new AlertDialog.Builder(this);
        termsAndConditionsDialogBuilder.setView(termsAndConditionsView)
                .setPositiveButton("Agree", (dialog, which) -> {
                    prefManager.setFirstTermsAndConditions(false);
                    dialog.dismiss();
                })
                .setNegativeButton("Disagree", (dialog, which) -> {
                    prefManager.setFirstTermsAndConditions(true);
                    finish();
                });

        AlertDialog termsAndConditionsDialog = termsAndConditionsDialogBuilder.create();
        termsAndConditionsDialog.show();
    }

    private void setupChat(String userId, String adminId) {
        // Create unique chat rooms using the userId and adminId
        senderRoom = userId + adminId;
        receiverRoom = adminId + userId;

        Log.d("ChatActivity", "senderRoom: " + senderRoom);
        Log.d("ChatActivity", "receiverRoom: " + receiverRoom);

        // Set up Firebase references for sender and receiver rooms
        dbReferenceSender = FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("chats").child(senderRoom);
        dbReferenceReceiver = FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("chats").child(receiverRoom);

        if (dbReferenceSender != null && dbReferenceReceiver != null) {
            Log.d("Firebase", "Database references are valid");
        } else {
            Log.e("Firebase", "Database references are null");
        }

        // Set the title to the admin's name
        if(adminId.equals("1")) {
            getSupportActionBar().setTitle(receiverName);
        } else {
            getSupportActionBar().setTitle(adminId);
        }

        // Listen for incoming messages
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

                messageAdapter.clear();

                for (MessageModel message : messages) {
                    messageAdapter.add(message);
                }

                messageAdapter.notifyDataSetChanged();

                scrollToBottom();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });

        // Select Image
        select_image_ImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // Handle sending messages
        send_message_ImageView.setOnClickListener(v -> {
            InvokeSendMessage();
        });
    }

    private void InvokeSendMessage() {
        String message = message_EditText.getText().toString();
        if(selectedImageUri != null) {
            send_message_ImageView.setClickable(false);
            send_message_ImageView.setFocusable(false);
            select_image_ImageView.setClickable(false);
            select_image_ImageView.setFocusable(false);
            ImageView loading_ImageView = findViewById(R.id.loading_ImageView);
            loading_ImageView.setVisibility(View.VISIBLE);
            loading_ImageView.setImageResource(R.drawable.loading_circle);
            String pathString = "images/" + UUID.randomUUID().toString();

            // Upload the selected image to Firebase Storage
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(pathString);
            storageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        selectedImageUri = null;
                        // Get the download URL
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            send_message_ImageView.setClickable(true);
                            send_message_ImageView.setFocusable(true);
                            select_image_ImageView.setClickable(true);
                            select_image_ImageView.setFocusable(true);
                            loading_ImageView.setVisibility(View.GONE);
                            selected_image_layout.setVisibility(View.GONE);
                            if(message.length() > 0) {
                                SendMessage(message, downloadUrl); // Pass the download URL instead of the path
                            } else {
                                SendMessage("", downloadUrl);
                            }
                        }).addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } else {
            if(message.length() > 0) {
                SendMessage(message, null);
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void SendMessage(String message, String imageUrl) {
        String messageId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        MessageModel messageModel = null;

        if(imageUrl != null) {
            messageModel = new MessageModel(messageId, (intentData.getStringExtra("UserUID") != null) ? "1" : FirebaseAuth.getInstance().getUid(), message, timestamp, imageUrl);
        } else {
            messageModel = new MessageModel(messageId, (intentData.getStringExtra("UserUID") != null) ? "1" : FirebaseAuth.getInstance().getUid(), message, timestamp, null);
        }

        messageAdapter.add(messageModel);

        try {
            dbReferenceSender.child(messageId).setValue(messageModel)
                    .addOnSuccessListener(unused -> Log.d("Firebase", "Message saved successfully to sender room"))
                    .addOnFailureListener(e -> Log.e("Firebase", "Failed to save message: " + e.getMessage()));

            dbReferenceReceiver.child(messageId).setValue(messageModel)
                    .addOnSuccessListener(unused -> Log.d("Firebase", "Message saved successfully to receiver room"))
                    .addOnFailureListener(e -> Log.e("Firebase", "Failed to save message to receiver room: " + e.getMessage()));
        } catch (Exception e) {
            Log.e("Firebase", "Exception caught: " + e.getMessage());
        }

        scrollToBottom();
        message_EditText.setText("");
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
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            // Display the selected image above the EditText
            ImageView selected_ImageView = findViewById(R.id.selected_ImageView);
            selected_ImageView.setImageURI(imageUri);

            // Make the ImageView visible and add a cancel button
            selected_image_layout.setVisibility(View.VISIBLE);

            // Store the selected image URI
            this.selectedImageUri = imageUri;
        } else if(requestCode == FIRST_TIME_ADMIN) {
            finish();
            Intent intent = new Intent(this, UserListActivity.class);
            intent.putExtra("Price", intentData.getStringExtra("Price"));
            intent.putExtra("Title", intentData.getStringExtra("Title"));
            intent.putExtra("Image", intentData.getIntExtra("Image", -1));
            intent.putExtra("FrameType", intentData.getStringExtra("FrameType"));
            intent.putExtra("Type", intentData.getStringExtra("Type"));
            intent.putExtra("Function", intentData.getStringExtra("Function"));
            intent.putExtra("Size", intentData.getFloatExtra("Size", 1.0f));
            intent.putExtra("Description", intentData.getStringExtra("Description"));
            intent.putExtra("Color", intentData.getIntExtra("Color", Color.BLACK));
            intent.putExtra("LensesColor", intentData.getIntExtra("LensesColor", Color.BLACK));
            intent.putExtra("TempleColor", intentData.getIntExtra("TempleColor", Color.BLACK));
            intent.putExtra("TempleTipColor", intentData.getIntExtra("TempleTipColor", Color.BLACK));
            intent.putExtra("ID", intentData.getStringExtra("ID"));
            startActivity(intent);
        }
    }

    public void scrollToBottom() {
        chat_RecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    public void onCancelImageClick(View view) {
        selected_image_layout.setVisibility(View.GONE);

        // Clear the selected image URI
        this.selectedImageUri = null;
    }

    private void adjustSendMessageLayout(boolean isKeyboardVisible) {
        Rect rect = new Rect();
        mainLayout.getWindowVisibleDisplayFrame(rect);

        int rootViewHeight = rect.height();
        int sendMessageLayoutBottom = send_message_layout.getBottom();

        if (isKeyboardVisible && sendMessageLayoutBottom > rootViewHeight) {
            int extraHeight = sendMessageLayoutBottom - rootViewHeight;
            send_message_layout.setPadding(0, 0, 0, extraHeight);


            if (isInitialAdjustment) {
                scrollToBottom();
                isInitialAdjustment = false;
            }
        } else {
            send_message_layout.setPadding(0, 0, 0, 0);
            isInitialAdjustment = true;
        }
    }

    private int getNavigationBarHeight() {
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public void showFunctioChangeDialog(int functionValue, String send_details_message) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_function_change, null);
        RadioGroup rgFunctionOptions = dialogView.findViewById(R.id.rgFunctionOptions);
        rgFunctionOptions.check(functionValue);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setTitle("Change function")
                .setPositiveButton("CHANGE", (dialog, which) -> {
                    int selectedId = rgFunctionOptions.getCheckedRadioButtonId();
                    if (selectedId != -1) {
                        RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
                        String selectedText = selectedRadioButton.getText().toString();

                        SendMessage(send_details_message.replace("Function: \n", "Function: " + selectedText.replace("Clear", "Eyeglasses") + "\n"), null);

                        dialog.dismiss();
                    } else {
                        Toast.makeText(this, "No option selected", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}