    package com.example.glassesguru;

    import android.app.Activity;
    import android.content.Context;
    import android.content.Intent;
    import android.content.res.ColorStateList;
    import android.graphics.Color;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.LinearLayout;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.cardview.widget.CardView;
    import androidx.constraintlayout.widget.ConstraintLayout;
    import androidx.recyclerview.widget.RecyclerView;

    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;

    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.Comparator;
    import java.util.Date;
    import java.util.List;
    import java.util.Locale;

    public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private Context context;
        private List<UserModel> userList;
        private Activity activity;
        private Intent intentData;
        private int unseenCount = 0;

        public UserAdapter(Activity activity, Context context, List<UserModel> userList, Intent intentData) {
            this.activity = activity;
            this.context = context;
            this.userList = userList;
            this.intentData = intentData;
        }

        public void updateUserList(List<UserModel> newList) {
            userList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            UserModel userModel = userList.get(position);

            holder.uidTextView.setText("User UID: " + userModel.getUid());
            holder.signedInTextView.setText("Signed In: " + new Date(userModel.getSignedInTimestamp()).toString());
            holder.createdTextView.setText("Created: " + new Date(userModel.getCreatedTimestamp()).toString());
            String senderRoom = userModel.getUid() + "1";

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

                    // Count unseen messages that are not from senderId "1"
                    unseenCount = countUnseenMessages(messages, "1");
                    holder.notification_count_TextView.setText(String.valueOf(unseenCount));

                    if(unseenCount == 0) {
                        holder.notification_count_TextView.setVisibility(View.GONE);
                        holder.user_Layout.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                    } else {
                        holder.notification_count_TextView.setVisibility(View.VISIBLE);
                        holder.user_Layout.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFC107")));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            });

            holder.notification_count_TextView.setText(String.valueOf(unseenCount));

            holder.user_Layout.setOnClickListener(v -> {
                String ID = intentData.getStringExtra("ID");
                String title = intentData.getStringExtra("Title");
                String frameType = intentData.getStringExtra("FrameType");
                String type = intentData.getStringExtra("Type");
                String price = intentData.getStringExtra("Price");
                float size = intentData.getFloatExtra("Size", 1.0f);
                String description = intentData.getStringExtra("Description");
                int color = intentData.getIntExtra("Color", Color.BLACK);
                int lensesColor = intentData.getIntExtra("LensesColor", Color.BLACK);
                int templeColor = intentData.getIntExtra("TempleColor", Color.BLACK);
                int templeTipColor = intentData.getIntExtra("TempleTipColor", Color.BLACK);
                int imageResId = intentData.getIntExtra("Image", -1);
                String functionValue = intentData.getStringExtra("Function");

                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("UserUID", userModel.getUid());
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
                activity.startActivity(intent);

                // Update all messages as seen when layout is clicked
                updateAllMessagesAsSeen(senderRoom, "1");
            });
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
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            });
        }

        @Override
        public int getItemCount() {
            return userList.size();
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

        public static class UserViewHolder extends RecyclerView.ViewHolder {
            CardView user_Layout;
            TextView uidTextView, signedInTextView, createdTextView, notification_count_TextView;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                user_Layout = itemView.findViewById(R.id.user_Layout);
                uidTextView = itemView.findViewById(R.id.uidTextView);
                signedInTextView = itemView.findViewById(R.id.signedInTextView);
                createdTextView = itemView.findViewById(R.id.createdTextView);
                notification_count_TextView = itemView.findViewById(R.id.notification_count_TextView);
            }
        }
    }
