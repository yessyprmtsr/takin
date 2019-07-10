package com.multazamgsd.takin.ui.main;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;
import com.google.android.material.navigation.NavigationView;
import com.multazamgsd.takin.LoginActivity;
import com.multazamgsd.takin.R;
import com.multazamgsd.takin.ui.home.HomeFragment;
import com.multazamgsd.takin.util.AuthHelper;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final String SELECTED_BOTTOM_MENU = "selected_bottom_menu";
    private AuthHelper authHelper;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        authHelper = new AuthHelper(this);
        if(authHelper.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        //Setting up drawer navigation
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationDrawerView = findViewById(R.id.nav_drawer_view);
        navigationDrawerView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //Get navigation drawer header layout
        View navHeaderView = navigationDrawerView.getHeaderView(0);
        TextView username = navHeaderView.findViewById(R.id.textViewDrawerUsername);
        username.setText("Hello");
        TextView email = navHeaderView.findViewById(R.id.textViewDrawerEmail);
        email.setText(authHelper.getCurrentUser().getEmail());

        // Setting up bottom navigation
        bottomNavigationView = findViewById(R.id.nav_bottom_view);
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnBottomNavigationItemSelectedListener);
        // Set selected bottom menu
        if (savedInstanceState != null) {
            bottomNavigationView.setSelectedItemId(savedInstanceState.getInt(SELECTED_BOTTOM_MENU));
        } else {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.draw, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) { return true; }
        return super.onOptionsItemSelected(item);
    }

    //Navigation drawer item click listener
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_my_event) {

        } else if (id == R.id.nav_liked_event) {

        } else if (id == R.id.nav_logout) {
            authHelper.doLogout();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // Bottom navigation click listener
    private BottomNavigationView.OnNavigationItemSelectedListener mOnBottomNavigationItemSelectedListener = item -> {
        Fragment fragment = null;

        switch (item.getItemId()) {
            case R.id.navigation_home:
                fragment = HomeFragment.newInstance();
                break;
            case R.id.navigation_seminar:
                fragment = HomeFragment.newInstance();
                break;
            case R.id.navigation_committee:
                fragment = HomeFragment.newInstance();
                break;
            case R.id.navigation_contest:
                fragment = HomeFragment.newInstance();
                break;
            default:
                fragment = HomeFragment.newInstance();
        }

        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.container, fragment)
                    .commit();
        }
        return true;
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_BOTTOM_MENU, bottomNavigationView.getSelectedItemId());
    }
}
