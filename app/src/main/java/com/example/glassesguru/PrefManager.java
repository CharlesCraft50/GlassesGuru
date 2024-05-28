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
}
