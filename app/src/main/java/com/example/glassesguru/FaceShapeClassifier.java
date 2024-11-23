package com.example.glassesguru;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceShapeClassifier {
    private static final String MODEL_FILENAME = "fine_tuned_vgg16.tflite";
    private static final String MODEL_URL = "https://github.com/CharlesCraft50/GlassesGuru/releases/download/arcore/fine_tuned_vgg16.tflite";

    private Interpreter interpreter;
    private boolean modelInitialized = false;
    public boolean noToast = false;

    public FaceShapeClassifier(Context context) {
        File modelFile = new File(context.getFilesDir(), MODEL_FILENAME);
        if (!modelFile.exists()) {
            downloadModel(context, modelFile);
        } else {
            initInterpreter(modelFile);
        }
    }

    private void initInterpreter(File modelFile) {
        try {
            ByteBuffer modelBuffer = loadModelFile(modelFile);
            interpreter = new Interpreter(modelBuffer);
            modelInitialized = true;
            Log.d("FaceShapeClassifier", "Interpreter initialized successfully.");
        } catch (IllegalArgumentException | IOException e) {
            Log.e("FaceShapeClassifier", "Failed to initialize interpreter: " + e.getMessage());
        }
    }

    private MappedByteBuffer loadModelFile(File modelFile) throws IOException {
        FileInputStream inputStream = new FileInputStream(modelFile);
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
    }

    private void downloadModel(Context context, File outputFile) {
        new ModelDownloader(context, outputFile, new ModelDownloader.ModelDownloadListener() {
            @Override
            public void onDownloadComplete(File downloadedFile) {
                initInterpreter(downloadedFile);
            }

            @Override
            public void onDownloadFailed() {
                Log.e("FaceShapeClassifier", "Model download failed.");
            }
        }).execute(MODEL_URL);
    }

//    public String classifyFace(Bitmap bitmap) {
//        if (!modelInitialized) {
//            noToast = false;
//            return "Model download failed. Connect to the internet first to download the model or wait few minutes to finish downloading";
//        }
//
//        ByteBuffer inputBuffer = convertBitmapToByteBuffer(bitmap);
//
//        float[][] output = new float[1][5]; // Adjusted to match the output size of the model
//
//        interpreter.run(inputBuffer, output);
//
//        // Log output for debugging
//        for (int i = 0; i < output[0].length; i++) {
//            Log.d("FaceShapeClassifier", "Output[" + i + "]: " + output[0][i]);
//        }
//
//        // Apply softmax to the output
//        float[] probabilities = softmax(output[0]);
//
//        // Log probabilities for debugging
//        for (int i = 0; i < probabilities.length; i++) {
//            Log.d("FaceShapeClassifier", "Probability[" + i + "]: " + probabilities[i]);
//        }
//
//        return getFaceShape(probabilities);
//    }

    public String classifyFace(Bitmap bitmap) {
        if (!modelInitialized) {
            noToast = false;
            return "Model download failed. Connect to the internet first to download the model or wait a few minutes to finish downloading.";
        }

        ByteBuffer inputBuffer = convertBitmapToByteBuffer(bitmap);

        float[][] output = new float[1][5]; // Adjusted to match the output size of the model

        interpreter.run(inputBuffer, output);

        // Log the raw model output
        StringBuilder rawOutputLog = new StringBuilder("Raw model output: [");
        for (int i = 0; i < output[0].length; i++) {
            rawOutputLog.append(output[0][i]);
            if (i < output[0].length - 1) {
                rawOutputLog.append(", ");
            }
        }
        rawOutputLog.append("]");
        Log.d("FaceShapeClassifier", rawOutputLog.toString());

        // Apply softmax to the output
        float[] probabilities = softmax(output[0]);

        // Log probabilities for debugging
        for (int i = 0; i < probabilities.length; i++) {
            Log.d("FaceShapeClassifier", "Probability[" + i + "]: " + probabilities[i]);
        }

        return getFaceShape(probabilities);
    }


//    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
//        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
//        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3); // Change size to match model input
//
//        buffer.order(ByteOrder.nativeOrder());
//
//        int[] intValues = new int[224 * 224];
//        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());
//        int pixel = 0;
//        for (int i = 0; i < 224; ++i) {
//            for (int j = 0; j < 224; ++j) {
//                final int val = intValues[pixel++];
//                // Convert RGB pixel values to float and normalize
//                buffer.putFloat(((val >> 16) & 0xFF) / 255.0f); // Red channel
//                buffer.putFloat(((val >> 8) & 0xFF) / 255.0f);  // Green channel
//                buffer.putFloat((val & 0xFF) / 255.0f);         // Blue channel
//            }
//        }
//        return buffer;
//    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        // Resize and center-crop the bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

        // Allocate buffer for model input
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3); // 4 bytes per float
        buffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[224 * 224];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());
        int pixel = 0;

        // Normalize RGB values and convert to floats
        for (int i = 0; i < 224; ++i) {
            for (int j = 0; j < 224; ++j) {
                final int val = intValues[pixel++];
                buffer.putFloat(((val >> 16) & 0xFF) / 127.5f - 1.0f); // Normalize to [-1, 1]
                buffer.putFloat(((val >> 8) & 0xFF) / 127.5f - 1.0f);  // Normalize to [-1, 1]
                buffer.putFloat((val & 0xFF) / 127.5f - 1.0f);         // Normalize to [-1, 1]
            }
        }
        return buffer;
    }

//    private float[] softmax(float[] logits) {
//        float[] expScores = new float[logits.length];
//        float sumExpScores = 0.0f;
//
//        for (int i = 0; i < logits.length; i++) {
//            expScores[i] = (float) Math.exp(logits[i]);
//            sumExpScores += expScores[i];
//        }
//
//        float[] probabilities = new float[logits.length];
//        for (int i = 0; i < logits.length; i++) {
//            probabilities[i] = expScores[i] / sumExpScores;
//        }
//
//        return probabilities;
//    }

    private float[] softmax(float[] logits) {
        float maxLogit = Float.MIN_VALUE;
        for (float logit : logits) {
            if (logit > maxLogit) {
                maxLogit = logit;
            }
        }

        float[] expScores = new float[logits.length];
        float sumExpScores = 0.0f;

        for (int i = 0; i < logits.length; i++) {
            expScores[i] = (float) Math.exp(logits[i] - maxLogit);
            sumExpScores += expScores[i];
        }

        float[] probabilities = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            probabilities[i] = expScores[i] / sumExpScores;
        }

        return probabilities;
    }

//    private String getFaceShape(float[] probabilities) {
//        noToast = true;
//        String[] faceShapes = {"Round", "Oval", "Square", "Heart", "Long"};
//
//        int shapeIndex = -1;
//        float maxProbability = Float.MIN_VALUE;
//
//        // Find the index of the shape with the maximum probability
//        for (int i = 0; i < probabilities.length; i++) {
//            if (probabilities[i] > maxProbability) {
//                maxProbability = probabilities[i];
//                shapeIndex = i;
//            }
//        }
//
//        // Return the corresponding face shape or "Unknown" if no shape found
//        if (shapeIndex != -1 && shapeIndex < faceShapes.length) {
//            return faceShapes[shapeIndex];
//        } else {
//            return "Unknown";
//        }
//    }

    private String getFaceShape(float[] probabilities) {
        noToast = true;

        // Updated class names based on your TFLite model output
        String[] faceShapes = {"Heart", "Long", "Oval", "Round", "Square"};

        int shapeIndex = -1;
        float maxProbability = Float.MIN_VALUE;

        // Find the index of the shape with the maximum probability
        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] > maxProbability) {
                maxProbability = probabilities[i];
                shapeIndex = i;
            }
        }

        // Log only the confidence for debugging
        Log.d("FaceShapeClassifier", String.format("Confidence: %.2f%%", maxProbability * 100));

        // Return the corresponding face shape or "Unknown" if no shape found
        if (shapeIndex != -1 && shapeIndex < faceShapes.length) {
            return faceShapes[shapeIndex];
        } else {
            return "Unknown";
        }
    }
}