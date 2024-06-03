package com.example.glassesguru;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;
    private static final String PREF_NAME = "tutorial_pref";
    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    private static final String IS_FIRST_TIME_LAUNCH_MORE_OPTIONS = "IsFirstTimeLaunchMoreOptions";
    private static final String KEY_FAVORITES = "favorites";

    public PrefManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    public void setFirstTimeLaunchMoreOptions(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH_MORE_OPTIONS, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }
    public boolean isFirstTimeLaunchMoreOptions() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH_MORE_OPTIONS, true);
    }

    // Method to add a glasses ID to favorites
    public void addToFavorites(String glassesId) {
        SharedPreferences.Editor editor = pref.edit();
        String favorites = pref.getString(KEY_FAVORITES, "");
        if (!favorites.contains(glassesId)) {
            favorites += glassesId + ",";
            editor.putString(KEY_FAVORITES, favorites);
            editor.apply();
        }
    }

    // Method to remove a glasses ID from favorites
    public void removeFromFavorites(String glassesId) {
        SharedPreferences.Editor editor = pref.edit();
        String favorites = pref.getString(KEY_FAVORITES, "");
        if (favorites.contains(glassesId)) {
            favorites = favorites.replace(glassesId + ",", "");
            editor.putString(KEY_FAVORITES, favorites);
            editor.apply();
        }
    }

    // Method to check if a glasses ID is marked as favorite
    public boolean isFavorite(String glassesId) {
        String favorites = pref.getString(KEY_FAVORITES, "");
        return favorites.contains(glassesId);
    }

    // Method to get the list of favorite glasses IDs
    public String getFavorites() {
        return pref.getString(KEY_FAVORITES, "");
    }
}
