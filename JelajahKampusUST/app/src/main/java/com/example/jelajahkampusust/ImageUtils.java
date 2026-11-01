package com.example.jelajahkampusust;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageUtils {
    private ImageUtils() {}

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(
            (int) (Runtime.getRuntime().maxMemory() / 1024 / 8)) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount() / 1024; }
    };

    public static File compressForUpload(File src, File outDir) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(src.getAbsolutePath(), bounds);

        int max = 1600, sample = 1;
        while (bounds.outWidth / sample > max * 2 || bounds.outHeight / sample > max * 2) sample *= 2;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bmp = BitmapFactory.decodeFile(src.getAbsolutePath(), opts);
        if (bmp == null) throw new IOException("Foto tidak dapat dibaca");

        int rotation = 0;
        try {
            ExifInterface exif = new ExifInterface(src.getAbsolutePath());
            int o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (o == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
            else if (o == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
            else if (o == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
        } catch (IOException ignored) { }

        float scale = Math.min(1f, (float) max / Math.max(bmp.getWidth(), bmp.getHeight()));
        if (rotation != 0 || scale < 1f) {
            Matrix m = new Matrix();
            if (rotation != 0) m.postRotate(rotation);
            if (scale < 1f) m.postScale(scale, scale);
            Bitmap t = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            if (t != bmp) bmp.recycle();
            bmp = t;
        }

        if (!outDir.exists()) //noinspection ResultOfMethodCallIgnored
            outDir.mkdirs();
        File out = new File(outDir, "upload_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos);
        }
        bmp.recycle();
        return out;
    }

    public static Bitmap decodeScaled(File f, int targetPx) {
        BitmapFactory.Options b = new BitmapFactory.Options();
        b.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), b);
        int sample = 1;
        while (b.outWidth / (sample * 2) >= targetPx && b.outHeight / (sample * 2) >= targetPx) sample *= 2;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        if (bmp == null) return null;
        try {
            ExifInterface exif = new ExifInterface(f.getAbsolutePath());
            int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rot = ori == ExifInterface.ORIENTATION_ROTATE_90 ? 90
                    : ori == ExifInterface.ORIENTATION_ROTATE_180 ? 180
                    : ori == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
            if (rot != 0) {
                Matrix m = new Matrix();
                m.postRotate(rot);
                Bitmap t = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                if (t != bmp) bmp.recycle();
                bmp = t;
            }
        } catch (IOException ignored) { }
        return bmp;
    }

    public static void load(String url, ImageView view) {
        if (url == null || url.isEmpty()) { view.setImageDrawable(null); return; }
        view.setTag(url);
        Bitmap cached = CACHE.get(url);
        if (cached != null) { view.setImageBitmap(cached); return; }
        view.setImageDrawable(null);

        EXEC.execute(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(15000);
                InputStream is = c.getInputStream();
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                byte[] data = bos.toByteArray();

                BitmapFactory.Options b = new BitmapFactory.Options();
                b.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, b);
                int sample = 1;
                while (b.outWidth / (sample * 2) >= 800) sample *= 2;
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = sample;
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, o);
                if (bmp != null) {
                    CACHE.put(url, bmp);
                    MAIN.post(() -> { if (url.equals(view.getTag())) view.setImageBitmap(bmp); });
                }
            } catch (Exception ignored) {
            } finally {
                if (c != null) c.disconnect();
            }
        });
    }
}
