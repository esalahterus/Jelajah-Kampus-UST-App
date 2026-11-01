package com.example.jelajahkampusust;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.VH> {

    public interface OnClick { void onClick(Models.Mission m); }

    private final List<Models.Mission> data;
    private final OnClick listener;

    public MissionAdapter(List<Models.Mission> data, OnClick listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mission, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Models.Mission m = data.get(position);
        h.title.setText(m.title);
        h.building.setText("Lokasi: " + m.buildingName);
        h.points.setText("+" + m.pointReward + " poin");
        h.status.setText(m.statusLabel());

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(40f);
        bg.setColor(m.statusColor(h.itemView.getContext()));
        h.status.setBackground(bg);
        h.status.setPadding(24, 6, 24, 6);

        h.itemView.setOnClickListener(v -> listener.onClick(m));
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title, building, status, points;
        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tvTitle);
            building = v.findViewById(R.id.tvBuilding);
            status = v.findViewById(R.id.tvStatus);
            points = v.findViewById(R.id.tvPoints);
        }
    }
}
