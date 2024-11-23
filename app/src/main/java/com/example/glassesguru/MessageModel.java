package com.example.glassesguru;

public class MessageModel {
    private String messageId;
    private String senderId;
    private String message;
    private long timestamp;
    private String imageUrl;
    private boolean seen = false;
    private boolean isLiked = false;

    // No-argument constructor
    public MessageModel() {
        // Default constructor required for calls to DataSnapshot.getValue(MessageModel.class)
    }

    // Constructor with parameters
    public MessageModel(String messageId, String senderId, String message, long timestamp, String imageUrl) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.message = message;
        this.timestamp = timestamp; // Set the timestamp
        this.imageUrl = imageUrl;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean isLiked) {
        this.isLiked = isLiked;
    }
}
