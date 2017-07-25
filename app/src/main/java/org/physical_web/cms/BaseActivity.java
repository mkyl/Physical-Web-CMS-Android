package org.physical_web.cms;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.os.Bundle;
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

import org.physical_web.cms.beacons.BeaconFragment;
import org.physical_web.cms.beacons.BeaconManager;
import org.physical_web.cms.exhibits.ExhibitFragment;
import org.physical_web.cms.exhibits.ExhibitManager;
import org.physical_web.cms.setup.SetupManager;
import org.physical_web.cms.sync.ContentSynchronizer;

import java.io.File;

public class BaseActivity extends AppCompatActivity {
    private static final String TAG = BaseActivity.class.getSimpleName();

    private String[] menuTitles;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;

    private SetupManager setupManager;
    private BeaconManager beaconManager;
    private FileManager fileManager;
    private ContentSynchronizer contentSynchronizer;

    private ExhibitManager exhibitManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        setupManager = new SetupManager(this);
        fileManager = new FileManager(this);

        beaconManager = BeaconManager.getInstance();
        beaconManager.setContext(this);

        exhibitManager = ExhibitManager.getInstance();
        exhibitManager.setContext(this);

        File folderToSync = fileManager.getRootFolder();
        contentSynchronizer = ContentSynchronizer.getInstance();
        contentSynchronizer.init(this, folderToSync);

        setupNavigationDrawer();
        setupActionBar();
        // if statement to avoid overlapping fragments
        if (savedInstanceState == null)
            setupWelcomeFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupManager.checkRequirements(); // ensure drive authorization setup
        contentSynchronizer.connectReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        contentSynchronizer.disconnectReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        beaconManager.closeAndSave();
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
        getFragmentManager().beginTransaction().
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
            Fragment switchFragment;
            switch(name) {
                case "Home":
                    switchFragment = new WelcomeFragment();
                    break;
                case "Exhibits":
                    switchFragment = new ExhibitFragment();
                    break;
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

            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, switchFragment);
            transaction.addToBackStack(null);
            transaction.commit();

            drawerLayout.closeDrawer(Gravity.START);
        }
    }
}
