package com.trtc.uikit.livekit.example.view.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/** Local Supabase session cache. Refresh tokens are never sent to Agora. */
public class AuthSessionStore {
    private static final String PREFS = "party_hmo_auth";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private final SharedPreferences preferences;

    public AuthSessionStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(String userId, String accessToken, String refreshToken, long expiresIn) {
        preferences.edit().putString(KEY_USER_ID, userId)
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() / 1000L + Math.max(60L, expiresIn))
                .apply();
    }

    public boolean hasSession() {
        return !TextUtils.isEmpty(getUserId()) && !TextUtils.isEmpty(getAccessToken());
    }

    public boolean isExpiringSoon() {
        return preferences.getLong(KEY_EXPIRES_AT, 0L) <= System.currentTimeMillis() / 1000L + 60L;
    }

    public String getUserId() { return preferences.getString(KEY_USER_ID, ""); }
    public String getAccessToken() { return preferences.getString(KEY_ACCESS_TOKEN, ""); }
    public String getRefreshToken() { return preferences.getString(KEY_REFRESH_TOKEN, ""); }

    public void clear() { preferences.edit().clear().apply(); }
}
