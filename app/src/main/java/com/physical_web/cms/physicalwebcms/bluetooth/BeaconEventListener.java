package com.physical_web.cms.physicalwebcms.bluetooth;

import android.bluetooth.BluetoothDevice;

import java.util.List;

/**
 * This interface must be implemented to receive data from the BluetoothManager class.
 */
public interface BeaconEventListener {
    /**
     * Returns list of configurable Eddystone com.physical_web.cms.physicalwebcms.beacons after scan complete.
     *
     * @param beacons Beacons in configuration mode
     */
    void onScanComplete(List<BluetoothDevice> beacons);

    /**
     * Returns result of beacon URI update attempt.
     *
     * @param device beacon which operation was attempted on.
     * @param status WRITE_SUCCESS if succesful
     */
    void uriWriteCallback(BluetoothDevice device, int status);
}
