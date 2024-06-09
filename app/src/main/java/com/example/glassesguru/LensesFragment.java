package com.example.glassesguru;

import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LensesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LensesFragment extends Fragment {

    private ConstraintLayout constraintSingleVision, constraintBifocal, constraintReading, constraintNonPrescription;
    private LinearLayout bifocalOptionsLayout, readingOptionsLayout;
    private static final String TAG = "LensesFragment";

    public LensesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LensesFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LensesFragment newInstance(String param1, String param2) {
        LensesFragment fragment = new LensesFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lenses, container, false);
        constraintSingleVision = view.findViewById(R.id.constraintSingleVision);
        constraintSingleVision.setOnClickListener(v -> {
            selectView(constraintSingleVision);
        });
        constraintBifocal = view.findViewById(R.id.constraintBifocal);
        constraintBifocal.setOnClickListener(v -> {
            selectView(constraintBifocal);
            toggleBifocalOptionsLayout();
        });
        bifocalOptionsLayout = view.findViewById(R.id.bifocalOptionsLayout);
        bifocalOptionsLayout.setVisibility(View.GONE); // Hide bifocal options layout by default

        constraintReading = view.findViewById(R.id.constraintReading);
        constraintReading.setOnClickListener(v -> {
            selectView(constraintReading);
            toggleReadingOptionsLayout();
        });
        readingOptionsLayout = view.findViewById(R.id.readingOptionsLayout);
        readingOptionsLayout.setVisibility(View.GONE); // Hide reading options layout by default

        constraintNonPrescription = view.findViewById(R.id.constraintNonPrescription);
        constraintNonPrescription.setOnClickListener(v -> {
            selectView(constraintNonPrescription);
        });

        return view;
    }

    private void selectView(View view) {
        view.setBackgroundResource(R.drawable.rounded_box_light_blue);

        // Set the background of the other views back to normal
        if (view != constraintSingleVision) {
            constraintSingleVision.setBackgroundResource(R.drawable.rounded_box_white);
        }
        if (view != constraintBifocal) {
            constraintBifocal.setBackgroundResource(R.drawable.rounded_box_white);
            bifocalOptionsLayout.setVisibility(View.GONE);
        }
        if (view != constraintReading) {
            constraintReading.setBackgroundResource(R.drawable.rounded_box_white);
            readingOptionsLayout.setVisibility(View.GONE);
        }
        if (view != constraintNonPrescription) {
            constraintNonPrescription.setBackgroundResource(R.drawable.rounded_box_white);
        }
    }

    private void toggleBifocalOptionsLayout() {
        if (bifocalOptionsLayout.getVisibility() == View.GONE) {
            bifocalOptionsLayout.setVisibility(View.VISIBLE);
        } else {
            bifocalOptionsLayout.setVisibility(View.GONE);
        }
    }

    private void toggleReadingOptionsLayout() {
        if (readingOptionsLayout.getVisibility() == View.GONE) {
            readingOptionsLayout.setVisibility(View.VISIBLE);
        } else {
            readingOptionsLayout.setVisibility(View.GONE);
        }
    }
}