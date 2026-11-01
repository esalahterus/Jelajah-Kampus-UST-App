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

    /**
     * Build debug memakai Laragon atau IP komputer lokal.
     * Android API 37 harus meminta izin jaringan lokal untuk kasus ini.
     */
    public static boolean usesLocalBackend() {
        return BuildConfig.DEBUG;
    }

    /** Interval kirim lokasi peserta ke server, dalam milidetik. */
    public static final long TRACKING_MIN_INTERVAL_MS = 5000;

    /** Interval pembaruan posisi peserta lain di peta, dalam milidetik. */
    public static final long PARTICIPANT_REFRESH_MS = 5000;
}