package com.example.smartwallet.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREFS_NAME = "smartwallet_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    
    private static TokenManager instance;
    private SharedPreferences prefs;
    
    private TokenManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context.getApplicationContext());
        }
        return instance;
    }
    
    public void saveToken(String token) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }
    
    public String getToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    public void clearToken() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply();
    }
    
    public boolean hasToken() {
        return getToken() != null;
    }
}


