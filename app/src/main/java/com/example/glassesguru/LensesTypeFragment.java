package com.example.glassesguru;

import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LensesTypeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LensesTypeFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ConstraintLayout constraintBlueBlockLenses, constraintClearLens, constraintTransitions, constraintNonPrescription;
    private LinearLayout blueBlockLensesOptionsLayout;

    public LensesTypeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LensesTypeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LensesTypeFragment newInstance(String param1, String param2) {
        LensesTypeFragment fragment = new LensesTypeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lenses_type, container, false);
        constraintBlueBlockLenses = view.findViewById(R.id.constraintBlueBlockLenses);
        blueBlockLensesOptionsLayout = view.findViewById(R.id.blueBlockLensesOptionsLayout);
        constraintBlueBlockLenses.setOnClickListener(v -> {
                selectView(constraintBlueBlockLenses);
                toggleBlueBlockLensesOptionsLayout();
        });
        constraintClearLens = view.findViewById(R.id.constraintClearLens);
        constraintClearLens.setOnClickListener(v -> {
            selectView(constraintClearLens);
        });
        constraintTransitions = view.findViewById(R.id.constraintTransitions);
        constraintTransitions.setOnClickListener(v -> {
            selectView(constraintTransitions);
        });
        constraintNonPrescription = view.findViewById(R.id.constraintNonPrescription);
        constraintNonPrescription.setOnClickListener(v -> {
            selectView(constraintNonPrescription);
        });
        return view;
    }

    private void selectView(View view) {
        view.setBackgroundResource(R.drawable.rounded_box_light_blue);

        // Set the background of the other views back to normal
        if (view != constraintBlueBlockLenses) {
            constraintBlueBlockLenses.setBackgroundResource(R.drawable.rounded_box_white);
            blueBlockLensesOptionsLayout.setVisibility(View.GONE);
        }
        if (view != constraintClearLens) {
            constraintClearLens.setBackgroundResource(R.drawable.rounded_box_white);
        }
        if (view != constraintTransitions) {
            constraintTransitions.setBackgroundResource(R.drawable.rounded_box_white);
        }
        if (view != constraintNonPrescription) {
            constraintNonPrescription.setBackgroundResource(R.drawable.rounded_box_white);
        }
    }

    private void toggleBlueBlockLensesOptionsLayout() {
        if (blueBlockLensesOptionsLayout.getVisibility() == View.GONE) {
            blueBlockLensesOptionsLayout.setVisibility(View.VISIBLE);
        } else {
            blueBlockLensesOptionsLayout.setVisibility(View.GONE);
        }
    }
}