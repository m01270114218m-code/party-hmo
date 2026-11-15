package com.trtc.uikit.livekit.example.view.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

import com.trtc.uikit.livekit.example.BaseActivity;
import com.trtc.uikit.livekit.example.R;
import com.trtc.uikit.livekit.example.store.AppStore;
import com.trtc.uikit.livekit.example.view.main.MainActivity;
import com.trtc.uikit.livekit.example.BuildConfig;

import org.json.JSONObject;
import java.io.IOException;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends BaseActivity {
    private static final String TAG = "LoginActivity";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient();
    private EditText emailEdit;
    private EditText passwordEdit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_login);
        emailEdit = findViewById(R.id.et_email);
        passwordEdit = findViewById(R.id.et_password);
        findViewById(R.id.btn_login).setOnClickListener(v -> authenticate(false));
        findViewById(R.id.btn_register).setOnClickListener(v -> authenticate(true));
    }

    private void authenticate(boolean register) {
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            emailEdit.setError("Email and password are required");
            return;
        }
        if (TextUtils.isEmpty(BuildConfig.SUPABASE_URL) ||
                TextUtils.isEmpty(BuildConfig.SUPABASE_PUBLISHABLE_KEY)) {
            emailEdit.setError("Supabase configuration is missing");
            return;
        }
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);
                String endpoint = BuildConfig.SUPABASE_URL + "/auth/v1/" +
                        (register ? "signup" : "token?grant_type=password");
                Request request = new Request.Builder()
                        .url(endpoint)
                        .addHeader("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                        .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                        .post(RequestBody.create(body.toString(), JSON))
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    String responseText = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> passwordEdit.setError("Authentication failed"));
                        Log.e(TAG, responseText);
                        return;
                    }
                    JSONObject result = new JSONObject(responseText);
                    String userId = result.has("user") && !result.isNull("user")
                            ? result.getJSONObject("user").optString("id", UUID.randomUUID().toString())
                            : UUID.randomUUID().toString();
                    AppStore.userId = userId;
                    runOnUiThread(this::startMainActivity);
                }
            } catch (Exception e) {
                Log.e(TAG, "Supabase authentication failed", e);
                runOnUiThread(() -> passwordEdit.setError("Network or authentication error"));
            }
        }).start();
    }

    private void startMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}