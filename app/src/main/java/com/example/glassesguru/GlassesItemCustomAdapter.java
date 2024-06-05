package com.example.glassesguru;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class GlassesItemCustomAdapter extends RecyclerView.Adapter<GlassesItemCustomAdapter.MyViewHolder> {
    private Activity activity;
    private Context context;
    private ArrayList<Integer> glasses_image;
    private ArrayList<String> glasses_id, glasses_title, glasses_obj_name, temple_obj_name, lenses_obj_name, glasses_frame_type, glasses_type, pads_obj_name, description, stacks, glasses_price;
    private CameraFaceActivity faceActivityInstance;
    private PrefManager prefManager;

    public GlassesItemCustomAdapter(Activity activity, Context context, ArrayList<String> glasses_id, ArrayList<Integer> glasses_image, ArrayList<String> glasses_title, ArrayList<String> glasses_obj_name, ArrayList<String> temple_obj_name, ArrayList<String> lenses_obj_name, ArrayList<String> glasses_frame_type, ArrayList<String> glasses_type, ArrayList<String> pads_obj_name, ArrayList<String> description, ArrayList<String> stacks, ArrayList<String> glasses_price) {
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
        prefManager = new PrefManager(context);
        if (activity instanceof CameraFaceActivity) {
            faceActivityInstance = (CameraFaceActivity) activity;
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.item_glasses, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        int imageResId = glasses_image.get(position);
        holder.glassesImageView.setImageResource(imageResId);

        holder.glassesImageView.setAdjustViewBounds(true);

        holder.glassesTitle.setText(glasses_title.get(position));

        holder.glassesCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newModel = glasses_obj_name.get(position);
                String newTemple = temple_obj_name.get(position);
                String newLenses = lenses_obj_name.get(position);
                String newGlassesType = glasses_frame_type.get(position);
                String newPads = pads_obj_name.get(position);
                faceActivityInstance.updateGlassesModel(newModel, newTemple, newLenses, newGlassesType, newPads);
                faceActivityInstance.capture_button.startLoadingAnimation();
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
            intent.putExtra("Color", faceActivityInstance.selectedColor);
            activity.startActivityForResult(intent, CameraFaceActivity.REFRESH_ITEMS);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return glasses_id.size();
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
