package com.example.jelajahkampusust;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;

import java.io.File;

public class JelajahApp extends Application implements Application.ActivityLifecycleCallbacks {

    /** Pengelola pelacakan lokasi tunggal untuk seluruh aplikasi. */
    public static TrackingManager tracking;

    @Override
    public void onCreate() {
        super.onCreate();

        // Konfigurasi osmdroid: user agent wajib, cache di folder aplikasi (tanpa izin storage).
        IConfigurationProvider cfg = Configuration.getInstance();
        cfg.setUserAgentValue("JelajahKampusUST/1.0 (Android; contact@jelajahkampusust.com)");
        File base = new File(getCacheDir(), "osmdroid");
        cfg.setOsmdroidBasePath(base);
        cfg.setOsmdroidTileCache(new File(base, "tiles"));

        tracking = new TrackingManager(this);
        registerActivityLifecycleCallbacks(this);
    }

    // Pelacakan hanya berjalan saat ada Activity aplikasi yang tampil (lifecycle, NFR-10).
    @Override public void onActivityStarted(Activity a) { tracking.onActivityStarted(); }
    @Override public void onActivityStopped(Activity a) { tracking.onActivityStopped(); }
    @Override public void onActivityCreated(Activity a, Bundle b) { }
    @Override public void onActivityResumed(Activity a) { }
    @Override public void onActivityPaused(Activity a) { }
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
    @Override public void onActivityDestroyed(Activity a) { }
}
