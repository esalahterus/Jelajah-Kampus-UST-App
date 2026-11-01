package com.example.jelajahkampusust;

import android.content.Context;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class Models {
    private Models() {}

    public static class Mission implements Serializable {
        public int id, buildingId, pointReward, activeSessionId;
        public String title, description, buildingName, status;
        public double latitude, longitude;
        public boolean hasLocation;
        // Bukti terakhir (bisa kosong)
        public boolean hasEvidence;
        public String evidencePhotoUrl = "", evidenceStatus = "", evidenceNote = "", evidenceTime = "";

        public static Mission from(JSONObject o) throws JSONException {
            Mission m = new Mission();
            m.id = o.getInt("id");
            m.buildingId = o.getInt("building_id");
            m.title = o.getString("title");
            m.description = o.optString("description", "");
            m.pointReward = o.optInt("point_reward", 0);
            m.buildingName = o.optString("building_name", "");
            m.status = o.optString("status", "not_started");
            m.activeSessionId = o.optInt("active_session_id", 0);
            m.hasLocation = !o.isNull("latitude") && !o.isNull("longitude");
            if (m.hasLocation) {
                m.latitude = o.getDouble("latitude");
                m.longitude = o.getDouble("longitude");
            }
            JSONObject ev = o.optJSONObject("evidence");
            if (ev != null) {
                m.hasEvidence = true;
                m.evidencePhotoUrl = ev.optString("photo_url", "");
                m.evidenceStatus = ev.optString("verification_status", "");
                m.evidenceNote = ev.optString("admin_note", "");
                m.evidenceTime = ev.optString("submitted_at", "");
            }
            return m;
        }

        public String statusLabel() {
            switch (status) {
                case "in_progress": return "Sedang berlangsung";
                case "pending": return "Menunggu verifikasi";
                case "rejected": return "Ditolak - kirim ulang";
                case "completed": return "Selesai";
                default: return "Belum dimulai";
            }
        }

        public int statusColor(Context c) {
            int res;
            switch (status) {
                case "in_progress": res = R.color.st_in_progress; break;
                case "pending": res = R.color.st_pending; break;
                case "rejected": res = R.color.st_rejected; break;
                case "completed": res = R.color.st_completed; break;
                default: res = R.color.st_not_started;
            }
            return ContextCompat.getColor(c, res);
        }

        public static List<Mission> list(JSONObject json) throws JSONException {
            List<Mission> out = new ArrayList<>();
            JSONArray arr = json.getJSONArray("missions");
            for (int i = 0; i < arr.length(); i++) out.add(from(arr.getJSONObject(i)));
            return out;
        }
    }

    public static class Facility {
        public String name, description;
    }

    public static class Media {
        public int id;
        public String title, type, url;
        public boolean isAudio() { return "audio".equals(type); }
        @Override public String toString() { return title; }
    }

    public static class Building {
        public int id;
        public String name, description, address;
        public double latitude, longitude;
        public boolean hasLocation;
        public List<Facility> facilities = new ArrayList<>();
        public List<Media> media = new ArrayList<>();

        public static Building from(JSONObject o) throws JSONException {
            Building b = new Building();
            b.id = o.getInt("id");
            b.name = o.getString("name");
            b.description = o.optString("description", "");
            b.address = o.optString("address", "");
            b.hasLocation = !o.isNull("latitude") && !o.isNull("longitude");
            if (b.hasLocation) {
                b.latitude = o.getDouble("latitude");
                b.longitude = o.getDouble("longitude");
            }
            JSONArray fa = o.optJSONArray("facilities");
            if (fa != null) for (int i = 0; i < fa.length(); i++) {
                JSONObject f = fa.getJSONObject(i);
                Facility fac = new Facility();
                fac.name = f.optString("name");
                fac.description = f.optString("description");
                b.facilities.add(fac);
            }
            JSONArray ma = o.optJSONArray("media");
            if (ma != null) for (int i = 0; i < ma.length(); i++) {
                JSONObject m = ma.getJSONObject(i);
                Media md = new Media();
                md.id = m.optInt("id");
                md.title = m.optString("title");
                md.type = m.optString("media_type");
                md.url = m.optString("media_url");
                b.media.add(md);
            }
            return b;
        }

        public static List<Building> list(JSONObject json) throws JSONException {
            List<Building> out = new ArrayList<>();
            JSONArray arr = json.getJSONArray("buildings");
            for (int i = 0; i < arr.length(); i++) out.add(from(arr.getJSONObject(i)));
            return out;
        }
    }

    public static class Participant {
        public String name, nim, status, recordedAt;
        public double latitude, longitude;
        public boolean hasLocation, isMe;

        public static List<Participant> list(JSONObject json) throws JSONException {
            List<Participant> out = new ArrayList<>();
            JSONArray arr = json.getJSONArray("participants");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Participant p = new Participant();
                p.name = o.optString("name");
                p.nim = o.optString("nim");
                p.status = o.optString("status");
                p.recordedAt = o.optString("recorded_at", "");
                p.isMe = o.optBoolean("is_me", false);
                p.hasLocation = !o.isNull("latitude") && !o.isNull("longitude");
                if (p.hasLocation) {
                    p.latitude = o.getDouble("latitude");
                    p.longitude = o.getDouble("longitude");
                }
                out.add(p);
            }
            return out;
        }

        public String statusLabel() {
            switch (status) {
                case "active": return "Aktif";
                case "inactive": return "Tidak aktif";
                default: return "Lokasi tidak tersedia";
            }
        }
    }
}
