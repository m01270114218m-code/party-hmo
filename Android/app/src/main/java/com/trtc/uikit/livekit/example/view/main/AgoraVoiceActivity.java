package com.trtc.uikit.livekit.example.view.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.trtc.uikit.livekit.example.BaseActivity;
import com.trtc.uikit.livekit.example.BuildConfig;
import com.trtc.uikit.livekit.example.R;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

public class AgoraVoiceActivity extends BaseActivity {
    private static final int REQ_RECORD_AUDIO = 1001;
    private RtcEngine rtcEngine;
    private EditText channelInput;
    private TextView statusView;
    private Button joinButton;

    private final IRtcEngineEventHandler eventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                statusView.setText("Connected to room: " + channel);
                joinButton.setText("Leave room");
            });
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            runOnUiThread(() -> {
                statusView.setText("Not connected");
                joinButton.setText("Join room");
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_agora_voice);

        channelInput = findViewById(R.id.et_channel);
        statusView = findViewById(R.id.tv_status);
        joinButton = findViewById(R.id.btn_join);

        if (BuildConfig.AGORA_APP_ID == null || BuildConfig.AGORA_APP_ID.isEmpty()) {
            statusView.setText("Agora App ID is missing");
            joinButton.setEnabled(false);
            return;
        }

        joinButton.setOnClickListener(v -> {
            if (rtcEngine == null) {
                if (hasMicrophonePermission()) {
                    initializeAndJoin();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
                }
            } else {
                rtcEngine.leaveChannel();
            }
        });
    }

    private boolean hasMicrophonePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void initializeAndJoin() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = BuildConfig.AGORA_APP_ID;
            config.mEventHandler = eventHandler;
            rtcEngine = RtcEngine.create(config);
            rtcEngine.enableAudio();

            String channel = channelInput.getText().toString().trim();
            if (channel.isEmpty()) channel = "party-hmo";

            ChannelMediaOptions options = new ChannelMediaOptions();
            options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            options.publishMicrophoneTrack = true;
            options.autoSubscribeAudio = true;

            // Production projects should obtain a short-lived token from a secure backend.
            rtcEngine.joinChannel(null, channel, 0, options);
            statusView.setText("Connecting...");
        } catch (Exception e) {
            statusView.setText("Agora error: " + e.getMessage());
            destroyEngine();
        }
    }

    @Override
    protected void onDestroy() {
        destroyEngine();
        super.onDestroy();
    }

    private void destroyEngine() {
        if (rtcEngine != null) {
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (rtcEngine != null) rtcEngine.leaveChannel();
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeAndJoin();
        } else if (requestCode == REQ_RECORD_AUDIO) {
            statusView.setText("Microphone permission is required");
        }
    }
}
