package com.example.jelajahkampusust;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

/** FR-01: registrasi akun mahasiswa. */
public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etNim, etProdi, etEmail, etPassword;
    private Button btnRegister;
    private ProgressBar progress;
    private TextView tvError;
    private ApiClient api;
    private static final int REQUEST_LOCAL_NETWORK = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        api = new ApiClient(this);

        etName = findViewById(R.id.etName);
        etNim = findViewById(R.id.etNim);
        etProdi = findViewById(R.id.etProdi);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progress);
        tvError = findViewById(R.id.tvError);

        btnRegister.setOnClickListener(v -> doRegister());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void doRegister() {
        String name = t(etName), nim = t(etNim), prodi = t(etProdi), email = t(etEmail);
        String pass = etPassword.getText() == null ? "" : etPassword.getText().toString();

        if (name.isEmpty() || nim.isEmpty() || prodi.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showError("Semua field wajib diisi"); return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { showError("Format email tidak valid"); return; }
        if (!nim.matches("[0-9A-Za-z]{5,30}")) { showError("NIM harus 5-30 karakter huruf/angka"); return; }
        if (pass.length() < 6) { showError("Password minimal 6 karakter"); return; }

        if (needsLocalNetworkPermission()
                && checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_LOCAL_NETWORK},
                    REQUEST_LOCAL_NETWORK
            );
            return;
        }

        progress.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        tvError.setVisibility(View.GONE);

        api.post("register.php", ApiClient.params("name", name, "nim", nim, "program_study", prodi,
                "email", email, "password", pass), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject json) {
                Toast.makeText(RegisterActivity.this, json.optString("message", "Registrasi berhasil"), Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
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
                doRegister();
            } else {
                showError(
                        "Izin jaringan lokal diperlukan untuk terhubung ke server Laragon."
                );
            }
        }
    }
    private void showError(String m) {
        tvError.setText(m);
        tvError.setVisibility(View.VISIBLE);
    }

    private static String t(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
