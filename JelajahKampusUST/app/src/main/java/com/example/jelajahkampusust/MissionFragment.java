package com.example.jelajahkampusust;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MissionFragment extends Fragment {

    private final List<Models.Mission> missions = new ArrayList<>();
    private MissionAdapter adapter;
    private SwipeRefreshLayout swipe;
    private View stateBox;
    private TextView tvState;
    private Button btnRetry;
    private ProgressBar progress;
    private ApiClient api;
    private SessionManager session;

    private final ActivityResultLauncher<Intent> detailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    int buildingId = result.getData().getIntExtra("show_building_id", 0);
                    if (buildingId > 0) ((MainActivity) requireActivity()).showMapAt(buildingId);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_missions, container, false);
        api = new ApiClient(requireContext());
        session = new SessionManager(requireContext());

        swipe = v.findViewById(R.id.swipe);
        stateBox = v.findViewById(R.id.stateBox);
        tvState = v.findViewById(R.id.tvState);
        btnRetry = v.findViewById(R.id.btnRetry);
        progress = v.findViewById(R.id.progress);

        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MissionAdapter(missions, m -> {
            Intent i = new Intent(requireContext(), MissionDetailActivity.class);
            i.putExtra("mission_id", m.id);
            detailLauncher.launch(i);
        });
        rv.setAdapter(adapter);

        swipe.setOnRefreshListener(this::load);
        btnRetry.setOnClickListener(x -> load());
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
        if (missions.isEmpty()) progress.setVisibility(View.VISIBLE);
        api.get("missions.php", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject json) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                progress.setVisibility(View.GONE);
                try {
                    missions.clear();
                    missions.addAll(Models.Mission.list(json));
                } catch (Exception e) {
                    showState("Data misi tidak dapat dibaca", true);
                    return;
                }
                adapter.notifyDataSetChanged();
                syncActiveSession();
                if (missions.isEmpty()) showState("Belum ada misi tersedia.", false);
                else stateBox.setVisibility(View.GONE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                progress.setVisibility(View.GONE);
                if (missions.isEmpty()) showState(message, true);
                else android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showState(String msg, boolean retry) {
        tvState.setText(msg);
        btnRetry.setVisibility(retry ? View.VISIBLE : View.GONE);
        stateBox.setVisibility(View.VISIBLE);
    }

    /** Samakan sesi aktif lokal dengan server (mis. sesi berakhir karena bukti diterima admin). */
    private void syncActiveSession() {
        Models.Mission active = null;
        for (Models.Mission m : missions) if (m.activeSessionId > 0) { active = m; break; }
        if (active == null) {
            if (session.hasActiveSession()) JelajahApp.tracking.stopSession();
        } else if (session.getActiveSessionId() != active.activeSessionId) {
            JelajahApp.tracking.startSession(active.activeSessionId, active.id, active.title);
        }
    }
}
