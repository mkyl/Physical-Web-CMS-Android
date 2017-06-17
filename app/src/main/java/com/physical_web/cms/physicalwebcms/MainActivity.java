package com.physical_web.cms.physicalwebcms;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    BeaconDBManager beaconDBManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // open database
        beaconDBManager = new BeaconDBManager(this);
    }

    @Override
    protected void onDestroy() {
        // close database
        beaconDBManager.close();
        super.onDestroy();
    }
}
