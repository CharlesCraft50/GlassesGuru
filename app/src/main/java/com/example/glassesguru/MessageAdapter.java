package com.example.glassesguru;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {
    public static final int VIEW_TYPE_MESSAGE_SENT = 1;
    public static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private Context context;
    private List<MessageModel> messageModelList;
    private Activity activity;
    private boolean isAdmin = false;
    private GestureDetector gestureDetector;
    DatabaseReference dbReferenceSender, dbReferenceReceiver;
    PrefManager prefManager;
    private boolean allFilesExist = true;
    private ChatActivity chatActivityInstance;

    public MessageAdapter(Activity activity, Context context, PrefManager prefManager) {
        this.activity = activity;
        this.context = context;
        this.messageModelList = new ArrayList<>();
        this.prefManager = prefManager;

        // Update Liked column in Firebase
        String senderRoom = "1" + prefManager.getUserUID();
        String receiverRoom = prefManager.getUserUID() + "1";
        dbReferenceSender = FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("chats").child(senderRoom);
        dbReferenceReceiver = FirebaseDatabase.getInstance(PrefManager.FIREBASE_DATABASE_URL).getReference("chats").child(receiverRoom);

        if (activity instanceof ChatActivity) {
            chatActivityInstance = (ChatActivity) activity;
        }
    }

    public void add(MessageModel messageModel) {
        messageModelList.add(messageModel);
        notifyDataSetChanged();
    }

    public void clear() {
        messageModelList.clear();
        notifyDataSetChanged();
    }

    public void setAdmin() {
        isAdmin = true;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        View view = null;

        if(viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = layoutInflater.inflate(R.layout.item_message_sent, parent, false);
        } else if(viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = layoutInflater.inflate(R.layout.item_message_received, parent, false);
        }
        
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.MyViewHolder holder, int position) {
        MessageModel messageModel = messageModelList.get(position);
        String messageText = messageModel.getMessage();

        if (isAdmin) {
            // Admin perspective: Invert the logic
            if (!messageModel.getSenderId().equals("1")) {
                // Treat messages from the current user as received
                holder.received_message_TextView.setText(messageText);
            } else {
                // Treat messages from others as sent
                holder.sent_message_TextView.setText(messageText);
            }
        } else {
            // Regular user perspective
            if (messageModel.getSenderId().equals(FirebaseAuth.getInstance().getUid())) {
                holder.sent_message_TextView.setText(messageText);
            } else {
                holder.received_message_TextView.setText(messageText);
            }
        }

        setupChatFilters(holder, messageModel, messageText);
    }

    private void setupChatFilters(@NonNull MessageAdapter.MyViewHolder holder, MessageModel messageModel, String messageText) {
        if(messageText.length() > 0) {
            holder.message_layout.setVisibility(View.VISIBLE);
        } else {
            holder.message_layout.setVisibility(View.GONE);
        }

        if(messageModel.isLiked()) {
            holder.heart_ImageView.setVisibility(View.VISIBLE);
        } else {
            holder.heart_ImageView.setVisibility(View.GONE);
        }

        if(messageModel.getImageUrl() != null) {
            holder.image_message_ImageView.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(messageModel.getImageUrl())
                    .placeholder(R.drawable.loading_circle) // Placeholder while the image loads
                    .error(R.drawable.ic_error)             // Image displayed on error
                    .into(holder.image_message_ImageView);

            // Go to Photo fullscreen activity on click
            holder.image_message_ImageView.setOnClickListener(v -> {
                Intent intent = new Intent(context, PhotoFullscreenActivity.class);
                intent.putExtra("photoPathUrl", messageModel.getImageUrl());
                activity.startActivity(intent);
            });

        } else {
            holder.image_message_ImageView.setVisibility(View.GONE);
        }

        if(messageText.contains("<send details>")) {
            holder.glasses_details_layout.setVisibility(View.VISIBLE);

            holder.heart_ImageView.setVisibility(View.GONE);

            if(messageModel.isLiked()) {
                holder.heart_glasses_details_ImageView.setVisibility(View.VISIBLE);
            } else {
                holder.heart_glasses_details_ImageView.setVisibility(View.GONE);
            }

            Map<String, String> details = extractDetails(messageText);

            if (details != null) {
                // Set the details to the corresponding views
                int imageRes = Integer.parseInt(details.get("Image"));

                if(Boolean.parseBoolean(details.get("IsDownloaded"))) {
                    File imageFile = new File(context.getExternalFilesDir(null), "glasses_" + details.get("ID") + ".png");

                    if(imageFile.exists()) {
                        Glide.with(holder.glassesImageView.getContext())
                                .load(imageFile)
                                .placeholder(R.drawable.loading_circle) // Placeholder while the image loads
                                .error(R.drawable.ic_error)             // Image displayed on error
                                .into(holder.glassesImageView);

                        holder.glassesImageView.setAdjustViewBounds(true);
                    } else {
                        holder.glassesImageView.setImageResource(R.drawable.ic_download);
                        holder.download_more_glasses_Textview.setVisibility(View.VISIBLE);

                        holder.glasses_image_layout.setOnClickListener(v -> {
                            Toast.makeText(context, "Fetching available models...", Toast.LENGTH_SHORT).show();
                            fetchAndDownloadGlassesModels();
                            prefManager.setAllFilesDownloaded(true);
                        });
                    }
                } else {
                    holder.glassesImageView.setImageResource(imageRes);
                }

                holder.title_TextView.setText(details.get("Title"));
                holder.frameType_TextView.setText(details.get("FrameType"));
                holder.price_TextView.setText(details.get("Price"));

                holder.glassesFrameColorCard.setBackgroundColor(Color.parseColor(showColor(Integer.parseInt(details.get("FrameColor")))));

                int lensesColor = Integer.parseInt(details.get("LensesColor"));
                int templeColor = Integer.parseInt(details.get("TempleColor"));
                int templeTipColor = Integer.parseInt(details.get("TempleTip"));

                // Check if the LensesColor exists
                if (lensesColor != 0x00000000) {
                    holder.glassesLensesColorCard.setVisibility(View.VISIBLE);
                    holder.glassesLensesColorCard.setCardBackgroundColor(lensesColor);
                } else {
                    holder.glassesLensesColorCard.setVisibility(View.GONE);
                }

                // Check if the TempleColor exists
                if (templeColor != 0x00000000) {
                    holder.glassesTempleColorCard.setVisibility(View.VISIBLE);
                    holder.glassesTempleColorCard.setBackgroundColor(templeColor);
                } else {
                    holder.glassesTempleColorCard.setVisibility(View.GONE);
                }

                // Check if the TempleTipColor exists
                if (templeTipColor != 0x00000000) {
                    holder.glassesTempleTipColorCard.setVisibility(View.VISIBLE);
                    holder.glassesTempleTipColorCard.setBackgroundColor(templeTipColor);
                } else {
                    holder.glassesTempleTipColorCard.setVisibility(View.GONE);
                }

                holder.function_TextView.setText(details.get("Function"));
                holder.size_TextView.setText(details.get("Size"));

                if(holder.received_message_TextView != null) {
                    holder.received_message_TextView.setText("Details:");
                }

                if(holder.sent_message_TextView != null) {
                    holder.sent_message_TextView.setText("Details:");
                }

                holder.change_function_Button.setOnClickListener(v -> {
                    String send_details_message = "<send details>\n" +
                            "Image: " + details.get("Image") + "\n" +
                            "Title: " + details.get("Title") + "\n" +
                            "FrameType: " + details.get("FrameType") + "\n" +
                            "Price: " + details.get("Price") + "\n" +
                            "FrameColor: " + details.get("FrameColor") + "\n" +
                            "LensesColor: " + details.get("LensesColor") + "\n" +
                            "TempleColor: " + details.get("TempleColor") + "\n" +
                            "TempleTip: " + details.get("TempleTip") + "\n" +
                            "Function: \n" +
                            "Size: " + details.get("Size") + "\n" +
                            "ID: " + details.get("ID") + "\n" +
                            "IsDownloaded: " + details.get("IsDownloaded") + "\n";
                    chatActivityInstance.showFunctioChangeDialog(getRadioButtonId(details.get("Function")), send_details_message);
                });
            }
        } else {
            holder.glasses_details_layout.setVisibility(View.GONE);
        }

        if(messageText.contains("<appointment>")) {
            holder.booking_details_layout.setVisibility(View.VISIBLE);

            holder.heart_ImageView.setVisibility(View.GONE);

            if(messageModel.isLiked()) {
                holder.heart_booking_details_ImageView.setVisibility(View.VISIBLE);
            } else {
                holder.heart_booking_details_ImageView.setVisibility(View.GONE);
            }

            Map<String, String> details = extractDetails(messageText);

            if (details != null) {

                holder.name_value_TextView.setText(details.get("Name"));
                holder.date_value_TextView.setText(details.get("Date"));
                holder.time_value_TextView.setText(details.get("Time"));
                holder.reason_value_TextView.setText(details.get("Reason"));

                if(holder.received_message_TextView != null) {
                    holder.received_message_TextView.setText("Appointment Details:");
                }

                if(holder.sent_message_TextView != null) {
                    holder.sent_message_TextView.setText("Appointment Details:");
                }
            }
        } else {
            holder.booking_details_layout.setVisibility(View.GONE);
        }

        if (messageText.contains("<liked-message>")) {
            holder.message_layout.setVisibility(View.GONE);
        }
    }

    public int getRadioButtonId(String functionName) {
        switch (functionName) {
            case "Clear":
                return R.id.rbClear;
            case "Eyeglasses":
                return R.id.rbClear;
            case "Blue Block":
                return R.id.rbBlueBlock;
            case "Sunglasses":
                return R.id.rbSunglasses;
            case "Photochromic":
                return R.id.rbPhotochromic;
            case "Blue Block Pro":
                return R.id.rbBlueBlockPro;
            default:
                return -1;
        }
    }

    private String showColor(int color) {
        float[] customColor = new float[]{0f, 0f, 0f, 1f};

        if (color != 0x00000000) {
            customColor = new float[]{Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f, Color.alpha(color) / 255f};
        }

        String hexColor = String.format("#%02X%02X%02X%02X",
                (int) (customColor[3] * 255),
                (int) (customColor[0] * 255),
                (int) (customColor[1] * 255),
                (int) (customColor[2] * 255));

        return hexColor;
    }

    private static Map<String, String> extractDetails(String input) {
        Map<String, String> detailsMap = new HashMap<>();
        String regex = "(Image|Title|FrameType|Price|FrameColor|LensesColor|TempleColor|TempleTip|Function|Size|Name|Date|Time|Reason|ID|IsDownloaded):\\s*(.*)";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            detailsMap.put(key, value);
        }

        return detailsMap;
    }

    private void fetchAndDownloadGlassesModels() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("models/glasses");

        storageRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                ArrayList<StorageReference> fileRefs = new ArrayList<>(listResult.getItems());
                String lastGlassesId = "";

                if (fileRefs.isEmpty()) {
                    // No files to download, proceed
                    completeActivityTransition();
                    return;
                }

                for (StorageReference fileRef : fileRefs) {
                    allFilesExist = false; // Mark as false as we're processing files
                    downloadFile(fileRef, fileRefs.size());
                    if (fileRef.getName().startsWith("glasses_")) {
                        lastGlassesId = fileRef.getName().split("_")[1];
                    }
                }

                if (lastGlassesId != null && lastGlassesId.matches("\\d+")) {
                    prefManager.setGlassesCountDownloaded(Integer.parseInt(lastGlassesId));
                } else {
                    Log.e("GlassesCountError", "Invalid glasses ID: " + lastGlassesId);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Failed to fetch files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadFile(StorageReference fileRef, int totalFiles) {
        File localFile = new File(context.getExternalFilesDir(null), fileRef.getName());
        localFile.getParentFile().mkdirs();

        if (localFile.exists() && !fileRef.getName().equals("data.json")) {
            checkAllFilesDownloaded(totalFiles);  // Proceed to check if all files are done
            return;
        }

        fileRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(context, "Downloaded: " + localFile.getName(), Toast.LENGTH_SHORT).show();
                checkAllFilesDownloaded(totalFiles);  // Check after each download completes
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(context, "Download failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int filesDownloaded = 0;

    private void checkAllFilesDownloaded(int totalFiles) {
        filesDownloaded++;
        if (filesDownloaded == totalFiles) {
            allFilesExist = true;
            completeActivityTransition();
        }
    }

    private void completeActivityTransition() {
        if (allFilesExist) {
            Toast.makeText(activity, "All files up to date!", Toast.LENGTH_SHORT).show();
        }
        activity.recreate();
    }

    @Override
    public int getItemCount() {
        return messageModelList.size();
    }

    public List<MessageModel> getMessageModelList()
    {
        return messageModelList;
    }

    public int getLastReceivedMessagePosition() {
        // Iterate through the messageModelList in reverse order to find the last received message
        for (int i = messageModelList.size() - 1; i >= 0; i--) {
            if (getItemViewType(i) == VIEW_TYPE_MESSAGE_RECEIVED) {
                return i; // Return the position of the last received message
            }
        }
        return -1; // Return -1 if no received message is found
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel messageModel = messageModelList.get(position);

        // Check if the current user is an admin
        if (isAdmin) {
            // If the user is acting as an admin, consider all messages with the current user ID as received
            if (!messageModel.getSenderId().equals("1")) {
                return VIEW_TYPE_MESSAGE_RECEIVED;
            } else {
                return VIEW_TYPE_MESSAGE_SENT;
            }
        } else {
            // Regular user perspective
            if (messageModel.getSenderId().equals(FirebaseAuth.getInstance().getUid())) {
                return VIEW_TYPE_MESSAGE_SENT;
            } else {
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout message_layout;
        private TextView sent_message_TextView, received_message_TextView;
        private ImageView image_message_ImageView, heart_ImageView, heart_glasses_details_ImageView, heart_booking_details_ImageView;

        // Glasses Details:
        private LinearLayout glasses_details_layout, booking_details_layout;
        private TextView name_value_TextView, date_value_TextView, time_value_TextView, reason_value_TextView;
        private ImageView glassesImageView;
        private TextView title_TextView, frameType_TextView, price_TextView, function_TextView, size_TextView;
        private CardView glassesFrameColorCard, glassesLensesColorCard, glassesTempleColorCard, glassesTempleTipColorCard;
        private TextView download_more_glasses_Textview;
        private LinearLayout glasses_image_layout;
        private Button change_function_Button;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            message_layout = itemView.findViewById(R.id.message_layout);
            sent_message_TextView = itemView.findViewById(R.id.sent_message_TextView);
            received_message_TextView = itemView.findViewById(R.id.received_message_TextView);
            image_message_ImageView = itemView.findViewById(R.id.image_message_ImageView);
            heart_ImageView = itemView.findViewById(R.id.heart_ImageView);
            heart_glasses_details_ImageView = itemView.findViewById(R.id.heart_glasses_details_ImageView);
            heart_booking_details_ImageView = itemView.findViewById(R.id.heart_booking_details_ImageView);

            // Glasses Details

            glasses_details_layout = itemView.findViewById(R.id.glasses_details_layout);
            glasses_image_layout = itemView.findViewById(R.id.glasses_image_layout);
            glassesImageView = itemView.findViewById(R.id.glassesImageView);
            download_more_glasses_Textview = itemView.findViewById(R.id.download_more_glasses_Textview);
            title_TextView = itemView.findViewById(R.id.nonPrescriptionLabel);
            frameType_TextView = itemView.findViewById(R.id.nonPrecriptionDescription);
            price_TextView = itemView.findViewById(R.id.glassesPrice);
            glassesFrameColorCard = itemView.findViewById(R.id.glassesFrameColorCard);
            glassesLensesColorCard = itemView.findViewById(R.id.glassesLensesColorCard);
            glassesTempleColorCard = itemView.findViewById(R.id.glassesTempleColorCard);
            glassesTempleTipColorCard = itemView.findViewById(R.id.glassesTempleTipColorCard);
            function_TextView = itemView.findViewById(R.id.glassesFunction);
            change_function_Button = itemView.findViewById(R.id.change_function_Button);
            size_TextView = itemView.findViewById(R.id.glassesSize);

            // Booking Details
            booking_details_layout = itemView.findViewById(R.id.booking_details_layout);
            name_value_TextView = itemView.findViewById(R.id.name_value_TextView);
            date_value_TextView = itemView.findViewById(R.id.date_value_TextView);
            time_value_TextView = itemView.findViewById(R.id.time_value_TextView);
            reason_value_TextView = itemView.findViewById(R.id.reason_value_TextView);

            message_layout.setOnTouchListener(new View.OnTouchListener() {
                private GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if(getItemViewType() == VIEW_TYPE_MESSAGE_RECEIVED) {
                            toggleLike(messageModelList.get(getPosition()));
                        }
                        return super.onDoubleTap(e);
                    }
                });

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d("TEST", "Raw event: " + event.getAction() + ", (" + event.getRawX() + ", " + event.getRawY() + ")");
                    gestureDetector.onTouchEvent(event);
                    return true;
                }});

        }

        private void toggleLike(MessageModel messageModel) {
            if (messageModel != null) {
                boolean newLikedStatus = !messageModel.isLiked();
                messageModel.setLiked(newLikedStatus);
                notifyItemChanged(getPosition());

                // Update the `isLiked` field in Firebase
                String messageId = messageModel.getMessageId();

                dbReferenceSender.child(messageId).child("liked").setValue(newLikedStatus);
                dbReferenceReceiver.child(messageId).child("liked").setValue(newLikedStatus);

                String messageIdRandom = UUID.randomUUID().toString();
                long timestamp = System.currentTimeMillis();

                if(messageModel.isLiked()) {
                    String senderId = (isAdmin) ? "1" : prefManager.getUserUID();
                    MessageModel messageModel1 = new MessageModel(messageIdRandom, senderId, senderId + " liked your message. <liked-message>", timestamp, null);
                    dbReferenceSender.child("temporaryLike").setValue(messageModel1);
                    dbReferenceReceiver.child("temporaryLike").setValue(messageModel1);
                }
            }
        }
    }


}
