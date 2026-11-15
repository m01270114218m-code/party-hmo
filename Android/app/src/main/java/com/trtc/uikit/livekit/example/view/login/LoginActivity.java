package com.trtc.uikit.livekit.example.view.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.trtc.uikit.livekit.example.BaseActivity;
import com.trtc.uikit.livekit.example.R;
import com.trtc.uikit.livekit.example.data.SupabaseApi;
import com.trtc.uikit.livekit.example.store.AppStore;
import com.trtc.uikit.livekit.example.view.main.MainActivity;

import org.json.JSONObject;

public class LoginActivity extends BaseActivity {
    private static final String TAG = "LoginActivity";
    private EditText emailEdit;
    private EditText passwordEdit;
    private AuthSessionStore sessionStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionStore = new AuthSessionStore(this);
        if (sessionStore.hasSession()) {
            restoreSession();
            return;
        }
        showLogin();
    }

    private void showLogin() {
        setContentView(R.layout.app_activity_login);
        emailEdit = findViewById(R.id.et_email);
        passwordEdit = findViewById(R.id.et_password);
        findViewById(R.id.btn_login).setOnClickListener(v -> authenticate(false));
        findViewById(R.id.btn_register).setOnClickListener(v -> authenticate(true));
    }

    private void authenticate(boolean register) {
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString();
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEdit.setError("Enter a valid email");
            return;
        }
        if (password.length() < 6) {
            passwordEdit.setError("Password must be at least 6 characters");
            return;
        }
        setBusy(true);
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);
                String endpoint = register ? "/auth/v1/signup" : "/auth/v1/token?grant_type=password";
                JSONObject result = SupabaseApi.request(endpoint, "POST", body, null);
                JSONObject user = result.optJSONObject("user");
                String access = result.optString("access_token", "");
                if (user == null || user.optString("id").isEmpty()) {
                    runOnUiThread(() -> {
                        setBusy(false);
                        Toast.makeText(this, "Account created. Confirm your email, then sign in.", Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                if (access.isEmpty()) throw new IllegalStateException("No active session returned by Supabase");
                String userId = user.getString("id");
                sessionStore.save(userId, access, result.optString("refresh_token", ""),
                        result.optLong("expires_in", 3600));
                AppStore.userId = userId;
                try {
                    syncProfile(userId, email, access);
                } catch (Exception profileError) {
                    Log.w(TAG, "Profile initialization will be retried later", profileError);
                }
                runOnUiThread(() -> {
                    setBusy(false);
                    openMain();
                });
            } catch (Exception e) {
                Log.e(TAG, "Supabase authentication failed", e);
                runOnUiThread(() -> {
                    setBusy(false);
                    Toast.makeText(this, readableError(e), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void restoreSession() {
        setContentView(R.layout.app_activity_login);
        new Thread(() -> {
            try {
                String access = sessionStore.getAccessToken();
                if (sessionStore.isExpiringSoon()) {
                    JSONObject refreshBody = new JSONObject();
                    refreshBody.put("refresh_token", sessionStore.getRefreshToken());
                    JSONObject refreshed = SupabaseApi.request(
                            "/auth/v1/token?grant_type=refresh_token", "POST", refreshBody, null);
                    access = refreshed.optString("access_token", "");
                    if (access.isEmpty()) throw new IllegalStateException("Session refresh failed");
                    sessionStore.save(refreshed.optJSONObject("user").optString("id"),
                            access, refreshed.optString("refresh_token", ""),
                            refreshed.optLong("expires_in", 3600));
                }
                JSONObject user = SupabaseApi.request("/auth/v1/user", "GET", null, access);
                String userId = user.optString("id", "");
                if (userId.isEmpty()) throw new IllegalStateException("Session is no longer valid");
                AppStore.userId = userId;
                runOnUiThread(this::openMain);
            } catch (Exception e) {
                Log.w(TAG, "Stored session is invalid", e);
                sessionStore.clear();
                runOnUiThread(this::showLogin);
            }
        }).start();
    }

    private void syncProfile(String userId, String email, String access) throws Exception {
        JSONObject profile = new JSONObject();
        profile.put("id", userId);
        profile.put("username", "user_" + userId.substring(0, 8));
        profile.put("display_name", email.substring(0, email.indexOf('@')));
        SupabaseApi.request("/rest/v1/profiles?on_conflict=id", "POST", profile, access,
                "resolution=merge-duplicates,return=minimal");
    }

    private void setBusy(boolean busy) {
        findViewById(R.id.btn_login).setEnabled(!busy);
        findViewById(R.id.btn_register).setEnabled(!busy);
    }

    private String readableError(Exception e) {
        String message = e.getMessage() == null ? "Sign in failed" : e.getMessage();
        if (message.contains("Invalid login credentials")) return "Email or password is incorrect";
        if (message.contains("Email not confirmed")) return "Confirm your email before signing in";
        if (message.contains("User already registered")) return "This email is already registered";
        return message;
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
