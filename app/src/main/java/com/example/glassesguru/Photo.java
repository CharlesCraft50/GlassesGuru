package com.example.glassesguru;

import android.net.Uri;

public class Photo {
    private Uri photoUri;

    public Photo(Uri photoUri) {
        this.photoUri = photoUri;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(Uri photoUri) {
        this.photoUri = photoUri;
    }
}
