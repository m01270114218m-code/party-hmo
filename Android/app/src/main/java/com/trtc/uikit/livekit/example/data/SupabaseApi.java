package com.trtc.uikit.livekit.example.data;

import com.trtc.uikit.livekit.example.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Small REST client for Supabase Auth, PostgREST and Edge Functions. */
public final class SupabaseApi {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient();

    private SupabaseApi() {}

    public static JSONObject request(String path, String method, JSONObject body, String accessToken)
            throws IOException {
        return request(path, method, body, accessToken, null);
    }

    public static JSONObject request(String path, String method, JSONObject body, String accessToken,
                                      String prefer) throws IOException {
        if (BuildConfig.SUPABASE_URL == null || BuildConfig.SUPABASE_URL.isEmpty()
                || BuildConfig.SUPABASE_PUBLISHABLE_KEY == null
                || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isEmpty()) {
            throw new IOException("Supabase is not configured");
        }
        RequestBody requestBody = body == null ? null : RequestBody.create(body.toString(), JSON);
        Request.Builder builder = new Request.Builder()
                .url(BuildConfig.SUPABASE_URL + path)
                .addHeader("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                .addHeader("Authorization", "Bearer " + (accessToken == null || accessToken.isEmpty()
                        ? BuildConfig.SUPABASE_PUBLISHABLE_KEY : accessToken))
                .addHeader("Accept", "application/json");
        if (prefer != null) builder.addHeader("Prefer", prefer);
        builder.method(method, requestBody);
        try (Response response = CLIENT.newCall(builder.build()).execute()) {
            String text = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                String message = text;
                try {
                    JSONObject error = new JSONObject(text);
                    message = error.optString("msg", error.optString("message",
                            error.optString("error_description", error.optString("error", text))));
                } catch (Exception ignored) {}
                throw new IOException(message.isEmpty() ? "Supabase request failed (" + response.code() + ")" : message);
            }
            if (text.isEmpty()) return new JSONObject();
            try {
                if (text.startsWith("[")) {
                    JSONObject result = new JSONObject();
                    result.put("items", new JSONArray(text));
                    return result;
                }
                return new JSONObject(text);
            } catch (org.json.JSONException e) {
                throw new IOException("Invalid JSON response from Supabase", e);
            }
        }
    }

    public static String encode(String value) throws IOException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }
}
