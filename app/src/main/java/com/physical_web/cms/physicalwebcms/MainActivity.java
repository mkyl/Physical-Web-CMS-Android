package com.physical_web.cms.physicalwebcms;

import android.bluetooth.BluetoothDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    BluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO add this to menu
        // Intent entrollmentIntent = new Intent(this, EnrollmentActivity.class);
        // startActivity(entrollmentIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ensure drive authorization, internet connection, etc. are all set up
        SetupManager setupManager = new SetupManager(this);
        setupManager.checkRequirements();
    }
}
