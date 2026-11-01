package com.example.jelajahkampusust;

import android.Manifest;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Peta kampus (MP-04P + MP-05P): marker gedung, lokasi pengguna, dan peserta sesi aktif.
 * Sumber tile: OpenStreetMap (MAPNIK), user agent = package aplikasi.
 */
public class MapFragment extends Fragment implements TrackingManager.Listener {

    private static final GeoPoint DEFAULT_CENTER = new GeoPoint(-7.8060, 110.3925);

    private MapView map;
    private TextView tvInfo, tvBName, tvBDesc, tvBDistance;
    private View cardBuilding;
    private MyLocationNewOverlay myLocation;

    private final List<Models.Building> buildings = new ArrayList<>();
    private final Map<Integer, Marker> buildingMarkers = new HashMap<>();
    private final List<Marker> participantMarkers = new ArrayList<>();
    private Models.Building selected;
    private int pendingFocusBuildingId = 0;

    private ApiClient api;
    private SessionManager session;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean polling = false;

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) {
                    enableMyLocation();
                    JelajahApp.tracking.resume();
                } else {
                    Toast.makeText(requireContext(), "Izin lokasi ditolak. Fitur lokasi dan pelacakan tidak aktif.", Toast.LENGTH_LONG).show();
                }
            });

    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (!polling || !isAdded()) return;
            refreshParticipants();
            handler.postDelayed(this, Config.PARTICIPANT_REFRESH_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);
        api = new ApiClient(requireContext());
        session = new SessionManager(requireContext());

        map = v.findViewById(R.id.map);
        tvInfo = v.findViewById(R.id.tvInfo);
        cardBuilding = v.findViewById(R.id.cardBuilding);
        tvBName = v.findViewById(R.id.tvBName);
        tvBDesc = v.findViewById(R.id.tvBDesc);
        tvBDistance = v.findViewById(R.id.tvBDistance);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setMinZoomLevel(4.0);
        map.getController().setZoom(17.0);
        map.getController().setCenter(DEFAULT_CENTER);
        // Ketuk area kosong menutup kartu gedung
        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) { hideCard(); return false; }
            @Override public boolean longPressHelper(GeoPoint p) { return false; }
        }));

        v.findViewById(R.id.fabZoomIn).setOnClickListener(x -> map.getController().zoomIn());
        v.findViewById(R.id.fabZoomOut).setOnClickListener(x -> map.getController().zoomOut());
        v.findViewById(R.id.fabCampus).setOnClickListener(x -> centerOnCampus());
        v.findViewById(R.id.fabMyLocation).setOnClickListener(x -> focusMyLocation());
        v.findViewById(R.id.btnBClose).setOnClickListener(x -> hideCard());
        v.findViewById(R.id.btnBDetail).setOnClickListener(x -> {
            if (selected == null) return;
            Intent i = new Intent(requireContext(), BuildingDetailActivity.class);
            i.putExtra("building_id", selected.id);
            startActivity(i);
        });

        loadBuildings();
        return v;
    }

    // ---------------- Lifecycle ----------------

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        if (!isHidden()) onVisible();
    }

    @Override
    public void onPause() {
        super.onPause();
        onInvisible();
        map.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) onInvisible(); else onVisible();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        onInvisible();
        map.onDetach();
    }

    private void onVisible() {
        JelajahApp.tracking.addListener(this);
        if (JelajahApp.tracking.hasPermission()) enableMyLocation();
        updateInfoBar();
        if (session.hasActiveSession()) {
            polling = true;
            handler.removeCallbacks(pollTask);
            handler.post(pollTask);
        }
        if (pendingFocusBuildingId > 0) focusBuilding(pendingFocusBuildingId);
    }

    private void onInvisible() {
        JelajahApp.tracking.removeListener(this);
        polling = false;
        handler.removeCallbacks(pollTask);
        if (myLocation != null) myLocation.disableMyLocation();
    }

    // ---------------- Data gedung ----------------

    private void loadBuildings() {
        api.get("buildings.php", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject json) {
                if (!isAdded()) return;
                try {
                    buildings.clear();
                    buildings.addAll(Models.Building.list(json));
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Data gedung tidak dapat dibaca", Toast.LENGTH_SHORT).show();
                    return;
                }
                drawBuildingMarkers();
                if (pendingFocusBuildingId > 0) focusBuilding(pendingFocusBuildingId);
                else centerOnCampus();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Gagal memuat gedung: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void drawBuildingMarkers() {
        for (Marker m : buildingMarkers.values()) map.getOverlays().remove(m);
        buildingMarkers.clear();
        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_building_marker);
        for (Models.Building b : buildings) {
            if (!b.hasLocation) continue;
            Marker m = new Marker(map);
            m.setPosition(new GeoPoint(b.latitude, b.longitude));
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            m.setTitle(b.name);
            m.setIcon(icon);
            m.setOnMarkerClickListener((marker, mv) -> { showCard(b); return true; });
            map.getOverlays().add(m);
            buildingMarkers.put(b.id, m);
        }
        map.invalidate();
    }

    /** Dipanggil dari MainActivity.showMapAt(): fokus ke gedung dan tampilkan infonya. */
    public void focusBuilding(int buildingId) {
        pendingFocusBuildingId = buildingId;
        if (map == null) return;
        for (Models.Building b : buildings) {
            if (b.id == buildingId && b.hasLocation) {
                map.getController().animateTo(new GeoPoint(b.latitude, b.longitude));
                map.getController().setZoom(18.0);
                showCard(b);
                pendingFocusBuildingId = 0;
                return;
            }
        }
    }

    private void centerOnCampus() {
        if (buildings.isEmpty()) {
            map.getController().animateTo(DEFAULT_CENTER);
            return;
        }
        double lat = 0, lon = 0;
        int n = 0;
        for (Models.Building b : buildings) if (b.hasLocation) { lat += b.latitude; lon += b.longitude; n++; }
        if (n > 0) map.getController().animateTo(new GeoPoint(lat / n, lon / n));
        map.getController().setZoom(17.0);
    }

    // ---------------- Kartu gedung ----------------

    private void showCard(Models.Building b) {
        selected = b;
        tvBName.setText(b.name);
        tvBDesc.setText(b.description);
        updateDistanceText();
        cardBuilding.setVisibility(View.VISIBLE);
    }

    private void hideCard() {
        selected = null;
        cardBuilding.setVisibility(View.GONE);
    }

    private Location currentLocation() {
        Location l = JelajahApp.tracking.getLastLocation();
        if (myLocation != null && myLocation.getMyLocation() != null) {
            GeoPoint g = myLocation.getMyLocation();
            Location o = new Location("osmdroid");
            o.setLatitude(g.getLatitude());
            o.setLongitude(g.getLongitude());
            return o;
        }
        return l;
    }

    private void updateDistanceText() {
        if (selected == null || !selected.hasLocation) return;
        Location me = currentLocation();
        if (me == null) {
            tvBDistance.setText("Jarak: lokasi Anda belum tersedia");
            return;
        }
        float[] res = new float[1];
        Location.distanceBetween(me.getLatitude(), me.getLongitude(), selected.latitude, selected.longitude, res);
        tvBDistance.setText(res[0] >= 1000
                ? String.format(java.util.Locale.getDefault(), "Jarak lurus: %.2f km", res[0] / 1000f)
                : String.format(java.util.Locale.getDefault(), "Jarak lurus: %d m", Math.round(res[0])));
    }

    // ---------------- Lokasi pengguna ----------------

    private void enableMyLocation() {
        if (myLocation == null) {
            myLocation = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
            map.getOverlays().add(myLocation);
        }
        try {
            myLocation.enableMyLocation();
        } catch (SecurityException ignored) { }
    }

    private void focusMyLocation() {
        if (!JelajahApp.tracking.hasPermission()) {
            locationPermission.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            return;
        }
        enableMyLocation();
        if (!JelajahApp.tracking.isLocationEnabled()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("GPS tidak aktif")
                    .setMessage("Aktifkan layanan lokasi untuk menampilkan posisi Anda.")
                    .setPositiveButton("Buka pengaturan", (d, w) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Batal", null)
                    .show();
            return;
        }
        Location me = currentLocation();
        if (me == null) {
            Toast.makeText(requireContext(), "Lokasi belum tersedia, tunggu sinyal GPS...", Toast.LENGTH_SHORT).show();
            return;
        }
        map.getController().animateTo(new GeoPoint(me.getLatitude(), me.getLongitude()));
        updateDistanceText();
    }

    // ---------------- Peserta sesi aktif ----------------

    private void updateInfoBar() {
        if (session.hasActiveSession()) {
            tvInfo.setText("Sesi aktif: " + session.getActiveMissionTitle()
                    + (JelajahApp.tracking.hasPermission() ? "" : "\nIzin lokasi belum diberikan - tekan tombol lokasi."));
            tvInfo.setVisibility(View.VISIBLE);
        } else {
            tvInfo.setVisibility(View.GONE);
        }
    }

    private void refreshParticipants() {
        if (!session.hasActiveSession()) { clearParticipants(); updateInfoBar(); return; }
        api.get("tracking_list.php", ApiClient.params("mission_id", String.valueOf(session.getActiveMissionId())),
                new ApiClient.Callback() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        if (!isAdded()) return;
                        try {
                            drawParticipants(Models.Participant.list(json));
                        } catch (Exception ignored) { }
                    }

                    @Override public void onError(String message) { /* biarkan marker terakhir */ }
                });
    }

    private void clearParticipants() {
        for (Marker m : participantMarkers) map.getOverlays().remove(m);
        participantMarkers.clear();
        map.invalidate();
    }

    private void drawParticipants(List<Models.Participant> list) {
        clearParticipants();
        int others = 0;
        for (Models.Participant p : list) {
            if (!p.isMe) others++;
            if (p.isMe || !p.hasLocation) continue; // posisi sendiri sudah digambar overlay lokasi
            Marker m = new Marker(map);
            m.setPosition(new GeoPoint(p.latitude, p.longitude));
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            m.setTitle(p.name + " (" + p.nim + ")");
            m.setSnippet(p.statusLabel() + " - " + p.recordedAt);

            Drawable d = ContextCompat.getDrawable(requireContext(), R.drawable.ic_participant).mutate();
            int color = "active".equals(p.status) ? R.color.p_active : R.color.p_inactive;
            d.setColorFilter(ContextCompat.getColor(requireContext(), color), PorterDuff.Mode.SRC_ATOP);
            m.setIcon(d);
            map.getOverlays().add(m);
            participantMarkers.add(m);
        }
        map.invalidate();
        tvInfo.setText("Sesi aktif: " + session.getActiveMissionTitle() + "\nPeserta lain di misi ini: " + others);
        tvInfo.setVisibility(View.VISIBLE);
    }

    // ---------------- TrackingManager.Listener ----------------

    @Override
    public void onLocation(Location location) {
        updateDistanceText();
    }

    @Override
    public void onStatus(String message) {
        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        if (!session.hasActiveSession()) { polling = false; clearParticipants(); updateInfoBar(); }
    }
}
