package com.physical_web.cms.physicalwebcms;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

/**
 * Created by Kayali on 2017-06-18.
 */

public class BluetoothManager {
    private BluetoothAdapter bluetoothAdapter;

    public BluetoothManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public Boolean deviceSupportsBT() {
        return (BluetoothAdapter.getDefaultAdapter() != null);
    }

    public Boolean bluetoothIsEnabled() {
        return bluetoothAdapter.isEnabled();
    }
}
