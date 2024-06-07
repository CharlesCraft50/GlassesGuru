package com.example.glassesguru;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class EyeglassesFragment extends Fragment {
    private TextView glassesTitle, glassesFrameType, glassesPrice, glassesSize, glassesDescription;
    private View glassesColorCard;
    private PrefManager prefManager;
    private RadioGroup rgFunctionOptions;
    private static final String ARG_TITLE = "Title";
    private static final String ARG_FRAME_TYPE = "FrameType";
    private static final String ARG_TYPE = "Type";
    private static final String ARG_PRICE = "Price";
    private static final String ARG_SIZE = "Size";
    private static final String ARG_DESCRIPTION = "Description";
    private static final String ARG_COLOR = "Color";
    private static final String ARG_ID = "ID";

    // TODO: Rename and change types of parameters
    private String Title;
    private String FrameType;
    private String Type;
    private String Price;
    private float Size;
    private String Description;
    private int SelectedColor;
    private String ID;

    public EyeglassesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Title = getArguments().getString(ARG_TITLE);
            FrameType = getArguments().getString(ARG_FRAME_TYPE);
            Type = getArguments().getString(ARG_TYPE);
            Price = getArguments().getString(ARG_PRICE);
            Size = getArguments().getFloat(ARG_SIZE);
            Description = getArguments().getString(ARG_DESCRIPTION);
            SelectedColor = getArguments().getInt(ARG_COLOR);
            ID = getArguments().getString(ARG_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_eyeglasses, container, false);
        glassesTitle = view.findViewById(R.id.nonPrescriptionLabel);
        glassesFrameType = view.findViewById(R.id.nonPrecriptionDescription);
        rgFunctionOptions = view.findViewById(R.id.rgFunctionOptions);
        glassesPrice = view.findViewById(R.id.glassesPrice);
        glassesSize = view.findViewById(R.id.glassesSize);
        glassesDescription = view.findViewById(R.id.glassesDescription);
        glassesColorCard = view.findViewById(R.id.glassesColorCard);
        prefManager = new PrefManager(requireContext());
        setData(Title, FrameType, Type, Price, Size, Description, SelectedColor, ID);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void setData(String title, String frameType, String type, String price, float size, String description, int color, String ID) {
        glassesTitle.setText(title);
        glassesFrameType.setText(frameType);

        String pesoSymbol = "₱";
        String formattedPrice = pesoSymbol + price;
        glassesPrice.setText(formattedPrice);

        String sizeCode;
        if (size < 1.0f) {
            sizeCode = "S";
        } else if (size == 1.0f) {
            sizeCode = "M";
        } else {
            sizeCode = "L";
        }
        String sizeText = String.format("%.1f (%s)", size, sizeCode);
        glassesSize.setText(sizeText);

        glassesDescription.setText(description);

        float[] customColor = new float[]{0f, 0f, 0f, 1f};

        if (color != -1) {
            customColor = new float[]{Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f, Color.alpha(color) / 255f};
        }

        String hexColor = String.format("#%02X%02X%02X%02X",
                (int) (customColor[3] * 255),
                (int) (customColor[0] * 255),
                (int) (customColor[1] * 255),
                (int) (customColor[2] * 255));

        glassesColorCard.setBackgroundColor(Color.parseColor(hexColor));


        if ("Eyeglasses".equalsIgnoreCase(type)) {
            rgFunctionOptions.check(R.id.rbClear);
        } else if ("Sunglasses".equalsIgnoreCase(type)) {
            rgFunctionOptions.check(R.id.rbSunglasses);
        }
    }
}