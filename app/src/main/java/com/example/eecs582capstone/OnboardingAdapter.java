package com.example.eecs582capstone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class OnboardingAdapter extends FragmentStateAdapter {

    private final List<OnboardingItem> items;

    public OnboardingAdapter(@NonNull AppCompatActivity activity, List<OnboardingItem> items) {
        super(activity);
        this.items = items;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return OnboardingFragment.newInstance(
                items.get(position).getImageResId(),
                items.get(position).getTitle(),
                items.get(position).getDescription()
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}