package com.trtc.uikit.livekit.example.view.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.trtc.uikit.livekit.example.BaseActivity;
import com.trtc.uikit.livekit.example.R;
import com.trtc.uikit.livekit.example.data.SupabaseApi;
import com.trtc.uikit.livekit.example.store.AppStore;
import com.trtc.uikit.livekit.example.view.main.MainActivity;

import org.json.JSONObject;

import java.util.Random;
import java.util.regex.Pattern;

public class ProfileActivity extends BaseActivity {
    private ImageView avatarView;
    private EditText nameEdit;
    private Button saveButton;
    private TextView tipsView;
    private String avatarUrl;

    private static final String[] AVATARS = {
            "https://liteav.sdk.qcloud.com/app/res/picture/voiceroom/avatar/user_avatar1.png",
            "https://liteav.sdk.qcloud.com/app/res/picture/voiceroom/avatar/user_avatar2.png",
            "https://liteav.sdk.qcloud.com/app/res/picture/voiceroom/avatar/user_avatar3.png",
            "https://liteav.sdk.qcloud.com/app/res/picture/voiceroom/avatar/user_avatar4.png",
            "https://liteav.sdk.qcloud.com/app/res/picture/voiceroom/avatar/user_avatar5.png"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_login_profile);
        avatarView = findViewById(R.id.iv_user_avatar);
        nameEdit = findViewById(R.id.et_user_name);
        tipsView = findViewById(R.id.tv_tips_user_name);
        saveButton = findViewById(R.id.tv_register);

        avatarUrl = AVATARS[new Random().nextInt(AVATARS.length)];
        avatarView.setImageResource(R.drawable.app_avatar);
        String suggested = "User" + (1000 + new Random().nextInt(9000));
        nameEdit.setText(suggested);
        nameEdit.setSelection(suggested.length());
        nameEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        nameEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean valid = Pattern.compile("^[a-zA-Z0-9_]{2,20}$").matcher(s).matches();
                tipsView.setTextColor(getResources().getColor(valid
                        ? R.color.app_text_color_hint : R.color.app_color_input_no_match));
                saveButton.setEnabled(valid);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String displayName = nameEdit.getText().toString().trim();
        if (TextUtils.isEmpty(displayName) || !Pattern.compile("^[a-zA-Z0-9_]{2,20}$").matcher(displayName).matches()) {
            nameEdit.setError("Use 2-20 letters, numbers or underscores");
            return;
        }
        AuthSessionStore session = new AuthSessionStore(this);
        if (!session.hasSession()) {
            Toast.makeText(this, "Please sign in again", Toast.LENGTH_LONG).show();
            return;
        }
        saveButton.setEnabled(false);
        new Thread(() -> {
            try {
                JSONObject update = new JSONObject();
                update.put("display_name", displayName);
                update.put("avatar_url", avatarUrl);
                SupabaseApi.request("/rest/v1/profiles?id=eq." + SupabaseApi.encode(session.getUserId()),
                        "PATCH", update, session.getAccessToken(), "return=minimal");
                AppStore.userName = displayName;
                AppStore.userAvatar = avatarUrl;
                runOnUiThread(() -> {
                    saveButton.setEnabled(true);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    saveButton.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
