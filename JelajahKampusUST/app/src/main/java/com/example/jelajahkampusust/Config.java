package com.example.jelajahkampusust;

/** Konfigurasi aplikasi dan alamat backend. */
public final class Config {
    private Config() {}

    /**
     * Debug emulator:
     * http://10.0.2.2/jelajah-kampus-ust/
     *
     * Debug HP fisik:
     * ubah nilai BASE_URL pada build.gradle.kts menjadi
     * http://IP_LAPTOP/jelajah-kampus-ust/
     */
    public static final String BASE_URL = BuildConfig.BASE_URL;

    public static final String API = BASE_URL + "api/";

    public static boolean usesLocalBackend() {
        return BuildConfig.DEBUG;
    }

    public static final long TRACKING_MIN_INTERVAL_MS = 5000;

    public static final long PARTICIPANT_REFRESH_MS = 5000;
}