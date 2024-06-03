package com.example.glassesguru;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class ModelDownloader extends AsyncTask<String, Void, Boolean> {

    private Context context;
    private File outputFile;
    private ModelDownloadListener listener;

    public ModelDownloader(Context context, File outputFile, ModelDownloadListener listener) {
        this.context = context;
        this.outputFile = outputFile;
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(String... urls) {
        try {
            URL url = new URL(urls[0]);
            URLConnection connection = url.openConnection();
            connection.connect();

            // Download the file
            BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            return true;
        } catch (IOException e) {
            Log.e("ModelDownloader", "Error downloading model: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            listener.onDownloadComplete(outputFile);
        } else {
            listener.onDownloadFailed();
        }
    }

    public interface ModelDownloadListener {
        void onDownloadComplete(File outputFile);
        void onDownloadFailed();
    }
}