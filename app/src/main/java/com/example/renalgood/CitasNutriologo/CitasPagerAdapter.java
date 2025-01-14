package com.example.renalgood.CitasNutriologo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class CitasPagerAdapter extends FragmentStateAdapter {

    public CitasPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new CitasPendientesFragment();
            case 1:
                return new CitasConfirmadasFragment();
            default:
                return new CitasPendientesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}