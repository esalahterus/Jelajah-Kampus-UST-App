package com.example.jelajahkampusust;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

/** FR-02: login. Jika sudah login langsung ke beranda. */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progress;
    private TextView tvError;
    private SessionManager session;
    private ApiClient api;
    private static final int REQUEST_LOCAL_NETWORK = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        if (session.isLoggedIn()) {
            openMain();
            return;
        }
        setContentView(R.layout.activity_login);
        api = new ApiClient(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progress = findViewById(R.id.progress);
        tvError = findViewById(R.id.tvError);
        ((TextView) findViewById(R.id.tvServer)).setText("Server: " + Config.BASE_URL);

        btnLogin.setOnClickListener(v -> doLogin());
        findViewById(R.id.btnGoRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void doLogin() {
        String email = text(etEmail);
        String pass = etPassword.getText() == null ? "" : etPassword.getText().toString();

        if (email.isEmpty() || pass.isEmpty()) { showError("Email dan password wajib diisi"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { showError("Format email tidak valid"); return; }

        if (needsLocalNetworkPermission()
                && checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_LOCAL_NETWORK},
                    REQUEST_LOCAL_NETWORK
            );
            return;
        }

        setLoading(true);
        api.post("login.php", ApiClient.params("email", email, "password", pass), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    JSONObject u = json.getJSONObject("user");
                    session.saveLogin(json.getString("token"), u.optString("name"), u.optString("email"),
                            u.optString("nim"), u.optString("program_study"));
                    openMain();
                } catch (Exception e) {
                    setLoading(false);
                    showError("Respons login tidak valid");
                }
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                showError(message);
            }
        });
    }

    private boolean needsLocalNetworkPermission() {
        return Build.VERSION.SDK_INT >= 37 && Config.usesLocalBackend();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCAL_NETWORK) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (granted) {
                doLogin();
            } else {
                showError(
                        "Izin jaringan lokal diperlukan untuk terhubung ke server Laragon."
                );
            }
        }
    }
    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean on) {
        progress.setVisibility(on ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!on);
        if (on) tvError.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private static String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
