package com.physical_web.cms.physicalwebcms;

import android.bluetooth.BluetoothDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements EddstoneBeaconEvents {
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
    }

    public void checkGatt(View v) {
        bluetoothManager = new BluetoothManager(this);
        bluetoothManager.listConfigurableEddystoneBeacons(this);
    }

    @Override
    public void onScanComplete(List<BluetoothDevice> beacons) {
        for(BluetoothDevice device : beacons) {
            Log.d(TAG, "Connecting to device with address: " + device.getAddress());
            //bluetoothManager.supportsEddystoneURL(device, this);
            bluetoothManager.updateBeaconUri(device, "example.com", this);
        }
    }

    @Override
    public void uriWriteCallback (BluetoothDevice device, final int status) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.helloWorld)).setText("Updated URI status = " + status);
            }
        });
    };
}
