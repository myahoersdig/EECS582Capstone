/*
Filename: OnboardingAdapter.java
Author(s): Abdelrahman Zeidan
Created: 04-25-2026
Last Modified: 04-26-2026
Overview and Purpose: Connects onboarding data to swipeable fragments
Notes:
*/

package com.example.eecs582capstone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class OnboardingAdapter extends FragmentStateAdapter {

    // List of onboarding items
    private final List<OnboardingItem> items;

    public OnboardingAdapter(@NonNull AppCompatActivity activity, List<OnboardingItem> items) {
        super(activity);
        this.items = items;
    }

    // Creates fragment for each page
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return OnboardingFragment.newInstance(
                items.get(position).getImageResId(),
                items.get(position).getTitle(),
                items.get(position).getDescription()
        );
    }

    // Returns total number of pages
    @Override
    public int getItemCount() {
        return items.size();
    }
}