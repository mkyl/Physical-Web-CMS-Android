package com.physical_web.cms.physicalwebcms;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;

import java.util.UUID;

/**
 * Created by Kayali on 2017-06-18.
 */

public class BluetoothManager {
    // TODO find the right format for thhe eddystone UUID
    private static final UUID[] EDDYSTONE_UUID = {new UUID(0xFE, 0xAA)};
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothAdapter.LeScanCallback scanCallback;

    public BluetoothManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
        }
    }

    public Boolean deviceHasBluetoothAdapter() {
        return (BluetoothAdapter.getDefaultAdapter() != null);
    }
}
