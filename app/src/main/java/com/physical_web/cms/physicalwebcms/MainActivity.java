package com.physical_web.cms.physicalwebcms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Physical Web CMS";
    
    BeaconDBManager beaconDBManager;
    BluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // open database
        //beaconDBManager = new BeaconDBManager(this);

        Intent entrollmentIntent = new Intent(this, EnrollmentActivity.class);
        startActivity(entrollmentIntent);
    }

    @Override
    protected void onDestroy() {
        // close database
        //beaconDBManager.close();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ensure drive authorization, internet connection, etc. are all set up
        SetupManager setupManager = new SetupManager(this);
        setupManager.checkRequirements();

        beaconDBManager = new BeaconDBManager(this);
        ((TextView)findViewById(R.id.helloWorld)).setText("Enrolled beacons:\n " + beaconDBManager.getAllBeacons().toString());
        beaconDBManager.close();
    }
}
