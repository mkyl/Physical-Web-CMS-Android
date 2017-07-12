package com.physical_web.cms.physicalwebcms.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface BeaconScanningListener {
    void onBeaconDiscovered(BluetoothDevice beacon);
}
