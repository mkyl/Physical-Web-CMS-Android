package org.physical_web.cms.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Handler;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.urlshortener.Urlshortener;
import com.google.api.services.urlshortener.model.Url;
import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static org.physical_web.cms.bluetooth.BeaconEventListener.WRITE_FAIL_LOCKED;
import static org.physical_web.cms.bluetooth.BeaconEventListener.WRITE_FAIL_NOT_EDDYSTONE;
import static org.physical_web.cms.bluetooth.BeaconEventListener.WRITE_FAIL_NO_CONNECTION;
import static org.physical_web.cms.bluetooth.BeaconEventListener.WRITE_FAIL_OTHER;
import static org.physical_web.cms.bluetooth.BeaconEventListener.WRITE_SUCCESS;

/**
 * Handles discovering and configuring to Eddystone beacons over a Bluetooth Low Energy link.
 */
public class BluetoothManager {
    private final static String TAG = BluetoothManager.class.getSimpleName();
    private final static int SCAN_PERIOD = 15000; // milliseconds

    private final static UUID EDDYSTONE_CONFIGURATION_SERVICE =
            UUID.fromString("a3c87500-8ed3-4bdf-8a39-a01bebede295");
    private final static UUID EDDYSTONE_LOCK_STATE_CHARACTERISTIC =
            UUID.fromString("a3c87506-8ed3-4bdf-8a39-a01bebede295");
    private final static UUID EDDYSTONE_ACTIVE_SLOT_CHARACTERISTIC =
            UUID.fromString("a3c87502-8ed3-4bdf-8a39-a01bebede295");
    private final static UUID EDDYSTONE_ADV_SLOT_CHARACTERISTIC =
            UUID.fromString("a3c8750a-8ed3-4bdf-8a39-a01bebede295");

    private final static byte[] EDDYSTONE_LOCKED = new byte[]{(byte) 0x00};
    private final static byte[] EDDYSTONE_UNLOCKED = new byte[]{(byte) 0x01};
    private final static byte[] EDDYSTONE_UNLOCKED_WITH_NO_RELOCK = new byte[]{(byte) 0x02};

    private final static byte EDDYSTONE_URL_FRAMETYPE = (byte) 0x10;
    private final static byte EDDSTONE_URL_HTTPS_PREFIX = (byte) 0x03; // prefix for "https://"

    private final static byte ASCII_SPACE = (byte) 0x20;
    private final static int MAX_URI_LENGTH = 16;

    // Let me save you some trouble: don't try stealing this API key,
    // it's restricted to the developers' debugging keychain
    private final static String API_KEY = "AIzaSyBGSGDdaottaW0DZQJB8284BK4YWg-fCxA";

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private Handler scanHandler;

    private List<BluetoothDevice> scannedDevices;
    private Map<BluetoothDevice, BeaconEventListener> eventParentMap = new HashMap<>();
    private BeaconEventListener scanListener;
    private String shortenedUri;
    private byte[] newFrameValue = new byte[19];

    public BluetoothManager(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.scanHandler = new Handler(context.getMainLooper());
        this.scannedDevices = new ArrayList<>();
    }

    /**
     * Indicates whether the device is equipped with a Bluetooth Low Energy adapter.
     */
    public Boolean deviceSupportsBLE() {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Returns true only if a bluetooth adapter exists and is on.
     * @return
     */
    public Boolean bluetoothIsEnabled() {
        return (bluetoothAdapter != null && bluetoothAdapter.isEnabled());
    }

    /**
     * Searches for nearby Bluetooth Low Energy devices for SCAN_PERIOD milliseconds. Returns
     * results in a list.
     *
     * @param event Callback called when scan complete
     */
    public void listConfigurableEddystoneBeacons(final BeaconEventListener event) {
        // in case there are old beacons stored in the device list
        scannedDevices.clear();
        UUID[] desiredServices = new UUID[]{EDDYSTONE_CONFIGURATION_SERVICE};
        scanListener = event;

        bluetoothAdapter.startLeScan(desiredServices, listBeaconsCallback);
        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BluetoothAdapter.getDefaultAdapter().stopLeScan(listBeaconsCallback);
                event.onScanComplete();
            }
        }, SCAN_PERIOD);
    }

    // called when the scan for nearby beacons is complete
    private final BluetoothAdapter.LeScanCallback listBeaconsCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (!deviceSeenBefore(device)) {
                        scannedDevices.add(device);
                        scanListener.onConfigurableBeaconFound(device);
                    }
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
     * Configures the passed beacon to broadcast the given URI. Result of configuration returned
     * through callback
     *
     * @param beacon        Eddystone beacon to be updated
     * @param Uri           ASCII encoded webpage address
     * @param eventListener where callbacks will be returned
     */
    public void updateBeaconUri(final BluetoothDevice beacon, final String Uri,
                                BeaconEventListener eventListener) {
        this.shortenedUri = shortenIfNeeded(Uri);
        Log.d(TAG, "Shortened URI: " + shortenedUri);
        eventParentMap.put(beacon, eventListener);
        if (context instanceof Activity) {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    beacon.connectGatt(context, false, updateUriCallback);
                }
            });
        } else {
            beacon.connectGatt(context, false, updateUriCallback);
        }
    }

    // actions to be taken when connection to beacon is made
    private final BluetoothGattCallback updateUriCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close();
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
                    // beacon doesn't support eddystone configuration service
                    eventParentMap.get(device).uriWriteCallback(device, WRITE_FAIL_NOT_EDDYSTONE);
                    gatt.disconnect();
                }
            } else {
                Log.w(TAG, "Could not connect to beacon with address: " + device.getAddress());
                eventParentMap.get(device).uriWriteCallback(device, WRITE_FAIL_NO_CONNECTION);
                gatt.disconnect();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (characteristic.getUuid().equals(EDDYSTONE_LOCK_STATE_CHARACTERISTIC)) {
                BluetoothDevice device = gatt.getDevice();
                byte[] lockState = characteristic.getValue();

                if (Arrays.equals(lockState, EDDYSTONE_UNLOCKED) ||
                        Arrays.equals(lockState, EDDYSTONE_UNLOCKED_WITH_NO_RELOCK)) {
                    // device is unlocked
                    writeToBeacon(gatt, status);
                } else {
                    eventParentMap.get(device).uriWriteCallback(device, WRITE_FAIL_LOCKED);
                    gatt.disconnect();
                }
            }
        }

        // all conditions needed to configure beacon met, write advertisement frame
        private void writeToBeacon(BluetoothGatt gatt, int status) {
            BluetoothGattService eddystoneConfig =
                    gatt.getService(EDDYSTONE_CONFIGURATION_SERVICE);
            BluetoothGattCharacteristic dataSlot =
                    eddystoneConfig.getCharacteristic(EDDYSTONE_ADV_SLOT_CHARACTERISTIC);

            byte[] framePrefix = new byte[]{EDDYSTONE_URL_FRAMETYPE, EDDSTONE_URL_HTTPS_PREFIX};
            byte[] frameStoredURI = shortenedUri.getBytes(Charset.forName("ASCII"));

            for (int i = 0; i < newFrameValue.length; i++) {
                // fill unused bytes with spaces
                newFrameValue[i] = ASCII_SPACE;
            }

            // newFrameValue = framePrefix + frameStoredURI
            System.arraycopy(framePrefix, 0, newFrameValue, 0, framePrefix.length);
            System.arraycopy(frameStoredURI, 0, newFrameValue, framePrefix.length,
                    frameStoredURI.length);

            dataSlot.setValue(newFrameValue);

            // this will send the data over to the beacon and ask it to confirm with us
            // what it received before applying any changes
            gatt.beginReliableWrite();
            gatt.writeCharacteristic(dataSlot);
        }

        // called because we're using reliable write
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            BeaconEventListener eventToCallback = eventParentMap.get(gatt.getDevice());
            BluetoothDevice device = gatt.getDevice();

            if (status == GATT_SUCCESS) {
                if (Arrays.equals(characteristic.getValue(), newFrameValue)) {
                    // data on beacon side matches our side, proceed
                    gatt.executeReliableWrite();
                    // continues in onReliableWriteCompleted
                } else {
                    // beacon didn't recieve data correctly
                    Log.e(TAG, "Beacon recieved corrupted data");
                    gatt.abortReliableWrite(device);
                    eventToCallback.uriWriteCallback(device, WRITE_FAIL_OTHER);
                    gatt.disconnect();
                }
            } else {
                eventToCallback.uriWriteCallback(device, WRITE_FAIL_OTHER);
                gatt.disconnect();
            }

        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            BeaconEventListener eventToCallback = eventParentMap.get(gatt.getDevice());
            BluetoothDevice device = gatt.getDevice();

            if (status == GATT_SUCCESS) {
                eventToCallback.uriWriteCallback(device, WRITE_SUCCESS);
            } else {
                eventToCallback.uriWriteCallback(device, WRITE_FAIL_OTHER);
            }
            gatt.disconnect();
        }
    };

    // due to length constraints, URIs of over MAX_URI_LENGTH bytes length must be shortened
    // returns shortened Uri WITHOUT https:// prefix, null if shortening fails
    public String shortenIfNeeded(String Uri) {
        if (!Uri.startsWith("https://"))
            throw new IllegalArgumentException("Must be HTTPS URI due to Eddystone spec");

        String uriWithoutPrefix = Uri.substring("https://".length());

        if (uriWithoutPrefix.getBytes(Charset.forName("ASCII")).length < MAX_URI_LENGTH) {
            return uriWithoutPrefix;
        } else {
            // we have to shorten it

            Urlshortener.Builder builder = new Urlshortener.Builder(AndroidHttp
                    .newCompatibleTransport(), AndroidJsonFactory.getDefaultInstance(),
                    // this portion is required because the API key is restricted
                    new HttpRequestInitializer() {
                        @Override
                        public void initialize(HttpRequest request) throws IOException {
                            String packageName = context.getPackageName();
                            String SHA1 = getSHA1OfPackage(packageName);

                            request.getHeaders().set("X-Android-Package", packageName);
                            request.getHeaders().set("X-Android-Cert", SHA1);
                        }
                    });

            Urlshortener urlshortener = builder.build();

            com.google.api.services.urlshortener.model.Url url = new Url();
            url.setLongUrl(Uri);

            try {
                url = urlshortener.url()
                        .insert(url)
                        .setKey(API_KEY)
                        .execute();

                String shortenedUri = url.getId();
                if (shortenedUri.startsWith("https://"))
                    return shortenedUri.substring("https://".length());
                else
                    throw new IllegalStateException("Google URI shortner provided non https URI");
            } catch (IOException e) {
                throw new IllegalStateException("Uri Shortening failed");
            }
        }
    }

    private String getSHA1OfPackage(String packageName){
        try {
            Signature[] signatures = context.getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES).signatures;
            for (Signature signature: signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA-1");
                md.update(signature.toByteArray());
                return BaseEncoding.base16().encode(md.digest());
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}

