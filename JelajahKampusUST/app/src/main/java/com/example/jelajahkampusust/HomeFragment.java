package com.example.jelajahkampusust;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

/** FR-03: beranda berisi sapaan, ringkasan progres, dan misi yang sedang berlangsung. */
public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvProgress, tvStats, tvActive, tvError;
    private ProgressBar bar;
    private View cardActive;
    private Button btnRetry;
    private SessionManager session;
    private ApiClient api;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        session = new SessionManager(requireContext());
        api = new ApiClient(requireContext());

        tvGreeting = v.findViewById(R.id.tvGreeting);
        tvProgress = v.findViewById(R.id.tvProgress);
        tvStats = v.findViewById(R.id.tvStats);
        tvActive = v.findViewById(R.id.tvActiveMission);
        tvError = v.findViewById(R.id.tvError);
        bar = v.findViewById(R.id.barProgress);
        cardActive = v.findViewById(R.id.cardActive);
        btnRetry = v.findViewById(R.id.btnRetry);

        v.findViewById(R.id.btnGoMissions).setOnClickListener(x -> ((MainActivity) requireActivity()).goToTab(1));
        v.findViewById(R.id.btnGoMap).setOnClickListener(x -> ((MainActivity) requireActivity()).goToTab(2));
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
        tvGreeting.setText("Halo, " + session.getName() + "!");
        if (session.hasActiveSession()) {
            cardActive.setVisibility(View.VISIBLE);
            tvActive.setText(session.getActiveMissionTitle());
        } else {
            cardActive.setVisibility(View.GONE);
        }

        api.get("profile.php", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject j) {
                if (!isAdded()) return;
                int done = j.optInt("completed_count");
                int total = j.optInt("total_missions");
                tvProgress.setText(done + " / " + total + " misi");
                bar.setProgress(total == 0 ? 0 : done * 100 / total);
                tvStats.setText("Misi tersedia: " + total + "   |   Total poin: " + j.optInt("total_points"));
                tvError.setVisibility(View.GONE);
                btnRetry.setVisibility(View.GONE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                tvError.setText(message);
                tvError.setVisibility(View.VISIBLE);
                btnRetry.setVisibility(View.VISIBLE);
            }
        });
    }
}
