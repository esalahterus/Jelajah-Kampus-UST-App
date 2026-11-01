package com.example.jelajahkampusust;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TrackingManager {

    public interface Listener {
        void onLocation(Location location);
        void onStatus(String message);
    }

    private final Context app;
    private final LocationManager lm;
    private final SessionManager session;
    private final ApiClient api;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private Location last;
    private long lastSentAt = 0;
    private boolean updating = false;
    private int startedActivities = 0;

    public TrackingManager(Context context) {
        app = context.getApplicationContext();
        lm = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
        session = new SessionManager(app);
        api = new ApiClient(app);
    }

    // ---------------- API publik ----------------

    public void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }
    public Location getLastLocation() { return last; }

    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isLocationEnabled() {
        return lm != null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    public void startSession(int sessionId, int missionId, String title) {
        session.setActiveSession(sessionId, missionId, title);
        resume();
    }

    public void stopSession() {
        session.clearActiveSession();
        pause();
    }

    public void resume() {
        if (updating || startedActivities == 0 || !session.hasActiveSession() || !hasPermission() || lm == null) return;
        try {
            boolean any = false;
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 3, gpsListener, Looper.getMainLooper());
                any = true;
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, gpsListener, Looper.getMainLooper());
                any = true;
            }
            updating = any;
            if (!any) notifyStatus("GPS tidak aktif. Aktifkan GPS untuk pelacakan.");
        } catch (SecurityException e) {
            notifyStatus("Izin lokasi belum diberikan");
        }
    }

    public void pause() {
        if (!updating || lm == null) return;
        try {
            lm.removeUpdates(gpsListener);
        } catch (SecurityException ignored) { }
        updating = false;
    }

    // ---------------- Lifecycle aplikasi ----------------

    void onActivityStarted() {
        startedActivities++;
        if (startedActivities == 1) resume();
    }

    void onActivityStopped() {
        startedActivities = Math.max(0, startedActivities - 1);
        if (startedActivities == 0) pause();
    }

    // ---------------- Internal ----------------

    private final LocationListener gpsListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            last = location;
            for (Listener l : listeners) l.onLocation(location);
            sendToServer(location);
        }

        @Override public void onStatusChanged(String provider, int status, Bundle extras) { }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            notifyStatus("Lokasi aktif kembali");
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            notifyStatus("GPS dinonaktifkan, aktifkan untuk pelacakan.");
        }
    };

    private void sendToServer(Location loc) {
        if (!session.hasActiveSession()) return;
        long now = System.currentTimeMillis();
        if (now - lastSentAt < Config.TRACKING_MIN_INTERVAL_MS) return;
        lastSentAt = now;

        api.post("tracking_update.php", ApiClient.params(
                "session_id", String.valueOf(session.getActiveSessionId()),
                "latitude", String.valueOf(loc.getLatitude()),
                "longitude", String.valueOf(loc.getLongitude())
        ), new ApiClient.Callback() {
            @Override public void onSuccess(JSONObject json) { }

            @Override
            public void onError(String message) {
                // Server menyatakan sesi tidak aktif (mis. bukti diterima admin): hentikan pelacakan.
                if (message != null && message.startsWith("Sesi tidak aktif")) {
                    stopSession();
                    notifyStatus("Sesi eksplorasi telah berakhir, pelacakan dihentikan.");
                }
            }
        });
    }

    private void notifyStatus(String msg) {
        for (Listener l : listeners) l.onStatus(msg);
    }
}
