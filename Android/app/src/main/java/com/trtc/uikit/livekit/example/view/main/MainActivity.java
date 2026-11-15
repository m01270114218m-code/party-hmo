package com.trtc.uikit.livekit.example.view.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.trtc.uikit.livekit.example.BaseActivity;
import com.trtc.uikit.livekit.example.R;
import com.trtc.uikit.livekit.example.view.me.MeActivity;

public class MainActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_main);
        TextView title = findViewById(R.id.tv_title);
        title.setText("Party HMO");
        findViewById(R.id.video_live).setVisibility(View.GONE);
        findViewById(R.id.btn_multi_function).setVisibility(View.GONE);
        findViewById(R.id.voice_room).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AgoraVoiceActivity.class)));
        findViewById(R.id.btn_profile).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MeActivity.class)));
    }
}
