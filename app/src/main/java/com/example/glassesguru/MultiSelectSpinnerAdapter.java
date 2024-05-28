package com.example.glassesguru;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiSelectSpinnerAdapter extends ArrayAdapter<String> {
    private Context context;
    private List<String> items;
    private boolean[] selectedItems;
    private List<String> selectedItemsList;
    private String spinnerTitle;
    private OnItemSelectedListener listener;

    public MultiSelectSpinnerAdapter(Context context, int resource, List<String> items, String spinnerTitle) {
        super(context, resource, items);
        this.context = context;
        this.items = items;
        this.spinnerTitle = spinnerTitle;
        this.selectedItems = new boolean[items.size()];
        this.selectedItemsList = new ArrayList<>();
    }

    public interface OnItemSelectedListener {
        void onItemSelected(List<String> selectedItemsList);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }

        if (convertView != null) {
            TextView textView = convertView.findViewById(android.R.id.text1);
            if (textView != null) {
                textView.setText(spinnerTitle);
                textView.setTextSize(12);
                textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                textView.setTextColor(ContextCompat.getColor(context, R.color.light_gray_semi_transparent));
            }
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (position == 0) {
            TextView textView = new TextView(context);
            textView.setHeight(0);
            textView.setVisibility(View.GONE);
            return textView;
        } else {
            return super.getDropDownView(position, convertView, parent);
        }
    }

    public void showCustomDropDownView() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(spinnerTitle);

        builder.setMultiChoiceItems(items.toArray(new CharSequence[items.size()]), selectedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (which == 0) {
                    // If "All" is selected, clear other selections
                    if (isChecked) {
                        Arrays.fill(selectedItems, false);
                        selectedItemsList.clear();
                        selectedItems[0] = true;
                        selectedItemsList.add(items.get(0));
                    } else {
                        selectedItemsList.remove(items.get(0));
                    }
                } else {
                    // If any other item is selected, deselect "All"
                    if (isChecked) {
                        selectedItems[which] = true;
                        selectedItemsList.add(items.get(which));
                        selectedItems[0] = false;
                        selectedItemsList.remove(items.get(0));
                    } else {
                        selectedItems[which] = false;
                        selectedItemsList.remove(items.get(which));
                    }
                }
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener != null) {
                    listener.onItemSelected(selectedItemsList);
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Hide the navigation and status bar
        Window window = dialog.getWindow();
        if (window != null) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public List<String> getSelectedItemsList() {
        return selectedItemsList;
    }

    // Method to set selected items programmatically
    public void setSelectedItems(List<String> selectedItems) {
        // Clear the existing selection
        Arrays.fill(this.selectedItems, false);
        selectedItemsList.clear();

        // Set new selected items
        for (String item : selectedItems) {
            int index = items.indexOf(item);
            if (index >= 0) {
                this.selectedItems[index] = true;
                selectedItemsList.add(item);
            }
        }
        notifyDataSetChanged();
    }

    // Method to clear selected items
    public void clearSelectedItems() {
        Arrays.fill(selectedItems, false);
        selectedItemsList.clear();

        // Keep the first item ("All") selected
        if (!items.isEmpty()) {
            selectedItems[0] = true;
            selectedItemsList.add(items.get(0));
        }

        if (listener != null) {
            listener.onItemSelected(selectedItemsList);
        }
        notifyDataSetChanged();
    }

}
