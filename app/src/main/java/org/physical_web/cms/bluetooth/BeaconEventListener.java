package org.physical_web.cms.bluetooth;

import android.bluetooth.BluetoothDevice;

/**
 * This interface must be implemented to receive data from the BluetoothManager class.
 */
public interface BeaconEventListener {
    int WRITE_SUCCESS = 0;
    int WRITE_FAIL_NO_CONNECTION = 1;
    int WRITE_FAIL_NOT_EDDYSTONE = 2;
    int WRITE_FAIL_LOCKED = 3;
    int WRITE_FAIL_OTHER = 4;

    /**
     * Returns a configurable Eddystone beacons when it is first detected in a scan.
     *
     * @param device Beacons in configuration mode
     */
    void onConfigurableBeaconFound(BluetoothDevice device);

    /**
     * Returns result of beacon URI update attempt.
     *
     * @param device beacon which operation was attempted on.
     * @param status WRITE_SUCCESS if successful
     */
    void uriWriteCallback(BluetoothDevice device, int status);

    /**
     * Indicates scan is finished
     */
    void onScanComplete();
}
