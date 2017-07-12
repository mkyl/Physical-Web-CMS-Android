package org.physical_web.cms.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface BeaconScanningListener {
    void onBeaconDiscovered(BluetoothDevice beacon);
}
