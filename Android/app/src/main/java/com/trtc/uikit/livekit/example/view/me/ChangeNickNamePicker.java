package com.trtc.uikit.livekit.example.view.me;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.trtc.uikit.livekit.example.data.SupabaseApi;
import com.trtc.uikit.livekit.example.store.AppStore;
import com.trtc.uikit.livekit.example.view.login.AuthSessionStore;

import org.json.JSONObject;

import java.util.regex.Pattern;

/** Supabase-backed nickname editor. */
public class ChangeNickNamePicker extends Dialog {
    private final android.widget.TextView target;
    private EditText nameInput;
    private Button saveButton;

    public ChangeNickNamePicker(@NonNull Context context, android.widget.TextView target) {
        super(context);
        this.target = target;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(com.trtc.uikit.livekit.example.R.layout.app_change_name);
        nameInput = findViewById(com.trtc.uikit.livekit.example.R.id.et_input);
        saveButton = findViewById(com.trtc.uikit.livekit.example.R.id.tv_save);
        nameInput.setText(target.getText());
        nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        findViewById(com.trtc.uikit.livekit.example.R.id.iv_back).setOnClickListener(v -> dismiss());
        findViewById(com.trtc.uikit.livekit.example.R.id.iv_clear_content).setOnClickListener(v -> nameInput.setText(""));
        saveButton.setOnClickListener(v -> save());
    }

    private void save() {
        String name = nameInput.getText().toString().trim();
        if (!Pattern.compile("^[a-zA-Z0-9_]{2,20}$").matcher(name).matches()) {
            nameInput.setError("Use 2-20 letters, numbers or underscores");
            return;
        }
        AuthSessionStore session = new AuthSessionStore(getContext());
        if (!session.hasSession()) {
            Toast.makeText(getContext(), "Please sign in again", Toast.LENGTH_SHORT).show();
            return;
        }
        saveButton.setEnabled(false);
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("display_name", name);
                SupabaseApi.request("/rest/v1/profiles?id=eq." + SupabaseApi.encode(session.getUserId()),
                        "PATCH", body, session.getAccessToken(), "return=minimal");
                AppStore.userName = name;
                target.post(() -> target.setText(name));
                dismiss();
            } catch (Exception e) {
                target.post(() -> {
                    saveButton.setEnabled(true);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
