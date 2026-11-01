package com.example.jelajahkampusust;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/** MP-01P: satu Activity sebagai host, navigasi Beranda / Misi / Peta / Profil berbasis Fragment. */
public class MainActivity extends AppCompatActivity {

    private static final String[] TAGS = {"home", "missions", "map", "profile"};
    private static final int[] IDS = {R.id.nav_home, R.id.nav_missions, R.id.nav_map, R.id.nav_profile};

    private BottomNavigationView bottomNav;
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState != null) currentIndex = savedInstanceState.getInt("tab", 0);
        show(currentIndex);
        bottomNav.setSelectedItemId(IDS[currentIndex]);

        bottomNav.setOnItemSelectedListener(item -> {
            for (int i = 0; i < IDS.length; i++) {
                if (IDS[i] == item.getItemId()) { show(i); return true; }
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentIndex != 0) bottomNav.setSelectedItemId(IDS[0]);
                else finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", currentIndex);
    }

    /** Tampilkan fragment pada indeks tertentu (hide/show agar state peta tetap terjaga). */
    private void show(int index) {
        currentIndex = index;
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        for (int i = 0; i < TAGS.length; i++) {
            Fragment f = getSupportFragmentManager().findFragmentByTag(TAGS[i]);
            if (i == index) {
                if (f == null) tx.add(R.id.fragmentContainer, create(i), TAGS[i]);
                else tx.show(f);
            } else if (f != null) {
                tx.hide(f);
            }
        }
        tx.commit();
    }

    private Fragment create(int i) {
        switch (i) {
            case 1: return new MissionFragment();
            case 2: return new MapFragment();
            case 3: return new ProfileFragment();
            default: return new HomeFragment();
        }
    }

    public void goToTab(int index) {
        bottomNav.setSelectedItemId(IDS[index]);
    }

    /** Dipanggil dari detail misi: buka tab peta dan fokus ke gedung tujuan. */
    public void showMapAt(int buildingId) {
        goToTab(2);
        getSupportFragmentManager().executePendingTransactions();
        Fragment f = getSupportFragmentManager().findFragmentByTag("map");
        if (f instanceof MapFragment) ((MapFragment) f).focusBuilding(buildingId);
    }
}
