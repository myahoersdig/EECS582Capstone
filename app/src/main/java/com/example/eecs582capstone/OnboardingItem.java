/*
Filename: OnboardingItem.java
Author(s): Abdelrahman Zeidan
Created: 04-25-2026
Last Modified: 04-26-2026
Overview and Purpose: Stores data for each onboarding screen
Notes:
*/

package com.example.eecs582capstone;

public class OnboardingItem {

    // Data fields
    private final int imageResId;
    private final String title;
    private final String description;

    // Constructor
    public OnboardingItem(int imageResId, String title, String description) {
        this.imageResId = imageResId;
        this.title = title;
        this.description = description;
    }

    // Getters
    public int getImageResId() { return imageResId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}