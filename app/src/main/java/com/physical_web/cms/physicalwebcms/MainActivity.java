package com.physical_web.cms.physicalwebcms;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private String[] menuTitles;
    private DrawerLayout drawerLayout;
    private ListView drawerList;

    private SetupManager setupManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menuTitles = getResources().getStringArray(R.array.navigation_drawer_items);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.drawer_top_list);

        drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, menuTitles));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ensure drive authorization, internet connection, etc. are all set up
        if(setupManager == null) {
            setupManager = new SetupManager(this);
        }
        setupManager.checkRequirements();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String itemName = menuTitles[position];
            switchActivity(itemName);
        }

        private void switchActivity(String name) {
            Intent switchItent;
            switch(name) {
                case "Exhibits":
                    throw new UnsupportedOperationException("Not implemented yet");
                case "Beacons":
                    switchItent = new Intent(MainActivity.this, EnrollmentActivity.class);
                    break;
                case "Analytics":
                    throw new UnsupportedOperationException("Not implemented yet");
                case "Settings":
                    throw new UnsupportedOperationException("Not implemented yet");
                case "About":
                    throw new UnsupportedOperationException("Not implemented yet");
                default:
                    throw new RuntimeException("Unimplemented menu item");
            }
            startActivity(switchItent);
        }
    }
}

