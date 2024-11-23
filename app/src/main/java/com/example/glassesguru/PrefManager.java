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
    private static final String USERUID = "UserUID";
    private static final String IS_ADMIN = "IsAdmin";
    private static final String EMAIL = "glassesguru.official@gmail.com";
    private static final String PASSWORD = "grp3g3";
    public static final String FIREBASE_DATABASE_URL = "https://glassesguru-f7296-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static final String IS_NEW_USER = "IsNewUser";
    private static final String FIRST_TERMS_AND_CONDITIONS = "FirstTermsAndConditions";
    private static final String KEY_ALL_FILES_DOWNLOADED = "AllFilesDownloaded";
    private static final String KEY_GLASSES_COUNT_DOWNLOADED = "GlassesCountDownloaded";
    private static final String KEY_THEME_COLOR = "ThemeColor";

    public PrefManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setThemeColor(String color) {
        editor.putString(KEY_THEME_COLOR, color);
        editor.commit();
    }

    public String getThemeColor() {
        return pref.getString(KEY_THEME_COLOR, "slate");
    }

    public void setGlassesCountDownloaded(int count) {
        editor.putInt(KEY_GLASSES_COUNT_DOWNLOADED, count);
        editor.commit();
    }

    public int getGlassesCountDownloaded() {
        return pref.getInt(KEY_GLASSES_COUNT_DOWNLOADED, 0);
    }

    public void setAllFilesDownloaded(boolean isAllFilesDownloaded) {
        editor.putBoolean(KEY_ALL_FILES_DOWNLOADED, isAllFilesDownloaded);
        editor.commit();

    }

    public boolean isAllFilesDownloaded() {
        return pref.getBoolean(KEY_ALL_FILES_DOWNLOADED, false);
    }

    public void setFirstTermsAndConditions(boolean isFirstTime) {
        editor.putBoolean(FIRST_TERMS_AND_CONDITIONS, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTermsAndConditions() {
        return pref.getBoolean(FIRST_TERMS_AND_CONDITIONS, true);
    }

    public void setNewUser(boolean isNewUser) {
        editor.putBoolean(IS_NEW_USER, isNewUser);
        editor.commit();
    }

    public boolean isNewUser() {
        return pref.getBoolean(IS_NEW_USER, true);
    }

    public String getEmail() {
        return EMAIL;
    }

    public String getPassword() {
        return PASSWORD;
    }

    public boolean isAdmin() {
        return pref.getBoolean(IS_ADMIN, false);
    }

    public void setAdmin(boolean isAdmin) {
        editor.putBoolean(IS_ADMIN, isAdmin);
        editor.commit();
    }

    public void setUserUID(String userUID) {
        editor.putString(USERUID, userUID);
        editor.commit();
    }

    public String getUserUID() {
        return pref.getString(USERUID, "");
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
