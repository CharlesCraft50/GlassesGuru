package com.example.glassesguru;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;

public class GlassesItemCustomAdapter extends RecyclerView.Adapter<GlassesItemCustomAdapter.MyViewHolder> {
    private Activity activity;
    private Context context;
    private ArrayList<Integer> glasses_image;
    private ArrayList<String> glasses_id, glasses_title, glasses_obj_name, temple_obj_name, lenses_obj_name, glasses_frame_type, glasses_type, pads_obj_name, description, stacks, glasses_price, transparency;
    private ArrayList<Boolean> is_downloaded;
    private CameraFaceActivity faceActivityInstance;
    private PrefManager prefManager;
    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LAST_ITEM = 1;
    private boolean allFilesExist = true;

    public GlassesItemCustomAdapter(Activity activity, Context context, ArrayList<String> glasses_id, ArrayList<Integer> glasses_image, ArrayList<String> glasses_title, ArrayList<String> glasses_obj_name, ArrayList<String> temple_obj_name, ArrayList<String> lenses_obj_name, ArrayList<String> glasses_frame_type, ArrayList<String> glasses_type, ArrayList<String> pads_obj_name, ArrayList<String> description, ArrayList<String> stacks, ArrayList<String> glasses_price, ArrayList<String> transparency, ArrayList<Boolean> is_downloaded) {
        this.activity = activity;
        this.context = context;
        this.glasses_id = glasses_id;
        this.glasses_image = glasses_image;
        this.glasses_title = glasses_title;
        this.glasses_obj_name = glasses_obj_name;
        this.temple_obj_name = temple_obj_name;
        this.lenses_obj_name = lenses_obj_name;
        this.glasses_frame_type = glasses_frame_type;
        this.glasses_type = glasses_type;
        this.pads_obj_name = pads_obj_name;
        this.description = description;
        this.glasses_price = glasses_price;
        this.stacks = stacks;
        this.transparency = transparency;
        this.is_downloaded = is_downloaded;
        prefManager = new PrefManager(context);
        if (activity instanceof CameraFaceActivity) {
            faceActivityInstance = (CameraFaceActivity) activity;
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_ITEM) {
            View view = layoutInflater.inflate(R.layout.item_glasses, parent, false);
            return new MyViewHolder(view);
        } else {
            // Inflate a different layout for the last item
            View view = layoutInflater.inflate(R.layout.item_glasses_last, parent, false);
            return new MyViewHolder(view);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == glasses_id.size()) {
            return VIEW_TYPE_LAST_ITEM; // The last item (extra one)
        } else {
            return VIEW_TYPE_ITEM; // Regular item
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            if(!is_downloaded.get(position)) {
                int imageResId = glasses_image.get(position);
                holder.glassesImageView.setImageResource(imageResId);

                holder.glassesImageView.setAdjustViewBounds(true);
            } else {
                File imageFile = new File(context.getExternalFilesDir(null), "glasses_" + glasses_id.get(position) + ".png");

                if(imageFile.exists()) {
                    Glide.with(holder.glassesImageView.getContext())
                            .load(imageFile)
                            .into(holder.glassesImageView);

                    holder.glassesImageView.setAdjustViewBounds(true);
                } else {
                    Log.e("ImageLoading", "Image file not found: " + imageFile.getAbsolutePath());
                }
            }

            holder.glassesTitle.setText(glasses_title.get(position));

            holder.glassesCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newModel = glasses_obj_name.get(position);
                    String newTemple = temple_obj_name.get(position);
                    String newLenses = lenses_obj_name.get(position);
                    String newGlassesType = glasses_frame_type.get(position);
                    String newPads = pads_obj_name.get(position);
                    String newTransparency = transparency.get(position);
                    boolean newIsDownloaded = is_downloaded.get(position);

                    if(!is_downloaded.get(position)) {
                        faceActivityInstance.modelBasePath = "models/glasses/";
                    } else {
                        faceActivityInstance.modelBasePath = context.getExternalFilesDir(null).getAbsolutePath() + "/";
                    }

                    faceActivityInstance.updateGlassesModel(newModel, newTemple, newLenses, newGlassesType, newPads, newTransparency, newIsDownloaded);
                    faceActivityInstance.capture_button.startLoadingAnimation();

                    if (listener != null) {
                        listener.onItemClick(position);
                    }
                }
            });

            if(prefManager.isFavorite(glasses_id.get(position))) {
                holder.glassesFavoriteImageView.setVisibility(View.VISIBLE);
            } else {
                holder.glassesFavoriteImageView.setVisibility(View.GONE);
            }

            holder.glassesCardView.setOnLongClickListener(v -> {
                Intent intent = new Intent(context, GlassesActivity.class);
                intent.putExtra("ID", glasses_id.get(position));
                intent.putExtra("Image", glasses_image.get(position));
                intent.putExtra("Title", glasses_title.get(position));
                intent.putExtra("FrameType", glasses_frame_type.get(position));
                intent.putExtra("Type", glasses_type.get(position));
                intent.putExtra("Price", glasses_price.get(position));
                intent.putExtra("Size", faceActivityInstance.scaleFactor);
                intent.putExtra("Description", description.get(position));
                intent.putExtra("Color", faceActivityInstance.eyesObjectCustomColor);
                intent.putExtra("LensesColor", faceActivityInstance.lensesObjectCustomColor);
                intent.putExtra("TempleColor", faceActivityInstance.templeObjectCustomColor);
                intent.putExtra("TempleTipColor", faceActivityInstance.templeTipObjectCustomColor);
                intent.putExtra("IsDownloaded", is_downloaded.get(position));
                activity.startActivityForResult(intent, CameraFaceActivity.REFRESH_ITEMS);
                return true;
            });
        } else {
            if(prefManager.isAllFilesDownloaded()) {
                holder.glassesTitle.setText("Update Check");
            } else {
                holder.glassesTitle.setText("Download More");
            }
            holder.glassesCardView.setOnClickListener(v -> {
                holder.glassesTitle.setText("Downloading...");
                Toast.makeText(context, "Fetching available models...", Toast.LENGTH_SHORT).show();
                fetchAndDownloadGlassesModels();
                prefManager.setAllFilesDownloaded(true);
            });
        }

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

        if ("data.json".equals(fileRef.getName())) {
            // Always replace data.json regardless of its size
            downloadAndReplaceFile(fileRef, localFile, totalFiles);
        } else {
            if (localFile.exists()) {
                // Check if the local file size is the same as the remote file size
                fileRef.getMetadata().addOnSuccessListener(storageMetadata -> {
                    long remoteFileSize = storageMetadata.getSizeBytes();
                    if (localFile.length() == remoteFileSize) {
                        // File sizes match, so skip download
                        checkAllFilesDownloaded(totalFiles);
                    } else {
                        // File sizes differ, download and replace the local file
                        downloadAndReplaceFile(fileRef, localFile, totalFiles);
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to check file metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } else {
                // File doesn't exist, download it
                downloadAndReplaceFile(fileRef, localFile, totalFiles);
            }
        }
    }

    private void downloadAndReplaceFile(StorageReference fileRef, File localFile, int totalFiles) {
        fileRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
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

        Intent intent = new Intent(activity, CameraFaceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
        activity.finish();
    }

    @Override
    public int getItemCount() {
        return glasses_id.size() + 1;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView glassesImageView, glassesFavoriteImageView;
        TextView glassesTitle;
        LinearLayout glassesCardView;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            glassesImageView = itemView.findViewById(R.id.glassesImageView);
            glassesTitle = itemView.findViewById(R.id.glassesNameTextView);
            glassesCardView = itemView.findViewById(R.id.glassesCardView);
            glassesFavoriteImageView = itemView.findViewById(R.id.glassesFavoriteImageView);
        }
    }
}
