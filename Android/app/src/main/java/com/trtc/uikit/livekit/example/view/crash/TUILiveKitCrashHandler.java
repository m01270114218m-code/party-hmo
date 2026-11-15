package com.trtc.uikit.livekit.example.view.crash;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.trtc.uikit.livekit.example.view.login.LoginActivity;

/** Lightweight crash fallback without a Tencent SDK dependency. */
public class TUILiveKitCrashHandler implements Thread.UncaughtExceptionHandler {
    private static volatile TUILiveKitCrashHandler instance;
    private Context context;
    private Thread.UncaughtExceptionHandler previous;

    private TUILiveKitCrashHandler() {}

    public static TUILiveKitCrashHandler getInstance() {
        if (instance == null) {
            synchronized (TUILiveKitCrashHandler.class) {
                if (instance == null) instance = new TUILiveKitCrashHandler();
            }
        }
        return instance;
    }

    public void initialize(android.app.Application application) {
        context = application.getApplicationContext();
        previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable error) {
        Log.e("PartyHMO", "Fatal application error", error);
        if (context != null) {
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        }
        if (previous != null) previous.uncaughtException(thread, error);
        else android.os.Process.killProcess(android.os.Process.myPid());
    }
}
