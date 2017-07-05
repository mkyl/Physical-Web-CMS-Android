package com.physical_web.cms.physicalwebcms;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import layout.AboutFragment;
import layout.BeaconFragment;
import layout.ContentFragment;
import layout.WelcomeFragment;

public class BaseActivity extends AppCompatActivity implements ContentFragment.OnFragmentInteractionListener {
    public static final String TAG = BaseActivity.class.getSimpleName();

    private String[] menuTitles;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;

    private SetupManager setupManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        // TODO make async
        setupManager = new SetupManager(this);

        setupNavigationDrawer();
        setupActionBar();
        // if statement to avoid overlapping fragments
        if (savedInstanceState == null)
            setupWelcomeFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ensure drive authorization, internet connection, etc. are all set up
        setupManager.checkRequirements();
    }

    private void setupNavigationDrawer() {
        menuTitles = getResources().getStringArray(R.array.navigation_drawer_items);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.drawer_top_list);

        drawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, menuTitles));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();

        if(actionBar == null) {
            throw new IllegalStateException("Current theme doesn't have an action bar");
        } else {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        drawerToggle = new ActionBarDrawerToggle(this,
                drawerLayout,
                R.string.drawer_open,
                R.string.drawer_close);

        drawerLayout.addDrawerListener(drawerToggle);
    }

    private void setupWelcomeFragment() {
        WelcomeFragment welcomeFragment = new WelcomeFragment();
        getSupportFragmentManager().beginTransaction().
                add(R.id.fragment_container, welcomeFragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String itemName = menuTitles[position];
            switchFragment(itemName);
        }

        private void switchFragment(String name) {
            getSupportActionBar().setTitle(name);

            ContentFragment switchFragment;
            Bundle args = new Bundle();
            switch(name) {
                case "Exhibits":
                    throw new UnsupportedOperationException("Not implemented yet");
                case "Beacons":
                    switchFragment = new BeaconFragment();
                    break;
                case "Analytics":
                    throw new UnsupportedOperationException("Not implemented yet");
                case "Settings":
                    throw new UnsupportedOperationException("Not implemented yet");
                case "About":
                    switchFragment = new AboutFragment();
                    break;
                default:
                    throw new RuntimeException("Unimplemented menu item");
            }
            switchFragment.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, switchFragment);
            transaction.addToBackStack(null);
            transaction.commit();

            drawerLayout.closeDrawer(Gravity.START);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }
}
