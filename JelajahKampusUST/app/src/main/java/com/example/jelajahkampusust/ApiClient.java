package com.example.jelajahkampusust;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {

    public interface Callback {
        void onSuccess(JSONObject json);
        void onError(String message);
    }

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int TIMEOUT_MS = 15000;

    private final Context app;
    private final SessionManager session;

    public ApiClient(Context context) {
        this.app = context.getApplicationContext();
        this.session = new SessionManager(app);
    }

    public void get(String endpoint, Map<String, String> params, Callback cb) {
        StringBuilder url = new StringBuilder(Config.API + endpoint);
        if (params != null && !params.isEmpty()) url.append('?').append(encode(params));
        run("GET", url.toString(), null, null, null, cb);
    }

    public void post(String endpoint, Map<String, String> params, Callback cb) {
        run("POST", Config.API + endpoint, params, null, null, cb);
    }

    public void upload(String endpoint, Map<String, String> params, File file, Callback cb) {
        run("POST", Config.API + endpoint, params, "photo", file, cb);
    }

    public static Map<String, String> params(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    // ------------------------------------------------------------------

    private void run(String method, String url, Map<String, String> params,
                     String fileField, File file, Callback cb) {
        EXEC.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(file != null ? 60000 : TIMEOUT_MS);
                conn.setRequestMethod(method);
                conn.setRequestProperty("Accept", "application/json");
                if (session.isLoggedIn()) conn.setRequestProperty("X-Auth-Token", session.getToken());

                if ("POST".equals(method)) {
                    conn.setDoOutput(true);
                    if (file != null) writeMultipart(conn, params, fileField, file);
                    else writeForm(conn, params);
                }

                int code = conn.getResponseCode();
                InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                String body = is == null ? "" : readAll(is);

                JSONObject json;
                try {
                    json = new JSONObject(body);
                } catch (Exception parse) {
                    postError(cb, "Respons server tidak valid (HTTP " + code + ")");
                    return;
                }

                if (code == 401) {
                    forceLogout();
                    postError(cb, json.optString("message", "Sesi berakhir, silakan login ulang"));
                    return;
                }
                if (json.optBoolean("success", false)) {
                    MAIN.post(() -> cb.onSuccess(json));
                } else {
                    postError(cb, json.optString("message", "Permintaan gagal"));
                }
            } catch (SocketTimeoutException e) {
                postError(cb, "Koneksi timeout. Periksa jaringan dan server.");
            } catch (UnknownHostException e) {
                postError(cb, "Server tidak ditemukan. Periksa alamat server.");
            } catch (IOException e) {
                postError(cb, "Tidak dapat terhubung ke server. Pastikan Laragon (Apache & MySQL) berjalan dan jaringan aktif.");
            } catch (Exception e) {
                postError(cb, "Terjadi kesalahan: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void postError(Callback cb, String msg) {
        MAIN.post(() -> cb.onError(msg));
    }

    private void forceLogout() {
        session.clear();
        MAIN.post(() -> {
            if (JelajahApp.tracking != null) JelajahApp.tracking.stopSession();
            Intent i = new Intent(app, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            app.startActivity(i);
        });
    }

    private void writeForm(HttpURLConnection conn, Map<String, String> params) throws IOException {
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        byte[] data = (params == null ? "" : encode(params)).getBytes("UTF-8");
        conn.setFixedLengthStreamingMode(data.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(data);
        }
    }

    private void writeMultipart(HttpURLConnection conn, Map<String, String> params,
                                String fileField, File file) throws IOException {
        String boundary = "----JelajahKampus" + UUID.randomUUID();
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setChunkedStreamingMode(16 * 1024);
        String crlf = "\r\n";
        try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
            if (params != null) {
                for (Map.Entry<String, String> e : params.entrySet()) {
                    out.writeBytes("--" + boundary + crlf);
                    out.writeBytes("Content-Disposition: form-data; name=\"" + e.getKey() + "\"" + crlf);
                    out.writeBytes("Content-Type: text/plain; charset=UTF-8" + crlf + crlf);
                    out.write(e.getValue().getBytes("UTF-8"));
                    out.writeBytes(crlf);
                }
            }
            out.writeBytes("--" + boundary + crlf);
            out.writeBytes("Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + file.getName() + "\"" + crlf);
            out.writeBytes("Content-Type: image/jpeg" + crlf + crlf);
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            out.writeBytes(crlf + "--" + boundary + "--" + crlf);
            out.flush();
        }
    }

    private static String encode(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (sb.length() > 0) sb.append('&');
                sb.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=')
                        .append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), "UTF-8"));
            }
        } catch (IOException ignored) { }
        return sb.toString();
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        return bos.toString("UTF-8");
    }
}
