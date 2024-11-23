package com.example.glassesguru;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

public class CustomSpinner extends androidx.appcompat.widget.AppCompatSpinner {

    public CustomSpinner(Context context) {
        super(context);
    }

    public CustomSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        // Call the super method to show the default drop-down view
        super.performClick();

        // Show the custom drop-down view
        MultiSelectSpinnerAdapter adapter = (MultiSelectSpinnerAdapter) getAdapter();
        adapter.showCustomDropDownView();

        return true;
    }
}
