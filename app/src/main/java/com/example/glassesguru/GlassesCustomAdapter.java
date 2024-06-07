package com.example.glassesguru;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class GlassesCustomAdapter extends RecyclerView.Adapter<GlassesCustomAdapter.MyViewHolder> {
    private Activity activity;
    private Context context;
    private ArrayList glasses_id;
    private ArrayList<byte[]> glasses_image;
    private ArrayList glasses_title, glasses_brand, glasses_description, glasses_date, glasses_link, glasses_price;
    private String glassesModel;

    public GlassesCustomAdapter(Activity activity, Context context, ArrayList glasses_id, ArrayList<byte[]> glasses_image, ArrayList glasses_title, ArrayList glasses_brand, ArrayList glasses_description, ArrayList glasses_date, ArrayList glasses_link, ArrayList glasses_price) {
        this.activity = activity;
        this.context = context;
        this.glasses_id = glasses_id;
        this.glasses_image = glasses_image;
        this.glasses_title = glasses_title;
        this.glasses_brand = glasses_brand;
        this.glasses_description = glasses_description;
        this.glasses_date = glasses_date;
        this.glasses_link = glasses_link;
        this.glasses_price = glasses_price;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.glasses_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        byte[] imageData = glasses_image.get(position);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        holder.glasses_image_text.setImageBitmap(bitmap);
        holder.glasses_title_text.setText(glasses_title.get(position).toString());
        holder.glasses_brand_text.setText(glasses_brand.get(position).toString());
        holder.glasses_description_text.setText(glasses_description.get(position).toString());
        holder.glasses_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return glasses_id.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout glasses_card;
        ImageView glasses_image_text;
        TextView glasses_title_text, glasses_brand_text, glasses_description_text;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            glasses_card = itemView.findViewById(R.id.glasses_card);
            glasses_image_text = itemView.findViewById(R.id.glassesImage);
            glasses_title_text = itemView.findViewById(R.id.nonPrescriptionLabel);
            glasses_brand_text = itemView.findViewById(R.id.nonPrecriptionDescription);
            glasses_description_text = itemView.findViewById(R.id.glassesDescription);
        }
    }
}
