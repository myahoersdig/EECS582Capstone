package com.example.eecs582capstone;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseAuthClient {

    private static final String SUPABASE_URL = "https://slkwhundqgpughedzpav.supabase.co";
    private static final String ANON_KEY = "sb_publishable_U41f7HVizjbJaqKtoSDctA_Uw2CMpYW";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AuthCallback {
        void onSuccess(String email, String firstName, String lastName);
        void onError(String message);
    }

    public static void signUp(String email, String password, String firstName, String lastName, AuthCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject metadata = new JSONObject();
                metadata.put("first_name", firstName);
                metadata.put("last_name", lastName);

                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);
                body.put("data", metadata);

                JSONObject response = post("/auth/v1/signup", body.toString());

                if (response.has("error_code")) {
                    String msg = response.optString("msg", "Sign up failed");
                    mainHandler.post(() -> callback.onError(msg));
                } else if (response.has("id") || response.has("access_token")) {
                    mainHandler.post(() -> callback.onSuccess(email, firstName, lastName));
                } else {
                    mainHandler.post(() -> callback.onError("Unexpected response. Please try again."));
                }
            } catch (Exception e) {
                String msg = e.getMessage();
                mainHandler.post(() -> callback.onError(msg != null ? msg : "Network error"));
            }
        });
    }

    public static void signIn(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);

                JSONObject response = post("/auth/v1/token?grant_type=password", body.toString());

                if (response.has("access_token")) {
                    JSONObject user = response.optJSONObject("user");
                    JSONObject meta = user != null ? user.optJSONObject("user_metadata") : null;
                    String firstName = meta != null ? meta.optString("first_name", "") : "";
                    String lastName = meta != null ? meta.optString("last_name", "") : "";
                    mainHandler.post(() -> callback.onSuccess(email, firstName, lastName));
                } else {
                    String msg = response.optString("error_description",
                            response.optString("msg", "Invalid email or password"));
                    mainHandler.post(() -> callback.onError(msg));
                }
            } catch (Exception e) {
                String msg = e.getMessage();
                mainHandler.post(() -> callback.onError(msg != null ? msg : "Network error"));
            }
        });
    }

    private static JSONObject post(String path, String bodyJson) throws Exception {
        URL url = new URL(SUPABASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("apikey", ANON_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyJson.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) {
            return new JSONObject("{\"error_code\": \"no_response\", \"msg\": \"No response from server\"}");
        }

        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
        }
        is.close();

        return new JSONObject(sb.toString());
    }
}
