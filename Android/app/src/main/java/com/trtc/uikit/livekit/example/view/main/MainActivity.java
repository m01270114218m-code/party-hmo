package com.trtc.uikit.livekit.example.view.main;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.trtc.uikit.livekit.example.BaseActivity;
import com.trtc.uikit.livekit.example.data.SupabaseApi;
import com.trtc.uikit.livekit.example.view.login.AuthSessionStore;
import com.trtc.uikit.livekit.example.view.me.MeActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends BaseActivity {
    private LinearLayout root;
    private LinearLayout content;
    private AuthSessionStore session;
    private final List<Room> rooms = new ArrayList<>();
    private final Set<String> joinedRoomIds = new HashSet<>();
    private Spinner roomSpinner;
    private EditText messageInput;
    private TextView pageTitle;

    private static class Room {
        final String id;
        final String title;
        Room(String id, String title) { this.id = id; this.title = title; }
        @Override public String toString() { return title; }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new AuthSessionStore(this);
        buildShell();
        showRooms();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(0xff202431);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setPadding(dp(12), dp(10), dp(12), dp(10));
        return v;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        return b;
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xfff7f8fc);
        pageTitle = text("الغرف", 21, true);
        pageTitle.setGravity(Gravity.CENTER_VERTICAL);
        pageTitle.setBackgroundColor(0xffffffff);
        root.addView(pageTitle, new LinearLayout.LayoutParams(-1, dp(58)));

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(10), dp(12), dp(18));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(0xffffffff);
        String[] labels = {"الغرف", "الرسائل", "أنا", "المزيد"};
        for (int i = 0; i < labels.length; i++) {
            final int tab = i;
            Button b = button(labels[i]);
            b.setAllCaps(false);
            b.setTextSize(12);
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(58), 1));
            b.setOnClickListener(v -> {
                if (tab == 0) showRooms();
                else if (tab == 1) showMessages();
                else if (tab == 2) startActivity(new Intent(this, MeActivity.class));
                else showMore();
            });
        }
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(58)));
        setContentView(root);
    }

    private void showRooms() {
        pageTitle.setText("الغرف");
        content.removeAllViews();
        content.addView(text("غرف صوتية عامة", 18, true));
        Button refresh = button("تحديث الغرف");
        content.addView(refresh);
        refresh.setOnClickListener(v -> loadRooms());
        content.addView(text("اضغط على انضمام لدخول الغرفة الصوتية.", 14, false));
        loadRooms();
    }

    private void loadRooms() {
        content.removeAllViews();
        content.addView(text("جاري تحميل الغرف...", 15, false));
        new Thread(() -> {
            try {
                JSONObject response = SupabaseApi.request(
                        "/rest/v1/rooms?select=id,title,description,created_at&status=eq.active&room_type=eq.public&order=created_at.desc&limit=100",
                        "GET", null, session.getAccessToken());
                JSONArray rows = response.optJSONArray("items");
                List<Room> found = new ArrayList<>();
                if (rows != null) for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.getJSONObject(i);
                    found.add(new Room(row.getString("id"), row.optString("title", "غرفة صوتية")));
                }
                runOnUiThread(() -> renderRooms(found));
            } catch (Exception e) {
                runOnUiThread(() -> showError("تعذر تحميل الغرف: " + e.getMessage()));
            }
        }).start();
    }

    private void renderRooms(List<Room> found) {
        rooms.clear();
        rooms.addAll(found);
        content.removeAllViews();
        content.addView(text("الغرف المتاحة (" + found.size() + ")", 18, true));
        Button refresh = button("تحديث");
        content.addView(refresh);
        refresh.setOnClickListener(v -> loadRooms());
        if (found.isEmpty()) {
            content.addView(text("لا توجد غرف نشطة حالياً. يمكنك إنشاء غرفة من زر إنشاء غرفة.", 15, false));
        }
        for (Room room : found) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(0xffffffff);
            card.setPadding(dp(10), dp(8), dp(10), dp(8));
            TextView name = text(room.title, 17, true);
            card.addView(name);
            Button join = button("انضمام للغرفة");
            card.addView(join);
            join.setOnClickListener(v -> joinRoom(room));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, dp(6), 0, dp(6));
            content.addView(card, lp);
        }
        Button create = button("إنشاء غرفة جديدة");
        content.addView(create);
        create.setOnClickListener(v -> createRoomDialog());
    }

    private void createRoomDialog() {
        EditText input = new EditText(this);
        input.setHint("اسم الغرفة");
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("إنشاء غرفة صوتية")
                .setView(input)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("إنشاء", (d, w) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) { showError("اكتب اسم الغرفة"); return; }
                    new Thread(() -> {
                        try {
                            JSONObject body = new JSONObject();
                            body.put("owner_id", session.getUserId());
                            body.put("title", title);
                            body.put("room_type", "public");
                            body.put("status", "active");
                            JSONObject created = SupabaseApi.request("/rest/v1/rooms", "POST", body,
                                    session.getAccessToken(), "return=representation");
                            JSONArray rows = created.optJSONArray("items");
                            if (rows == null || rows.length() == 0) throw new Exception("لم يتم إنشاء الغرفة");
                            JSONObject r = rows.getJSONObject(0);
                            Room room = new Room(r.getString("id"), r.optString("title", title));
                            joinRoom(room);
                        } catch (Exception e) {
                            runOnUiThread(() -> showError("فشل إنشاء الغرفة: " + e.getMessage()));
                        }
                    }).start();
                }).show();
    }

    private void joinRoom(Room room) {
        new Thread(() -> {
            try {
                JSONObject member = new JSONObject();
                member.put("room_id", room.id);
                member.put("user_id", session.getUserId());
                member.put("role", "member");
                SupabaseApi.request("/rest/v1/room_members?on_conflict=room_id,user_id", "POST",
                        member, session.getAccessToken(), "resolution=merge-duplicates,return=minimal");
                runOnUiThread(() -> {
                    Intent intent = new Intent(this, AgoraVoiceActivity.class);
                    intent.putExtra("room_name", room.title);
                    startActivity(intent);
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError("تعذر الانضمام للغرفة: " + e.getMessage()));
            }
        }).start();
    }

    private void showMessages() {
        pageTitle.setText("الرسائل");
        content.removeAllViews();
        content.addView(text("رسائل الغرف التي انضممت إليها", 18, true));
        content.addView(text("جاري تحميل الغرف والرسائل...", 14, false));
        new Thread(() -> {
            try {
                JSONObject roomResult = SupabaseApi.request(
                        "/rest/v1/rooms?select=id,title&status=eq.active&order=created_at.desc&limit=100",
                        "GET", null, session.getAccessToken());
                JSONObject memberResult = SupabaseApi.request(
                        "/rest/v1/room_members?select=room_id&user_id=eq." + SupabaseApi.encode(session.getUserId()),
                        "GET", null, session.getAccessToken());
                Set<String> memberIds = new HashSet<>();
                JSONArray members = memberResult.optJSONArray("items");
                if (members != null) for (int i = 0; i < members.length(); i++)
                    memberIds.add(members.getJSONObject(i).optString("room_id"));
                List<Room> joined = new ArrayList<>();
                JSONArray all = roomResult.optJSONArray("items");
                if (all != null) for (int i = 0; i < all.length(); i++) {
                    JSONObject r = all.getJSONObject(i);
                    if (memberIds.contains(r.optString("id")))
                        joined.add(new Room(r.getString("id"), r.optString("title", "غرفة")));
                }
                runOnUiThread(() -> renderMessages(joined));
            } catch (Exception e) {
                runOnUiThread(() -> showError("تعذر تحميل الرسائل: " + e.getMessage()));
            }
        }).start();
    }

    private void renderMessages(List<Room> joined) {
        content.removeAllViews();
        content.addView(text("اختر غرفة لعرض وإرسال الرسائل", 16, true));
        if (joined.isEmpty()) {
            content.addView(text("لا توجد غرف انضممت إليها بعد. افتح تبويب الغرف وانضم إلى غرفة أولاً.", 14, false));
            return;
        }
        roomSpinner = new Spinner(this);
        roomSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, joined));
        content.addView(roomSpinner);
        messageInput = new EditText(this);
        messageInput.setHint("اكتب رسالتك...");
        content.addView(messageInput);
        Button send = button("إرسال");
        content.addView(send);
        LinearLayout history = new LinearLayout(this);
        history.setOrientation(LinearLayout.VERTICAL);
        content.addView(text("آخر الرسائل", 17, true));
        content.addView(history);
        Runnable reload = () -> loadRoomMessages(joined.get(roomSpinner.getSelectedItemPosition()), history);
        roomSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                loadRoomMessages(joined.get(pos), history);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        send.setOnClickListener(v -> {
            String bodyText = messageInput.getText().toString().trim();
            if (bodyText.isEmpty()) return;
            Room selected = joined.get(roomSpinner.getSelectedItemPosition());
            sendMessage(selected, bodyText, history);
        });
    }

    private void loadRoomMessages(Room room, LinearLayout history) {
        history.removeAllViews();
        new Thread(() -> {
            try {
                JSONObject response = SupabaseApi.request(
                        "/rest/v1/chat_messages?select=body,sender_id,created_at&room_id=eq."
                                + SupabaseApi.encode(room.id) + "&deleted_at=is.null&order=created_at.desc&limit=50",
                        "GET", null, session.getAccessToken());
                JSONArray rows = response.optJSONArray("items");
                runOnUiThread(() -> {
                    history.removeAllViews();
                    if (rows == null || rows.length() == 0) {
                        history.addView(text("لا توجد رسائل في هذه الغرفة حتى الآن.", 14, false));
                        return;
                    }
                    for (int i = rows.length() - 1; i >= 0; i--) {
                        JSONObject item = rows.optJSONObject(i);
                        String sender = item.optString("sender_id", "");
                        String body = item.optString("body", "");
                        history.addView(text((sender.equals(session.getUserId()) ? "أنت: " : "عضو: ") + body,
                                14, false));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    history.removeAllViews();
                    history.addView(text("تعذر قراءة الرسائل: " + e.getMessage(), 14, false));
                });
            }
        }).start();
    }

    private void sendMessage(Room room, String bodyText, LinearLayout history) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("room_id", room.id);
                body.put("sender_id", session.getUserId());
                body.put("message_type", "text");
                body.put("body", bodyText);
                SupabaseApi.request("/rest/v1/chat_messages", "POST", body, session.getAccessToken(),
                        "return=minimal");
                runOnUiThread(() -> {
                    messageInput.setText("");
                    loadRoomMessages(room, history);
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError("فشل إرسال الرسالة: " + e.getMessage()));
            }
        }).start();
    }

    private void showMore() {
        pageTitle.setText("المزيد");
        content.removeAllViews();
        content.addView(text("خدمات Party HMO", 18, true));
        Button profile = button("الملف الشخصي");
        Button rooms = button("الغرف الصوتية");
        Button messages = button("الرسائل");
        content.addView(profile);
        content.addView(rooms);
        content.addView(messages);
        profile.setOnClickListener(v -> startActivity(new Intent(this, MeActivity.class)));
        rooms.setOnClickListener(v -> showRooms());
        messages.setOnClickListener(v -> showMessages());
        content.addView(text("باقي أقسام التطبيق مثل العائلات والمحفظة والهدايا والإشعارات تحتاج استكمال ربطها قبل عرضها كوظائف جاهزة.", 14, false));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
