package com.example.jelajahkampusust;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Detail misi: informasi, mulai sesi (GPS), ambil foto (kamera, resolusi penuh via FileProvider),
 * kirim bukti ke server, dan akhiri sesi. Alur sesuai Use Case 1 & 2 pada PRD.
 */
public class MissionDetailActivity extends AppCompatActivity {

    private int missionId;
    private Models.Mission mission;
    private File photoFile;        // foto hasil kamera yang belum dikirim
    private File pendingFile;      // berkas tujuan kamera yang sedang berjalan

    private View scroll, stateBox, boxEvidence;
    private ProgressBar progress;
    private TextView tvTitle, tvBuilding, tvStatus, tvPoints, tvDesc, tvHint, tvState, tvPreviewInfo;
    private ImageView imgPreview;
    private Button btnMap, btnGuide, btnStart, btnPhoto, btnSend, btnEnd, btnRetry;

    private ApiClient api;
    private SessionManager session;
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private boolean busy = false;

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) {
                    checkGpsEnabled();
                } else {
                    Toast.makeText(this, "Izin lokasi ditolak. Pelacakan GPS tidak aktif.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else Toast.makeText(this, "Izin kamera ditolak, kamera tidak dapat digunakan.", Toast.LENGTH_LONG).show();
            });

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                if (ok && pendingFile != null && pendingFile.length() > 0) {
                    deleteQuietly(photoFile);
                    photoFile = pendingFile;
                    pendingFile = null;
                    showPhotoPreview();
                    updateButtons();
                } else {
                    // A2: kamera dibatalkan
                    deleteQuietly(pendingFile);
                    pendingFile = null;
                    Toast.makeText(this, "Pengambilan foto dibatalkan", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_detail);
        api = new ApiClient(this);
        session = new SessionManager(this);
        missionId = getIntent().getIntExtra("mission_id", 0);

        scroll = findViewById(R.id.scroll);
        stateBox = findViewById(R.id.stateBox);
        boxEvidence = findViewById(R.id.boxEvidence);
        progress = findViewById(R.id.progress);
        tvTitle = findViewById(R.id.tvTitle);
        tvBuilding = findViewById(R.id.tvBuilding);
        tvStatus = findViewById(R.id.tvStatus);
        tvPoints = findViewById(R.id.tvPoints);
        tvDesc = findViewById(R.id.tvDesc);
        tvHint = findViewById(R.id.tvHint);
        tvState = findViewById(R.id.tvState);
        tvPreviewInfo = findViewById(R.id.tvPreviewInfo);
        imgPreview = findViewById(R.id.imgPreview);
        btnMap = findViewById(R.id.btnMap);
        btnGuide = findViewById(R.id.btnGuide);
        btnStart = findViewById(R.id.btnStart);
        btnPhoto = findViewById(R.id.btnPhoto);
        btnSend = findViewById(R.id.btnSend);
        btnEnd = findViewById(R.id.btnEnd);
        btnRetry = findViewById(R.id.btnRetry);

        if (savedInstanceState != null) {
            String p = savedInstanceState.getString("photo");
            if (p != null && new File(p).exists()) photoFile = new File(p);
            String q = savedInstanceState.getString("pending");
            if (q != null) pendingFile = new File(q);
        }

        btnRetry.setOnClickListener(v -> load());
        btnMap.setOnClickListener(v -> {
            if (mission == null) return;
            Intent r = new Intent();
            r.putExtra("show_building_id", mission.buildingId);
            setResult(RESULT_OK, r);
            finish();
        });
        btnGuide.setOnClickListener(v -> {
            if (mission == null) return;
            Intent i = new Intent(this, BuildingDetailActivity.class);
            i.putExtra("building_id", mission.buildingId);
            startActivity(i);
        });
        btnStart.setOnClickListener(v -> startMission());
        btnPhoto.setOnClickListener(v -> takePhoto());
        btnSend.setOnClickListener(v -> sendEvidence());
        btnEnd.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Akhiri sesi")
                .setMessage("Pelacakan lokasi akan dihentikan. Bukti yang sudah dikirim tetap diproses admin.")
                .setPositiveButton("Akhiri", (d, w) -> endSession())
                .setNegativeButton("Batal", null)
                .show());

        load();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (photoFile != null) out.putString("photo", photoFile.getAbsolutePath());
        if (pendingFile != null) out.putString("pending", pendingFile.getAbsolutePath());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bg.shutdown();
    }

    // ---------------- Muat & tampilkan ----------------

    private void load() {
        stateBox.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        api.get("missions.php", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject json) {
                progress.setVisibility(View.GONE);
                try {
                    mission = null;
                    for (Models.Mission m : Models.Mission.list(json)) if (m.id == missionId) mission = m;
                } catch (Exception e) {
                    showFatal("Data misi tidak dapat dibaca", true);
                    return;
                }
                if (mission == null) { showFatal("Misi tidak ditemukan atau sudah tidak aktif.", false); return; }
                syncSession();
                render();
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
                if (mission == null) showFatal(message, true);
                else Toast.makeText(MissionDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFatal(String msg, boolean retry) {
        scroll.setVisibility(View.INVISIBLE);
        tvState.setText(msg);
        btnRetry.setVisibility(retry ? View.VISIBLE : View.GONE);
        stateBox.setVisibility(View.VISIBLE);
    }

    /** Samakan sesi lokal dengan keadaan server. */
    private void syncSession() {
        if (mission.activeSessionId > 0) {
            if (session.getActiveSessionId() != mission.activeSessionId)
                JelajahApp.tracking.startSession(mission.activeSessionId, mission.id, mission.title);
        } else if (session.getActiveMissionId() == mission.id) {
            JelajahApp.tracking.stopSession();
        }
    }

    private void render() {
        scroll.setVisibility(View.VISIBLE);
        tvTitle.setText(mission.title);
        tvBuilding.setText("Lokasi tujuan: " + mission.buildingName);
        tvPoints.setText("+" + mission.pointReward + " poin");
        tvDesc.setText(mission.description);

        tvStatus.setText(mission.statusLabel());
        GradientDrawable bgd = new GradientDrawable();
        bgd.setCornerRadius(40f);
        bgd.setColor(mission.statusColor(this));
        tvStatus.setBackground(bgd);
        tvStatus.setPadding(28, 8, 28, 8);

        btnMap.setEnabled(mission.hasLocation);

        // Info bukti terakhir dari server
        String hint = null;
        if (mission.hasEvidence) {
            switch (mission.evidenceStatus) {
                case "pending": hint = "Bukti dikirim " + mission.evidenceTime + ". Menunggu verifikasi admin."; break;
                case "accepted": hint = "Bukti diterima admin. Misi selesai."; break;
                case "rejected":
                    hint = "Bukti ditolak admin." + (mission.evidenceNote.isEmpty() ? "" : " Catatan: " + mission.evidenceNote)
                            + "\nSilakan ambil foto ulang dan kirim kembali.";
                    break;
            }
        }
        if (hint == null && mission.status.equals("in_progress"))
            hint = "Menuju lokasi tujuan, lalu ambil foto sebagai bukti kunjungan.";
        if (hint == null && mission.status.equals("not_started"))
            hint = "Tekan Mulai Misi untuk membuat sesi eksplorasi dan mengaktifkan pelacakan lokasi.";
        tvHint.setText(hint);
        tvHint.setVisibility(hint == null ? View.GONE : View.VISIBLE);

        updateButtons();
        showPhotoPreview();
    }

    private void updateButtons() {
        if (mission == null) return;
        boolean hasSession = mission.activeSessionId > 0;
        boolean completed = mission.status.equals("completed");
        boolean pending = mission.status.equals("pending");

        btnStart.setVisibility(!hasSession && !completed ? View.VISIBLE : View.GONE);
        btnStart.setText(mission.status.equals("rejected") ? "Mulai Ulang Misi" : "Mulai Misi");

        boolean canSubmit = hasSession && !completed && !pending;
        boxEvidence.setVisibility(canSubmit ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(canSubmit && photoFile != null && !busy);
        btnPhoto.setEnabled(!busy);
        btnPhoto.setText(photoFile == null ? "Ambil Foto" : "Ambil Ulang");

        btnEnd.setVisibility(hasSession ? View.VISIBLE : View.GONE);
        btnStart.setEnabled(!busy);
    }

    private void showPhotoPreview() {
        if (photoFile != null && photoFile.exists()) {
            Bitmap b = ImageUtils.decodeScaled(photoFile, 800);
            if (b != null) {
                imgPreview.setImageBitmap(b);
                imgPreview.setVisibility(View.VISIBLE);
                tvPreviewInfo.setText(photoFile.getName() + " - " + (photoFile.length() / 1024) + " KB (resolusi penuh)");
                return;
            }
        }
        imgPreview.setVisibility(View.GONE);
        tvPreviewInfo.setText("Belum ada foto. Tekan Ambil Foto.");
    }

    // ---------------- Mulai & akhiri sesi ----------------

    private void startMission() {
        setBusy(true);
        api.post("session_start.php", ApiClient.params("mission_id", String.valueOf(missionId)), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject json) {
                setBusy(false);
                JelajahApp.tracking.startSession(json.optInt("session_id"), mission.id, mission.title);
                Toast.makeText(MissionDetailActivity.this, json.optString("message", "Sesi dimulai"), Toast.LENGTH_SHORT).show();
                if (!JelajahApp.tracking.hasPermission()) {
                    new AlertDialog.Builder(MissionDetailActivity.this)
                            .setTitle("Izin lokasi")
                            .setMessage("Lokasi Anda hanya dibagikan selama sesi eksplorasi aktif. Izinkan akses lokasi untuk pelacakan?")
                            .setPositiveButton("Izinkan", (d, w) -> locationPermission.launch(new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}))
                            .setNegativeButton("Tidak", (d, w) -> Toast.makeText(MissionDetailActivity.this,
                                    "Pelacakan GPS tidak aktif.", Toast.LENGTH_LONG).show())
                            .show();
                } else {
                    checkGpsEnabled();
                }
                load();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                Toast.makeText(MissionDetailActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** A2 (use case 2): GPS tidak aktif -> minta pengguna mengaktifkan. */
    private void checkGpsEnabled() {
        if (!JelajahApp.tracking.isLocationEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("GPS tidak aktif")
                    .setMessage("Aktifkan layanan lokasi agar posisi Anda dapat dilacak selama sesi.")
                    .setPositiveButton("Buka pengaturan", (d, w) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Nanti", null)
                    .show();
        }
    }

    private void endSession() {
        setBusy(true);
        api.post("session_end.php", ApiClient.params("session_id", String.valueOf(mission.activeSessionId)), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject json) {
                setBusy(false);
                JelajahApp.tracking.stopSession();
                Toast.makeText(MissionDetailActivity.this, json.optString("message"), Toast.LENGTH_SHORT).show();
                load();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                Toast.makeText(MissionDetailActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---------------- Kamera ----------------

    private void takePhoto() {
        launchCamera();
    }

    private void launchCamera() {
        try {
            File base = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File dir = base != null ? new File(base, "evidence") : new File(getFilesDir(), "evidence");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("Folder foto tidak dapat dibuat");
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            pendingFile = File.createTempFile("bukti_" + ts + "_", ".jpg", dir);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pendingFile);
            cameraLauncher.launch(uri);
        } catch (ActivityNotFoundException e) {
            deleteQuietly(pendingFile);
            pendingFile = null;
            Toast.makeText(this, "Tidak ada aplikasi kamera di perangkat ini", Toast.LENGTH_LONG).show();
        } catch (IOException | IllegalArgumentException e) {
            Toast.makeText(this, "Gagal menyiapkan berkas foto: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ---------------- Kirim bukti ----------------

    private void sendEvidence() {
        if (photoFile == null || !photoFile.exists()) {
            // A3: foto tidak tersedia
            Toast.makeText(this, "Foto tidak tersedia, ambil foto terlebih dahulu.", Toast.LENGTH_LONG).show();
            return;
        }
        if (mission.activeSessionId <= 0) {
            Toast.makeText(this, "Mulai misi terlebih dahulu.", Toast.LENGTH_LONG).show();
            return;
        }
        setBusy(true);
        final File src = photoFile;
        final int sessionId = mission.activeSessionId;
        bg.execute(() -> {
            try {
                File compressed = ImageUtils.compressForUpload(src, new File(getCacheDir(), "upload"));
                runOnUiThread(() -> doUpload(compressed, sessionId));
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    Toast.makeText(this, "Foto tidak dapat diproses: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void doUpload(File compressed, int sessionId) {
        api.upload("evidence_upload.php", ApiClient.params("session_id", String.valueOf(sessionId)), compressed, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject json) {
                setBusy(false);
                deleteQuietly(compressed);
                deleteQuietly(photoFile);
                photoFile = null;
                Toast.makeText(MissionDetailActivity.this, json.optString("message", "Bukti terkirim"), Toast.LENGTH_LONG).show();
                load();
            }

            @Override
            public void onError(String message) {
                // A4/A5: jaringan atau server gagal. Foto dipertahankan agar bisa dikirim ulang.
                setBusy(false);
                deleteQuietly(compressed);
                Toast.makeText(MissionDetailActivity.this, "Bukti belum berhasil dikirim: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setBusy(boolean b) {
        busy = b;
        progress.setVisibility(b ? View.VISIBLE : View.GONE);
        updateButtons();
    }

    private static void deleteQuietly(File f) {
        if (f != null && f.exists()) //noinspection ResultOfMethodCallIgnored
            f.delete();
    }
}
