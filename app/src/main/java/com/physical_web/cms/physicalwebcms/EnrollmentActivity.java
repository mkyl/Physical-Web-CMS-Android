package com.physical_web.cms.physicalwebcms;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class EnrollmentActivity extends AppCompatActivity {
    // internal routing codes
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothManager bluetoothManager;
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrollment);

        bluetoothManager = new BluetoothManager();
        displayBTErrorSnackBar();
    }

    public void enableBluetooth(View view) {
        displayWaitingUI();
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK)
                    displayBeaconEnrollment();
                else
                    displayUserWarning();
                break;
        }
    }

    private void displayWaitingUI() {
        findViewById(R.id.userWarning).setVisibility(View.INVISIBLE);
        findViewById(R.id.bluetoothBar).setVisibility(View.VISIBLE);
        findViewById(R.id.enableButton).setEnabled(false);
    }

    private void displayBeaconEnrollment() {
        // hide the old
        findViewById(R.id.bluetoothIcon).setVisibility(View.INVISIBLE);
        findViewById(R.id.btDescription).setVisibility(View.INVISIBLE);
        findViewById(R.id.userWarning).setVisibility(View.INVISIBLE);
        findViewById(R.id.bluetoothBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.enableButton).setVisibility(View.INVISIBLE);

        // out with the new!
        findViewById(R.id.scanningZeroDevicesBar).setVisibility(View.VISIBLE);
        findViewById(R.id.noDevicesYetText).setVisibility(View.VISIBLE);
    }

    private void displayUserWarning() {
        findViewById(R.id.bluetoothBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.userWarning).setVisibility(View.VISIBLE);
        findViewById(R.id.enableButton).setEnabled(true);
    }

    private void displayBTErrorSnackBar() {
        if (!bluetoothManager.deviceHasBluetoothAdapter()) {
            snackbar = Snackbar
                    .make(findViewById(R.id.enrollmentLayout), "This device does not support Bluetooth",
                            Snackbar.LENGTH_INDEFINITE)
                    .setAction("RETRY", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            displayBTErrorSnackBar();
                        }
                    });
            snackbar.show();

            findViewById(R.id.enableButton).setEnabled(false);
        } else {
            if (snackbar != null)
                snackbar.dismiss();
            findViewById(R.id.enableButton).setEnabled(true);
        }
    }
}
