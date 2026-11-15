package com.trtc.uikit.livekit.example.view.me;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.trtc.uikit.livekit.example.BaseActivity;
import com.trtc.uikit.livekit.example.R;
import com.trtc.uikit.livekit.example.data.SupabaseApi;
import com.trtc.uikit.livekit.example.view.login.AuthSessionStore;
import com.trtc.uikit.livekit.example.view.login.LoginActivity;

import org.json.JSONArray;
import org.json.JSONObject;

public class MeActivity extends BaseActivity {
    private ImageView logoutView;
    private ImageView backView;
    private ImageView avatarView;
    private TextView nicknameView;
    private TextView followingCountView;
    private TextView followersCountView;
    private AuthSessionStore session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_fragment_me);
        session = new AuthSessionStore(this);
        logoutView = findViewById(R.id.iv_logout);
        backView = findViewById(R.id.iv_back);
        avatarView = findViewById(R.id.iv_avatar);
        nicknameView = findViewById(R.id.tv_nickname);
        followingCountView = findViewById(R.id.tv_likes_count);
        followersCountView = findViewById(R.id.tv_follows_count);
        avatarView.setImageResource(R.drawable.app_avatar);
        nicknameView.setText("Party HMO user");
        followingCountView.setText("0");
        followersCountView.setText("0");
        backView.setOnClickListener(v -> finish());
        logoutView.setOnClickListener(v -> confirmLogout());
        loadProfile();
    }

    private void loadProfile() {
        if (!session.hasSession()) return;
        new Thread(() -> {
            try {
                JSONObject result = SupabaseApi.request(
                        "/rest/v1/profiles?select=display_name,avatar_url&id=eq."
                                + SupabaseApi.encode(session.getUserId()) + "&limit=1",
                        "GET", null, session.getAccessToken());
                JSONArray rows = result.optJSONArray("items");
                if (rows != null && rows.length() > 0) {
                    JSONObject profile = rows.getJSONObject(0);
                    String name = profile.optString("display_name", "Party HMO user");
                    runOnUiThread(() -> nicknameView.setText(name));
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("تسجيل الخروج")
                .setMessage("هل تريد تسجيل الخروج من حسابك؟")
                .setNegativeButton("إلغاء", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("خروج", (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        new Thread(() -> {
            try {
                if (session.hasSession()) {
                    SupabaseApi.request("/auth/v1/logout", "POST", null, session.getAccessToken());
                }
            } catch (Exception ignored) {
            } finally {
                session.clear();
                runOnUiThread(() -> {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            }
        }).start();
    }
}
