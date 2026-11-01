package com.example.jelajahkampusust;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF = "jelajah_session";
    private final SharedPreferences sp;

    public SessionManager(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void saveLogin(String token, String name, String email, String nim, String prodi) {
        sp.edit().putString("token", token).putString("name", name).putString("email", email)
                .putString("nim", nim).putString("prodi", prodi).apply();
    }

    public boolean isLoggedIn() { return !getToken().isEmpty(); }
    public String getToken() { return sp.getString("token", ""); }
    public String getName() { return sp.getString("name", ""); }
    public String getEmail() { return sp.getString("email", ""); }
    public String getNim() { return sp.getString("nim", ""); }
    public String getProdi() { return sp.getString("prodi", ""); }

    // ---- Sesi eksplorasi aktif ----
    public void setActiveSession(int sessionId, int missionId, String missionTitle) {
        sp.edit().putInt("session_id", sessionId).putInt("mission_id", missionId)
                .putString("mission_title", missionTitle).apply();
    }

    public void clearActiveSession() {
        sp.edit().remove("session_id").remove("mission_id").remove("mission_title").apply();
    }

    public int getActiveSessionId() { return sp.getInt("session_id", 0); }
    public int getActiveMissionId() { return sp.getInt("mission_id", 0); }
    public String getActiveMissionTitle() { return sp.getString("mission_title", ""); }
    public boolean hasActiveSession() { return getActiveSessionId() > 0; }

    public void clear() { sp.edit().clear().apply(); }
}
