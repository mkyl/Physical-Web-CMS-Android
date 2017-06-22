package com.physical_web.cms.physicalwebcms;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.flipboard.bottomsheet.BottomSheetLayout;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class EnrollmentActivity extends AppCompatActivity {
    // internal routing codes
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_LOCATION = 2;

    private static final long SCAN_PERIOD = 10000;

    private BluetoothManager bluetoothManager;
    private List<BluetoothDevice> bluetoothDevices;
    private Handler scanHandler = new Handler();
    private BeaconAdapter beaconListAdapter;
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrollment);

        bluetoothManager = new BluetoothManager();
        displayBTErrorSnackBar();

        bluetoothDevices = new ArrayList<>();
        beaconListAdapter = new BeaconAdapter();
    }

    public void enableBluetooth(View view) {
        displayWaitingUI();
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)  {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK)
                    enableNetworkIfNeeded();
                else
                    displayUserWarning();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch(requestCode) {
            case REQUEST_ENABLE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    prepareForScan();
                } else
                    displayUserWarning();
                break;
        }
    }

    private void enableNetworkIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ENABLE_LOCATION);
            } else {
                prepareForScan();
            }
        } else {
            prepareForScan();
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(!isDuplicate(device)) {
                bluetoothDevices.add(device);
                findViewById(R.id.scanningZeroDevicesBar).setVisibility(View.INVISIBLE);
                findViewById(R.id.noDevicesYetText).setVisibility(View.INVISIBLE);
                findViewById(R.id.scannedBeaconsList).setVisibility(View.VISIBLE);
                // TODO update list
                beaconListAdapter.notifyDataSetChanged();
            }
        }
    };

    private boolean isDuplicate(BluetoothDevice device) {
        for (BluetoothDevice storedDevice: bluetoothDevices) {
            if (device.getAddress().equalsIgnoreCase(storedDevice.getAddress()))
                return true;
        }
        return false;
    }

    public void prepareForScan() {
        prepareForScan(null);
    }

    public void prepareForScan(final View v) {
        displayBeaconEnrollment();
        ((ListView) findViewById(R.id.scannedBeaconsList)).setAdapter(beaconListAdapter);
        ((ListView) findViewById(R.id.scannedBeaconsList)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice selectedDevice = bluetoothDevices.get(position);

                BottomSheetLayout bottomSheet = (BottomSheetLayout) findViewById(R.id.bottomsheet);
                bottomSheet.setBackgroundColor(Color.WHITE);
                bottomSheet.showWithSheetView(LayoutInflater.from(view.getContext()).inflate(R.layout.add_beacon, bottomSheet, false));
                ((TextView) findViewById(R.id.editBeaconNameText)).setText(selectedDevice.getName());
                ((TextView) findViewById(R.id.textBeaconAddress)).setText(selectedDevice.getAddress());
            }

        });
        startScan();
    }

    public void onAddBeacon(View v) {
        BeaconDBManager beaconDBManager = new BeaconDBManager(v.getContext());
        String name = ((TextView) findViewById(R.id.editBeaconNameText)).getText().toString();
        String address = ((TextView) findViewById(R.id.textBeaconAddress)).getText().toString();
        Beacon newBeacon = new Beacon(address, name);
        beaconDBManager.addBeacon(newBeacon);

        ((BottomSheetLayout) findViewById(R.id.bottomsheet)).dismissSheet();
        beaconDBManager.close();
    }

    private void startScan() {
        BluetoothAdapter.getDefaultAdapter().startLeScan(leScanCallback);
        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BluetoothAdapter.getDefaultAdapter().stopLeScan(leScanCallback);
                if(bluetoothDevices.isEmpty()) {
                    noDevicesFound();
                }
            }
        }, SCAN_PERIOD);
    }

    private void noDevicesFound() {
        findViewById(R.id.scanningZeroDevicesBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.noDevicesYetText).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.noDevicesYetText)).setText("Nothing found");
        findViewById(R.id.retryScanButton).setVisibility(View.VISIBLE);
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

    private void displayWaitingUI() {
        findViewById(R.id.userWarning).setVisibility(View.INVISIBLE);
        findViewById(R.id.bluetoothBar).setVisibility(View.VISIBLE);
        findViewById(R.id.enableButton).setEnabled(false);
    }

    private void displayBeaconEnrollment() {
        // out the old
        findViewById(R.id.bluetoothIcon).setVisibility(View.INVISIBLE);
        findViewById(R.id.btDescription).setVisibility(View.INVISIBLE);
        findViewById(R.id.userWarning).setVisibility(View.INVISIBLE);
        findViewById(R.id.bluetoothBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.enableButton).setVisibility(View.INVISIBLE);
        findViewById(R.id.retryScanButton).setVisibility(View.INVISIBLE);

        // in with the new!
        findViewById(R.id.scanningZeroDevicesBar).setVisibility(View.VISIBLE);
        findViewById(R.id.noDevicesYetText).setVisibility(View.VISIBLE);
        findViewById(R.id.scannedBeaconsTitle).setVisibility(View.VISIBLE);
        findViewById(R.id.scannedBeaconsList).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.noDevicesYetText)).setText("Seaching for beacons...");
    }

    private void displayUserWarning() {
        findViewById(R.id.bluetoothBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.userWarning).setVisibility(View.VISIBLE);
        findViewById(R.id.enableButton).setEnabled(true);
    }

    class BeaconAdapter extends BaseAdapter {
        private LayoutInflater inflater = (LayoutInflater) EnrollmentActivity.this
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        private List<BluetoothDevice> bluetoothDevices = EnrollmentActivity.this.bluetoothDevices;

        @Override
        public int getCount() {
            return bluetoothDevices.size();
        }

        @Override
        public Object getItem(int position) {
            return bluetoothDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BluetoothDevice beacon = (BluetoothDevice) getItem(position);
            ViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.simple_beacon_item, parent, false);

                holder = new ViewHolder();
                holder.nameTextView = (TextView) convertView.findViewById(R.id.beaconName);
                holder.addressTextView = (TextView) convertView.findViewById(R.id.beaconAddress);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if(beacon.getName() == null || beacon.getName().equals(""))
                holder.nameTextView.setText("Unnamed Beacon");
            else
               holder.nameTextView.setText(beacon.getName());

            holder.addressTextView.setText(beacon.getAddress());

            return convertView;
        }
    }

    private static class ViewHolder {
        public TextView nameTextView;
        public TextView addressTextView;
    }
}


