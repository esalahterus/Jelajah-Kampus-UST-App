package com.example.jelajahkampusust;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuildingDetailActivity extends AppCompatActivity {

    private int buildingId;
    private Models.Building building;
    private final List<Models.Media> audios = new ArrayList<>();
    private final List<Models.Media> videos = new ArrayList<>();

    private View scroll, stateBox, audioBox, videoBox;
    private ProgressBar progress;
    private TextView tvState, tvAudioTime, tvVideoState;
    private Spinner spAudio, spVideo;
    private SeekBar seekAudio;
    private Button btnPlay, btnPause, btnStop, btnVideoPlay;
    private VideoView videoView;

    private MediaPlayer player;
    private boolean playerPrepared = false;
    private boolean startWhenPrepared = false;
    private int audioIndexLoaded = -1;
    private int videoIndexLoaded = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ApiClient api;

    private final Runnable seekTask = new Runnable() {
        @Override
        public void run() {
            if (player != null && playerPrepared) {
                try {
                    seekAudio.setProgress(player.getCurrentPosition());
                    tvAudioTime.setText(fmt(player.getCurrentPosition()) + " / " + fmt(player.getDuration()));
                } catch (IllegalStateException ignored) { }
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_building_detail);
        api = new ApiClient(this);
        buildingId = getIntent().getIntExtra("building_id", 0);

        scroll = findViewById(R.id.scroll);
        stateBox = findViewById(R.id.stateBox);
        audioBox = findViewById(R.id.audioBox);
        videoBox = findViewById(R.id.videoBox);
        progress = findViewById(R.id.progress);
        tvState = findViewById(R.id.tvState);
        tvAudioTime = findViewById(R.id.tvAudioTime);
        tvVideoState = findViewById(R.id.tvVideoState);
        spAudio = findViewById(R.id.spAudio);
        spVideo = findViewById(R.id.spVideo);
        seekAudio = findViewById(R.id.seekAudio);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btnVideoPlay = findViewById(R.id.btnVideoPlay);
        videoView = findViewById(R.id.videoView);

        findViewById(R.id.btnRetry).setOnClickListener(v -> load());

        btnPlay.setOnClickListener(v -> playAudio());
        btnPause.setOnClickListener(v -> pauseAudio());
        btnStop.setOnClickListener(v -> stopAudio());
        btnVideoPlay.setOnClickListener(v -> toggleVideo());

        seekAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser && player != null && playerPrepared) player.seekTo(p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });

        spAudio.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos != audioIndexLoaded) stopAudio();
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
        spVideo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos != videoIndexLoaded) prepareVideo(pos);
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        load();
    }

    // ---------------- Data ----------------

    private void load() {
        stateBox.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        api.get("buildings.php", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject json) {
                progress.setVisibility(View.GONE);
                try {
                    building = null;
                    for (Models.Building b : Models.Building.list(json)) if (b.id == buildingId) building = b;
                } catch (Exception e) {
                    showFatal("Data gedung tidak dapat dibaca", true);
                    return;
                }
                if (building == null) { showFatal("Gedung tidak ditemukan.", false); return; }
                render();
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
                showFatal(message, true);
            }
        });
    }

    private void showFatal(String msg, boolean retry) {
        scroll.setVisibility(View.INVISIBLE);
        tvState.setText(msg);
        findViewById(R.id.btnRetry).setVisibility(retry ? View.VISIBLE : View.GONE);
        stateBox.setVisibility(View.VISIBLE);
    }

    private void render() {
        scroll.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.tvName)).setText(building.name);
        ((TextView) findViewById(R.id.tvAddress)).setText(building.address);
        ((TextView) findViewById(R.id.tvDesc)).setText(building.description);

        android.view.ViewGroup fb = findViewById(R.id.facilityBox);
        fb.removeAllViews();
        if (building.facilities.isEmpty()) {
            TextView t = new TextView(this);
            t.setText("Belum ada data fasilitas.");
            fb.addView(t);
        }
        for (Models.Facility f : building.facilities) {
            TextView t = new TextView(this);
            t.setText("• " + f.name + (f.description == null || f.description.isEmpty() ? "" : " - " + f.description));
            t.setPadding(0, 6, 0, 0);
            fb.addView(t);
        }

        audios.clear();
        videos.clear();
        for (Models.Media m : building.media) (m.isAudio() ? audios : videos).add(m);

        audioBox.setVisibility(audios.isEmpty() ? View.GONE : View.VISIBLE);
        videoBox.setVisibility(videos.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.tvNoMedia).setVisibility(audios.isEmpty() && videos.isEmpty() ? View.VISIBLE : View.GONE);

        if (!audios.isEmpty()) spAudio.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, audios));
        if (!videos.isEmpty()) {
            spVideo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, videos));
            MediaController mc = new MediaController(this);
            mc.setAnchorView(videoView);
            videoView.setMediaController(mc);
            videoView.setOnCompletionListener(mp -> btnVideoPlay.setText("Play Video"));
            videoView.setOnErrorListener((mp, what, extra) -> {
                tvVideoState.setText("Video tidak dapat diputar. Periksa koneksi atau format file.");
                return true;
            });
            prepareVideo(0);
        }
    }

    // ---------------- Audio (MediaPlayer) ----------------

    private void playAudio() {
        if (audios.isEmpty()) return;
        if (player != null && playerPrepared) {
            if (!player.isPlaying()) player.start();
            handler.removeCallbacks(seekTask);
            handler.post(seekTask);
            return;
        }
        if (player != null) return; // masih memuat

        int idx = spAudio.getSelectedItemPosition();
        audioIndexLoaded = idx;
        player = new MediaPlayer();
        startWhenPrepared = true;
        tvAudioTime.setText("Memuat audio...");
        try {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build());
            player.setDataSource(this, Uri.parse(audios.get(idx).url));
            player.setOnPreparedListener(mp -> {
                playerPrepared = true;
                seekAudio.setMax(mp.getDuration());
                if (startWhenPrepared) mp.start();
                handler.removeCallbacks(seekTask);
                handler.post(seekTask);
            });
            player.setOnCompletionListener(mp -> {
                seekAudio.setProgress(0);
                tvAudioTime.setText("Selesai");
            });
            player.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Audio tidak dapat diputar. Periksa koneksi atau format file.", Toast.LENGTH_LONG).show();
                releaseAudio();
                tvAudioTime.setText("00:00 / 00:00");
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal memuat audio: " + e.getMessage(), Toast.LENGTH_LONG).show();
            releaseAudio();
        }
    }

    private void pauseAudio() {
        if (player != null && playerPrepared && player.isPlaying()) player.pause();
        startWhenPrepared = false;
    }

    private void stopAudio() {
        releaseAudio();
        audioIndexLoaded = -1;
        seekAudio.setProgress(0);
        tvAudioTime.setText("00:00 / 00:00");
    }

    private void releaseAudio() {
        handler.removeCallbacks(seekTask);
        if (player != null) {
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
        playerPrepared = false;
        startWhenPrepared = false;
    }

    // ---------------- Video (VideoView) ----------------

    private void prepareVideo(int index) {
        if (index < 0 || index >= videos.size()) return;
        videoIndexLoaded = index;
        videoView.stopPlayback();
        btnVideoPlay.setText("Play Video");
        tvVideoState.setText("Memuat video...");
        videoView.setVideoURI(Uri.parse(videos.get(index).url));
        videoView.setOnPreparedListener(mp -> tvVideoState.setText("Siap diputar. Gunakan kontrol atau tombol Play."));
    }

    private void toggleVideo() {
        if (videos.isEmpty()) return;
        if (videoView.isPlaying()) {
            videoView.pause();
            btnVideoPlay.setText("Play Video");
        } else {
            // hanya satu media yang bersuara pada satu waktu
            pauseAudio();
            videoView.start();
            btnVideoPlay.setText("Pause Video");
        }
    }

    // ---------------- Lifecycle ----------------

    @Override
    protected void onPause() {
        super.onPause();
        pauseAudio();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
            btnVideoPlay.setText("Play Video");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseAudio();
        if (videoView != null) videoView.stopPlayback();
    }

    private static String fmt(int ms) {
        int s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }
}
