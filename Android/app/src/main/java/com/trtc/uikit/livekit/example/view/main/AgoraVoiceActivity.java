package com.trtc.uikit.livekit.example.view.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.trtc.uikit.livekit.example.BaseActivity;
import com.trtc.uikit.livekit.example.BuildConfig;
import com.trtc.uikit.livekit.example.R;
import com.trtc.uikit.livekit.example.data.SupabaseApi;
import com.trtc.uikit.livekit.example.view.login.AuthSessionStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.IRtcEngineEventHandler.RtcStats;

public class AgoraVoiceActivity extends BaseActivity {
    private static final String TAG = "AgoraVoiceActivity";
    private static final int REQ_RECORD_AUDIO = 1001;

    private RtcEngine rtcEngine;
    private EditText roomInput;
    private TextView statusView;
    private Button joinButton;
    private AuthSessionStore sessionStore;
    private String currentChannel;
    private int currentUid;

    private final IRtcEngineEventHandler eventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                statusView.setText("متصل بالغرفة: " + channel);
                joinButton.setText("مغادرة الغرفة");
                roomInput.setEnabled(false);
            });
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            runOnUiThread(() -> {
                statusView.setText("غير متصل");
                joinButton.setText("دخول الغرفة");
                roomInput.setEnabled(true);
            });
        }

        @Override
        public void onTokenPrivilegeWillExpire(String token) {
            renewAgoraToken();
        }

        @Override
        public void onError(int err) {
            runOnUiThread(() -> statusView.setText("Agora error: " + err));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_agora_voice);
        sessionStore = new AuthSessionStore(this);
        roomInput = findViewById(R.id.et_channel);
        statusView = findViewById(R.id.tv_status);
        joinButton = findViewById(R.id.btn_join);

        if (!sessionStore.hasSession()) {
            statusView.setText("سجل الدخول أولاً");
            joinButton.setEnabled(false);
            return;
        }
        if (TextUtils.isEmpty(BuildConfig.AGORA_APP_ID)) {
            statusView.setText("Agora App ID is missing");
            joinButton.setEnabled(false);
            return;
        }

        joinButton.setOnClickListener(v -> {
            if (rtcEngine != null) {
                rtcEngine.leaveChannel();
            } else if (hasMicrophonePermission()) {
                createOrJoinRoom();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            }
        });
    }

    private boolean hasMicrophonePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void createOrJoinRoom() {
        String roomName = roomInput.getText().toString().trim();
        if (roomName.isEmpty()) {
            roomInput.setError("اكتب اسم الغرفة");
            return;
        }
        setBusy(true);
        new Thread(() -> {
            try {
                String access = sessionStore.getAccessToken();
                String userId = sessionStore.getUserId();
                String query = "/rest/v1/rooms?select=id,title,agora_channel,owner_id&title=eq."
                        + SupabaseApi.encode(roomName) + "&status=eq.active&limit=1";
                JSONObject response = SupabaseApi.request(query, "GET", null, access);
                JSONArray rows = response.optJSONArray("items");
                String channel = null;
                String roomId;
                String roomOwner;
                if (rows != null && rows.length() > 0) {
                    JSONObject room = rows.getJSONObject(0);
                    roomId = room.getString("id");
                    roomOwner = room.optString("owner_id", "");
                    channel = room.optString("agora_channel", "");
                    if (channel.isEmpty()) {
                        channel = UUID.randomUUID().toString().replace("-", "");
                        JSONObject update = new JSONObject();
                        update.put("agora_channel", channel);
                        SupabaseApi.request("/rest/v1/rooms?id=eq." + roomId,
                                "PATCH", update, access, "return=minimal");
                    }
                } else {
                    channel = UUID.randomUUID().toString().replace("-", "");
                    JSONObject create = new JSONObject();
                    create.put("owner_id", userId);
                    create.put("title", roomName);
                    create.put("agora_channel", channel);
                    create.put("room_type", "public");
                    create.put("status", "active");
                    JSONObject created = SupabaseApi.request("/rest/v1/rooms", "POST", create, access,
                            "return=representation");
                    JSONArray createdRows = created.optJSONArray("items");
                    if (createdRows == null || createdRows.length() == 0) throw new IllegalStateException("تعذر إنشاء الغرفة");
                    JSONObject newRoom = createdRows.getJSONObject(0);
                    roomId = newRoom.getString("id");
                    roomOwner = userId;
                }
                JSONObject member = new JSONObject();
                member.put("room_id", roomId);
                member.put("user_id", userId);
                member.put("role", userId.equals(roomOwner) ? "owner" : "member");
                SupabaseApi.request("/rest/v1/room_members?on_conflict=room_id,user_id", "POST",
                        member, access, "resolution=merge-duplicates,return=minimal");
                int generatedUid = userId.hashCode() & 0x7fffffff;
                final int uid = generatedUid == 0 ? 1 : generatedUid;
                JSONObject tokenRequest = new JSONObject();
                tokenRequest.put("appId", BuildConfig.AGORA_APP_ID);
                tokenRequest.put("channel", channel);
                tokenRequest.put("uid", uid);
                JSONObject tokenResponse = SupabaseApi.request(
                        "/functions/v1/agora-token", "POST", tokenRequest, access);
                String token = tokenResponse.optString("rtcToken", "");
                if (token.isEmpty()) throw new IllegalStateException("لم يتم استلام Agora Token من الخادم");
                String finalChannel = channel;
                runOnUiThread(() -> initializeAndJoin(finalChannel, uid, token));
            } catch (Exception e) {
                Log.e(TAG, "Unable to create/join Supabase room", e);
                runOnUiThread(() -> {
                    setBusy(false);
                    statusView.setText(e.getMessage());
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void initializeAndJoin(String channel, int uid, String token) {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = BuildConfig.AGORA_APP_ID;
            config.mEventHandler = eventHandler;
            rtcEngine = RtcEngine.create(config);
            rtcEngine.enableAudio();
            rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            ChannelMediaOptions options = new ChannelMediaOptions();
            options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            options.publishMicrophoneTrack = true;
            options.autoSubscribeAudio = true;
            currentChannel = channel;
            currentUid = uid;
            rtcEngine.joinChannel(token, channel, uid, options);
            statusView.setText("جاري الاتصال...");
        } catch (Exception e) {
            statusView.setText("Agora error: " + e.getMessage());
            destroyEngine();
            setBusy(false);
        }
    }

    private void renewAgoraToken() {
        if (TextUtils.isEmpty(currentChannel) || currentUid == 0) return;
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("appId", BuildConfig.AGORA_APP_ID);
                body.put("channel", currentChannel);
                body.put("uid", currentUid);
                JSONObject result = SupabaseApi.request("/functions/v1/agora-token", "POST",
                        body, sessionStore.getAccessToken());
                String token = result.optString("rtcToken", "");
                if (!token.isEmpty() && rtcEngine != null) rtcEngine.renewToken(token);
            } catch (Exception e) {
                Log.e(TAG, "Agora token renewal failed", e);
                runOnUiThread(() -> statusView.setText("تعذر تجديد رمز الاتصال"));
            }
        }).start();
    }

    private void setBusy(boolean busy) {
        joinButton.setEnabled(!busy);
        if (busy) statusView.setText("جاري تجهيز الغرفة...");
    }

    private void destroyEngine() {
        if (rtcEngine != null) {
            RtcEngine.destroy();
            rtcEngine = null;
        }
        currentChannel = null;
        currentUid = 0;
    }

    @Override
    protected void onDestroy() {
        if (rtcEngine != null) rtcEngine.leaveChannel();
        destroyEngine();
        super.onDestroy();
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
            createOrJoinRoom();
        } else if (requestCode == REQ_RECORD_AUDIO) {
            statusView.setText("يلزم السماح باستخدام الميكروفون");
        }
    }
}
