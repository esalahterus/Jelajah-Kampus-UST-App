package com.example.jelajahkampusust;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Intent;

/** Profil mahasiswa: identitas, jumlah misi selesai, riwayat misi, dan logout. */
public class ProfileFragment extends Fragment {

    private TextView tvName, tvNim, tvProdi, tvSummary, tvError;
    private ViewGroup historyBox;
    private SessionManager session;
    private ApiClient api;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        session = new SessionManager(requireContext());
        api = new ApiClient(requireContext());

        tvName = v.findViewById(R.id.tvName);
        tvNim = v.findViewById(R.id.tvNim);
        tvProdi = v.findViewById(R.id.tvProdi);
        tvSummary = v.findViewById(R.id.tvSummary);
        tvError = v.findViewById(R.id.tvError);
        historyBox = v.findViewById(R.id.historyBox);

        v.findViewById(R.id.btnLogout).setOnClickListener(x -> new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Keluar dari akun? Sesi eksplorasi yang aktif akan diakhiri.")
                .setPositiveButton("Logout", (d, w) -> doLogout())
                .setNegativeButton("Batal", null)
                .show());
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) load();
    }

    private void load() {
        tvName.setText(session.getName());
        tvNim.setText("NIM: " + session.getNim());
        tvProdi.setText("Program Studi: " + session.getProdi());

        api.get("profile.php", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject j) {
                if (!isAdded()) return;
                tvError.setVisibility(View.GONE);
                tvSummary.setText(j.optInt("completed_count") + " misi selesai  |  " + j.optInt("total_points") + " poin");
                historyBox.removeAllViews();
                JSONArray h = j.optJSONArray("history");
                if (h == null || h.length() == 0) {
                    addRow("Belum ada misi yang selesai.", "");
                } else {
                    for (int i = 0; i < h.length(); i++) {
                        JSONObject o = h.optJSONObject(i);
                        if (o != null) addRow(o.optString("title"), o.optString("completion_date") + "  (+" + o.optInt("points") + " poin)");
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                tvError.setText(message);
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addRow(String title, String sub) {
        View row = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_2, historyBox, false);
        ((TextView) row.findViewById(android.R.id.text1)).setText(title);
        ((TextView) row.findViewById(android.R.id.text2)).setText(sub);
        historyBox.addView(row);
    }

    private void doLogout() {
        JelajahApp.tracking.stopSession();
        api.post("logout.php", null, new ApiClient.Callback() {
            @Override public void onSuccess(JSONObject json) { finishLogout(); }
            @Override public void onError(String message) { finishLogout(); }
        });
    }

    private void finishLogout() {
        session.clear();
        if (!isAdded()) return;
        Intent i = new Intent(requireContext(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
