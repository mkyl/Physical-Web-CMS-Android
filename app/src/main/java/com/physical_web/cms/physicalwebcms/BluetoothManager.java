package com.physical_web.cms.physicalwebcms;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Kayali on 2017-06-18.
 */

public class BluetoothManager {
    private BluetoothAdapter bluetoothAdapter;

    public BluetoothManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
        }
    }

    private void setupPhase() {
    }

    public Boolean deviceHasBluetoothAdapter() {
        return (BluetoothAdapter.getDefaultAdapter() != null);
    }

    public Boolean bluetoothIsEnabled() {
        return bluetoothAdapter.isEnabled();
    }
}
