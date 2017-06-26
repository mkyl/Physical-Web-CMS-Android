package com.physical_web.cms.physicalwebcms;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles connecting to beacons that utilize Bluetooth Low Energy.
 */
public class BluetoothManager {
    private final static String TAG = BluetoothManager.class.getSimpleName();
    private final static int SCAN_PERIOD = 10000; // milliseconds
    private final static UUID EDDYSTONE_CONFIGURATION_SERVICE =
            UUID.fromString("a3c87500-8ed3-4bdf-8a39-a01bebede295");

    private Activity context;
    private BluetoothAdapter bluetoothAdapter;
    private Handler scanHandler;

    private List<BluetoothDevice> scannedDevices;

    public BluetoothManager(Activity context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.scanHandler = new Handler(context.getMainLooper());
        this.scannedDevices = new ArrayList<>();
    }

    /**
     * Searches for nearby Bluetooth Low Energy devices for SCAN_PERIOD milliseconds. Returns
     * results in a list.
     *
     * @param event Callback called when scan complete
     */
    public void listBeacons(final findBeaconsScanComplete event) {
        // in case there are old beacons stored in the device list
        scannedDevices.clear();

        bluetoothAdapter.startLeScan(listBeaconsCallback);
        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BluetoothAdapter.getDefaultAdapter().stopLeScan(listBeaconsCallback);
                event.onScanComplete(scannedDevices);
            }
        }, SCAN_PERIOD);
    }

    // called when the scan for nearby beacons is complete
    private final BluetoothAdapter.LeScanCallback listBeaconsCallback =
            new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(!deviceSeenBefore(device))
                scannedDevices.add(device);
        }
    };

    /**
     * Indicates whether a particular bluetooth device has already been seen in the
     * ongoing scan.
     * @param device subject of inquiry
     * @return whether device has been seen in latest scan
     */
    private Boolean deviceSeenBefore(BluetoothDevice device) {
        for(BluetoothDevice earlierDevice: scannedDevices)
            if(earlierDevice.getAddress().equals(device.getAddress()))
                return true;
        return false;
    }

    /**
     * Indicates whether a particular bluetooth device supports the Eddystone URL protocol. Requires
     * beacon to be in range.
     *
     * @param beacon subject of inquiry
     */
    public void supportsEddystoneURL(final BluetoothDevice beacon) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BluetoothGatt bluetoothGatt = beacon.connectGatt(context, false, eddystoneURLCallback);
            }
        });
    }

    // called when status of GATT connection to the beacon in supportsEddystoneURL is updated
    private final BluetoothGattCallback eddystoneURLCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (gatt.getService(EDDYSTONE_CONFIGURATION_SERVICE) != null)
                    Log.d(TAG, "Found eddystone configuration service");
            } else {
                Log.w(TAG, "didn't quite work");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Found new service with UUID: " + characteristic.getUuid());
            }
        }
    };
}

interface findBeaconsScanComplete {
    public void onScanComplete(List<BluetoothDevice> beacons);
}
