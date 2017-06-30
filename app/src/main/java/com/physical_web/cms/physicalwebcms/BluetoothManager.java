package com.physical_web.cms.physicalwebcms;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.GATT;

/**
 * Handles connecting to beacons that utilize Bluetooth Low Energy.
 */
public class BluetoothManager {
    public final static int WRITE_SUCCESS = 0;
    public final static int WRITE_FAIL_NO_CONNECTION = 1;
    public final static int WRITE_FAIL_NOT_EDDYSTONE = 2;
    public final static int WRITE_FAIL_LOCKED = 3;
    public final static int WRITE_FAIL_OTHER = 4;


    private final static String TAG = BluetoothManager.class.getSimpleName();
    private final static int SCAN_PERIOD = 5000; // milliseconds

    private final static UUID EDDYSTONE_CONFIGURATION_SERVICE =
            UUID.fromString("a3c87500-8ed3-4bdf-8a39-a01bebede295");
    private final static UUID EDDYSTONE_LOCK_STATE_CHARACTERISTIC =
            UUID.fromString("a3c87506-8ed3-4bdf-8a39-a01bebede295");
    private final static UUID EDDYSTONE_ADV_SLOT_CHARACTERISTIC =
            UUID.fromString("a3c8750a-8ed3-4bdf-8a39-a01bebede295");
    private final static UUID NON_EXISTANT_CHARACTERISTIC =
            UUID.fromString("a3c27e0a-9ed3-4bdf-8a39-a01b6bedef95");

    private final static byte[] EDDYSTONE_LOCKED = new byte[] {(byte) 0x00};
    private final static byte[] EDDYSTONE_UNLOCKED = new byte[] {(byte) 0x01};
    private final static byte[] EDDYSTONE_UNLOCKED_WITH_NO_RELOCK = new byte[] {(byte) 0x02};

    private final static byte EDDYSTONE_URL_FRAMETYPE = (byte) 0x10;
    //private final static byte EDDYSTONE_NORMAL_TX_POWER = (byte) 0xEE;
    private final static byte EDDSTONE_URL_HTTPS_PREFIX = (byte) 0x03; //prefix for "https://"

    private Activity context;
    private BluetoothAdapter bluetoothAdapter;
    private Handler scanHandler;

    private List<BluetoothDevice> scannedDevices;
    private Map<BluetoothDevice, EddstoneBeaconEvents> eventParentMap = new HashMap<>();
    private String shortenedUri;
    private byte[] newFrameValue = new byte[19];

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
    public void listConfigurableEddystoneBeacons(final EddstoneBeaconEvents event) {
        // in case there are old beacons stored in the device list
        scannedDevices.clear();
        UUID[] desiredServices = new UUID[] {EDDYSTONE_CONFIGURATION_SERVICE};

        bluetoothAdapter.startLeScan(desiredServices, listBeaconsCallback);
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
                    if (!deviceSeenBefore(device))
                        scannedDevices.add(device);
                }
            };

    /**
     * Indicates whether a particular bluetooth device has already been seen in the
     * ongoing scan.
     *
     * @param device subject of inquiry
     * @return whether device has been seen in latest scan
     */
    private Boolean deviceSeenBefore(BluetoothDevice device) {
        for (BluetoothDevice earlierDevice : scannedDevices)
            if (earlierDevice.getAddress().equals(device.getAddress()))
                return true;
        return false;
    }

    /**
     * Indicates whether a particular bluetooth device supports the Eddystone URL protocol. Requires
     * beacon to be in range.
     *
     * @param beacon subject of inquiry
     */
    public void updateBeaconUri(final BluetoothDevice beacon, final String Uri,
                                EddstoneBeaconEvents event) {
        this.shortenedUri = shortenIfNeeded(Uri);
        eventParentMap.put(beacon, event);
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                beacon.connectGatt(context, false, checkBeaconCallback);
            }
        });
    }

    // called when status of GATT connection to the beacon in supportsEddystoneURL is updated
    private final BluetoothGattCallback checkBeaconCallback = new BluetoothGattCallback() {
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
            BluetoothDevice device = gatt.getDevice();
            if (status == GATT_SUCCESS) {
                BluetoothGattService eddystoneConfig =
                        gatt.getService(EDDYSTONE_CONFIGURATION_SERVICE);
                if (eddystoneConfig != null) {
                    BluetoothGattCharacteristic lockCharacteristic =
                            eddystoneConfig.getCharacteristic(EDDYSTONE_LOCK_STATE_CHARACTERISTIC);
                    gatt.readCharacteristic(lockCharacteristic);
                    // continues in onCharacteristicRead
                } else {
                    eventParentMap.get(device).uriWriteCallback(device, WRITE_FAIL_NOT_EDDYSTONE);
                    gatt.close();
                }
            } else {
                Log.w(TAG, "Could not connect to beacon with address: " + device.getAddress());
                eventParentMap.get(device).uriWriteCallback(device, WRITE_FAIL_NO_CONNECTION);
                gatt.close();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if(characteristic.getUuid().equals(EDDYSTONE_LOCK_STATE_CHARACTERISTIC)) {
                BluetoothDevice device = gatt.getDevice();
                byte[] lockState = characteristic.getValue();

                if (Arrays.equals(lockState, EDDYSTONE_UNLOCKED) ||
                        Arrays.equals(lockState, EDDYSTONE_UNLOCKED_WITH_NO_RELOCK)) {
                    // device is unlocked
                    writeToBeacon(gatt, status);
                } else {
                    eventParentMap.get(device).uriWriteCallback(device, WRITE_FAIL_LOCKED);
                    gatt.close();
                }
            }
        }

        // New services discovered
        private void writeToBeacon(BluetoothGatt gatt, int status) {
            BluetoothGattService eddystoneConfig =
                    gatt.getService(EDDYSTONE_CONFIGURATION_SERVICE);
            BluetoothGattCharacteristic dataSlot =
                    eddystoneConfig.getCharacteristic(EDDYSTONE_ADV_SLOT_CHARACTERISTIC);

            byte[] framePrefix = new byte[] {EDDYSTONE_URL_FRAMETYPE, EDDSTONE_URL_HTTPS_PREFIX};
            byte[] frameStoredURI = shortenedUri.getBytes(Charset.forName("ASCII"));

            for(int i = 0; i < newFrameValue.length; i++) {
                newFrameValue[i] = 0x20;
            }

            // newFrameValue = framePrefix + frameStoredURI
            System.arraycopy(framePrefix, 0, newFrameValue, 0, framePrefix.length);
            System.arraycopy(frameStoredURI, 0, newFrameValue, framePrefix.length,
                    frameStoredURI.length);

            dataSlot.setValue(newFrameValue);

            gatt.beginReliableWrite();
            gatt.writeCharacteristic(dataSlot);
        }

        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           int status) {
            EddstoneBeaconEvents eventToCallback = eventParentMap.get(gatt.getDevice());
            BluetoothDevice device = gatt.getDevice();

            if (status == GATT_SUCCESS) {
                if (Arrays.equals(characteristic.getValue(), newFrameValue)) {
                    gatt.executeReliableWrite();
                    eventToCallback.uriWriteCallback(device, WRITE_SUCCESS);
                } else {
                    // beacon didn't recieve data correctly
                    Log.e(TAG, "Beacon recieved corrupted data");
                    gatt.abortReliableWrite(device);
                    eventToCallback.uriWriteCallback(device, WRITE_FAIL_OTHER);
                }
            } else {
                eventToCallback.uriWriteCallback(device, WRITE_FAIL_OTHER);
            }

            gatt.close();
        }

    };

    private String shortenIfNeeded(String Uri) {
        if(Uri.getBytes(Charset.forName("ASCII")).length < 17) {
            return Uri;
        } else {
            // TODO connect to google shortner API
            throw new UnsupportedOperationException("URIs over 17 bytes large must be shortned.");
        }
    }
}

interface EddstoneBeaconEvents {
    void onScanComplete(List<BluetoothDevice> beacons);
    void uriWriteCallback(BluetoothDevice device, int status);
}
