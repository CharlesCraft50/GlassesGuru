package com.example.glassesguru;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class EyeglassesFragment extends Fragment {
    private TextView glassesTitle, glassesFrameType, glassesPrice, glassesSize, glassesDescription;
    private View glassesFrameColorCard, glassesLensesColorCard, glassesTempleColorCard, glassesTempleTipColorCard;
    private PrefManager prefManager;
    private RadioGroup rgFunctionOptions;
    private static final String ARG_TITLE = "Title";
    private static final String ARG_FRAME_TYPE = "FrameType";
    private static final String ARG_TYPE = "Type";
    private static final String ARG_PRICE = "Price";
    private static final String ARG_SIZE = "Size";
    private static final String ARG_DESCRIPTION = "Description";
    private static final String ARG_COLOR = "Color";
    private static final String ARG_LENSES_COLOR = "LensesColor";
    private static final String ARG_TEMPLE_COLOR = "TempleColor";
    private static final String ARG_TEMPLE_TIP_COLOR = "TempleTipColor";
    private static final String ARG_ID = "ID";

    // TODO: Rename and change types of parameters
    private String Title;
    private String FrameType;
    private String Type;
    private String Price;
    private float Size;
    private String Description;
    private int SelectedColor;
    private int LensesColor;
    private int TempleColor;
    private int TempleTipColor;
    private String ID;

    public EyeglassesFragment() {
        // Required empty public constructor
    }

    public interface OnRadioButtonSelectedListener {
        void onRadioButtonSelected(String text);
    }

    private OnRadioButtonSelectedListener listener;

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
            LensesColor = getArguments().getInt(ARG_LENSES_COLOR);
            TempleColor = getArguments().getInt(ARG_TEMPLE_COLOR);
            TempleTipColor = getArguments().getInt(ARG_TEMPLE_TIP_COLOR);
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
        glassesFrameColorCard = view.findViewById(R.id.glassesFrameColorCard);
        glassesLensesColorCard = view.findViewById(R.id.glassesLensesColorCard);
        glassesTempleColorCard = view.findViewById(R.id.glassesTempleColorCard);
        glassesTempleTipColorCard = view.findViewById(R.id.glassesTempleTipColorCard);
        prefManager = new PrefManager(requireContext());
        setData(Title, FrameType, Type, Price, Size, Description, SelectedColor, LensesColor, TempleColor, TempleTipColor, ID);

        // Set a listener for RadioGroup changes
        rgFunctionOptions.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId != -1) {
                    RadioButton checkedRadioButton = view.findViewById(checkedId);
                    String text = checkedRadioButton.getText().toString();
                    if (listener != null) {
                        listener.onRadioButtonSelected(text);
                    }
                }
            }
        });

        // Initialize the listener with the default checked value
        String initialText = getCheckedRadioButtonText();
        if (listener != null && !initialText.isEmpty()) {
            listener.onRadioButtonSelected(initialText);
        }

        return view;
    }

    public String getCheckedRadioButtonText() {
        if (getView() != null) {
            RadioGroup radioGroup = getView().findViewById(R.id.rgFunctionOptions);
            int checkedId = radioGroup.getCheckedRadioButtonId();
            if (checkedId != -1) {
                RadioButton checkedRadioButton = getView().findViewById(checkedId);
                return checkedRadioButton.getText().toString();
            }
        }
        return "";
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void setData(String title, String frameType, String type, String price, float size, String description, int color, int lensesColor, int templeColor, int templeTipColor, String ID) {
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

        glassesFrameColorCard.setBackgroundColor(Color.parseColor(showColor(color)));

        // Check if the LensesColor exists
        if (lensesColor != 0x00000000) {
            glassesLensesColorCard.setVisibility(View.VISIBLE);
            glassesLensesColorCard.setBackgroundColor(lensesColor);
        } else {
            glassesLensesColorCard.setVisibility(View.GONE);
        }

        // Check if the TempleColor exists
        if (templeColor != 0x00000000) {
            glassesTempleColorCard.setVisibility(View.VISIBLE);
            glassesTempleColorCard.setBackgroundColor(templeColor);
        } else {
            glassesTempleColorCard.setVisibility(View.GONE);
        }

        // Check if the TempleTipColor exists
        if (templeTipColor != 0x00000000) {
            glassesTempleTipColorCard.setVisibility(View.VISIBLE);
            glassesTempleTipColorCard.setBackgroundColor(templeTipColor);
        } else {
            glassesTempleTipColorCard.setVisibility(View.GONE);
        }

        rgFunctionOptions.check(R.id.rbClear);

        if ("Eyeglasses".equalsIgnoreCase(type)) {
            rgFunctionOptions.check(R.id.rbClear);
        } else if ("Sunglasses".equalsIgnoreCase(type)) {
            rgFunctionOptions.check(R.id.rbSunglasses);
        }
    }

    private String showColor(int color) {
        float[] customColor = new float[]{0f, 0f, 0f, 1f};

        if (color != 0x00000000) {
            customColor = new float[]{Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f, Color.alpha(color) / 255f};
        }

        String hexColor = String.format("#%02X%02X%02X%02X",
                (int) (customColor[3] * 255),
                (int) (customColor[0] * 255),
                (int) (customColor[1] * 255),
                (int) (customColor[2] * 255));

        return hexColor;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (OnRadioButtonSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnRadioButtonSelectedListener");
        }
    }
}