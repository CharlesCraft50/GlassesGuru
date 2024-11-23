package com.example.glassesguru;

public class UserModel {
    private String uid;
    private long signedInTimestamp;
    private long createdTimestamp;

    public UserModel() {
        // Default constructor required for calls to DataSnapshot.getValue(UserModel.class)
    }

    public UserModel(String uid, long signedInTimestamp, long createdTimestamp) {
        this.uid = uid;
        this.signedInTimestamp = signedInTimestamp;
        this.createdTimestamp = createdTimestamp;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getSignedInTimestamp() {
        return signedInTimestamp;
    }

    public void setSignedInTimestamp(long signedInTimestamp) {
        this.signedInTimestamp = signedInTimestamp;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
}

