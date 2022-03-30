package com.salih.sakainotifier;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.salih.sakainotifier.fragments.AlertsFragment;
import com.salih.sakainotifier.fragments.FoodFragment;
import com.salih.sakainotifier.fragments.GradesFragment;
import com.salih.sakainotifier.fragments.ProfileFragment;

public class NavigationBarActivity extends AppCompatActivity {

    private final int CAPACITY = 525;
    int currentFragmentID = R.id.profileFragment;

    ProfileFragment profileFragment = new ProfileFragment();
    AlertsFragment alertsFragment = new AlertsFragment();
    GradesFragment gradesFragment = new GradesFragment();
    FoodFragment foodFragment = new FoodFragment();

    Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_navigation_bar);

        getSupportFragmentManager().beginTransaction().add(R.id.fragment, profileFragment).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment, alertsFragment).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment, gradesFragment).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment, foodFragment).commit();

        loadFragment(profileFragment, true, "profile");
        activeFragment = profileFragment;

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnItemSelectedListener(navListener);

        bottomNavigationView.setOnItemReselectedListener(item -> {
            // do nothing if item is selected again
        });
    }

    private NavigationBarView.OnItemSelectedListener navListener = new NavigationBarView.OnItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            Fragment fragment = null;

            switch (item.getItemId()) {
                case R.id.foodFragment: // LAST
                    fragment = foodFragment;
                    currentFragmentID = R.id.foodFragment;
                    return loadFragment(fragment, true,"food"); // from left
                case R.id.gradesFragment:
                    fragment = gradesFragment;
                    if(currentFragmentID == R.id.foodFragment){
                        currentFragmentID = R.id.gradesFragment;
                        return loadFragment(fragment, false,"grades");
                    }
                    else{
                        currentFragmentID = R.id.gradesFragment;
                        return loadFragment(fragment, true, "grades");
                    }
                case R.id.alertsFragment:
                    fragment = alertsFragment;
                    if(currentFragmentID == R.id.profileFragment) {
                        currentFragmentID = R.id.alertsFragment;
                        return loadFragment(fragment, true, "alerts");
                    }
                    else {
                        currentFragmentID = R.id.alertsFragment;
                        return loadFragment(fragment, false, "alerts");
                    }
                case R.id.profileFragment: // FIRST
                    currentFragmentID = R.id.profileFragment;
                    fragment = profileFragment;

                    return loadFragment(fragment, false, "profile"); // from right
            }

            return true;
        }
    };

    private boolean loadFragment(Fragment fragment, boolean direction, String tag) { // right if true, left if false

        if(direction) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.enter_from_right,
                            R.anim.exit_to_left,
                            R.anim.enter_from_left,
                            R.anim.exit_to_right
                    )
                    .hide(profileFragment)
                    .hide(alertsFragment)
                    .hide(gradesFragment)
                    .hide(foodFragment)
                    .show(fragment)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.enter_from_left,
                            R.anim.exit_to_right,
                            R.anim.enter_from_right,
                            R.anim.exit_to_left
                    )
                    .hide(profileFragment)
                    .hide(alertsFragment)
                    .hide(gradesFragment)
                    .hide(foodFragment)
                    .show(fragment)
                    .commit();
        }

        activeFragment = fragment;
        return true;
    }
}

