package org.physical_web.cms.beacons;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.flipboard.bottomsheet.BottomSheetLayout;

import org.physical_web.cms.R;
import org.physical_web.cms.exhibits.ExhibitManager;

import java.util.ArrayList;
import java.util.List;

/**
 * This class allows the user to enroll new beacons into the beacon DB.
 */
public class EnrollmentActivity extends AppCompatActivity {
    private final static String TAG = EnrollmentActivity.class.getSimpleName();
    // internal routing codes
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_LOCATION = 2;

    private static final long SCAN_PERIOD = 10000;

    private List<BluetoothDevice> bluetoothDevices;
    private ExhibitManager exhibitManager;
    private Handler scanHandler = new Handler();
    private BeaconAdapter beaconListAdapter;
    private Snackbar snackbar;
    private Menu appBar;
    private View clickedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrollment);

        displayBTErrorSnackBar();
        exhibitManager = ExhibitManager.getInstance();

        bluetoothDevices = new ArrayList<>();
        beaconListAdapter = new BeaconAdapter();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        appBar = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                findViewById(R.id.scanningDevicesBar).setVisibility(View.VISIBLE);
                startScan();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)  {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK)
                    enableNetworkIfNeeded();
                else
                    displayUserWarning();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch(requestCode) {
            case REQUEST_ENABLE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    prepareForScan();
                } else
                    displayUserWarning();
                break;
        }
    }

    /**
     * Shows system dialog asking to enable bluetooth
     * @param view
     */
    public void enableBluetooth(View view) {
        displayWaitingUI();
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    /**
     * On Android 6.0+, shows dialog to ask for coarse location. This permission is required to
     * scan for BLE com.physical_web.cms.physicalwebcms.beacons on these versions of Android.
     */
    private void enableNetworkIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ENABLE_LOCATION);
            } else {
                prepareForScan();
            }
        } else {
            prepareForScan();
        }
    }

    // returns wether we've seen this device in the latest scan
    private boolean hasBeenAlreadyScanned(BluetoothDevice device) {
        for (BluetoothDevice storedDevice: bluetoothDevices) {
            if (device.getAddress().equalsIgnoreCase(storedDevice.getAddress()))
                return true;
        }
        return false;
    }

    private void prepareForScan() {
        prepareForScan(null);
    }

    /**
     * Setups up interface to display scan results, starts scan
     * @param v
     */
    public void prepareForScan(View v) {
        displayBeaconSearch();
        ((ListView) findViewById(R.id.scannedBeaconsList)).setAdapter(beaconListAdapter);
        ((ListView) findViewById(R.id.scannedBeaconsList)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice selectedDevice = bluetoothDevices.get(position);

                BottomSheetLayout bottomSheet = (BottomSheetLayout) findViewById(R.id.bottomsheet);
                bottomSheet.setBackgroundColor(Color.WHITE);
                bottomSheet.showWithSheetView(LayoutInflater.from(view.getContext()).inflate(R.layout.add_beacon, bottomSheet, false));
                ((TextView) findViewById(R.id.editBeaconNameText)).setText(selectedDevice.getName());
                ((TextView) findViewById(R.id.textBeaconAddress)).setText(selectedDevice.getAddress());
                clickedView = view;
            }

        });

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.enrollment, appBar);

        startScan();
    }

    // callback on each BLE advertisement received
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(!hasBeenAlreadyScanned(device)) {
                bluetoothDevices.add(device);
                findViewById(R.id.noDevicesYetText).setVisibility(View.INVISIBLE);
                findViewById(R.id.scannedBeaconsList).setVisibility(View.VISIBLE);
                beaconListAdapter.notifyDataSetChanged();
            }
        }
    };

    /**
     * Called by addBeacon method from a BottomSheet containing info about the beacon the user
     * wants to add. Creates a beacon object and adds it to database.
     * @param v
     */
    public void onAddBeacon(View v) {
        final String name = ((TextView) findViewById(R.id.editBeaconNameText)).getText().toString();
        final String address = ((TextView) findViewById(R.id.textBeaconAddress)).getText().toString();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Beacon newBeacon = new Beacon(address, name);
                BeaconDatabase db = BeaconDatabase.getDatabase(EnrollmentActivity.this);
                db.beaconDao().insertBeacons(newBeacon);
                db.close();
                exhibitManager.configureNewBeacon(newBeacon);
            }
        }).start();

        ((BottomSheetLayout) findViewById(R.id.bottomsheet)).dismissSheet();

        // display tick on added device in list
        clickedView.findViewById(R.id.tickView).setVisibility(View.VISIBLE);
        clickedView = null;
    }

    // start the BLE scan for SCAN_PERIOD milliseconds, results returned to specified callback
    private void startScan() {
        BluetoothAdapter.getDefaultAdapter().startLeScan(leScanCallback);
        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BluetoothAdapter.getDefaultAdapter().stopLeScan(leScanCallback);
                findViewById(R.id.scanningDevicesBar).setVisibility(View.INVISIBLE);
                if(bluetoothDevices.isEmpty()) {
                    noDevicesFound();
                }
            }
        }, SCAN_PERIOD);
    }

    // display UI elements in case no devices found after scan
    private void noDevicesFound() {
        findViewById(R.id.scanningDevicesBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.noDevicesYetText).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.noDevicesYetText)).setText("Nothing found");
    }

    // displays a snackbar that warns the user if the device doesn't have bluetooth
    // stops them from continuing
    private void displayBTErrorSnackBar() {
        if (!deviceHasBluetoothAdapter()) {
            snackbar = Snackbar
                    .make(findViewById(R.id.enrollmentLayout), "This device does not support Bluetooth",
                            Snackbar.LENGTH_INDEFINITE)
                    .setAction("RETRY", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            displayBTErrorSnackBar();
                        }
                    });
            snackbar.show();

            findViewById(R.id.enableButton).setEnabled(false);
        } else {
            if (snackbar != null)
                snackbar.dismiss();
            findViewById(R.id.enableButton).setEnabled(true);
        }
    }

    /**
     * Indicates whether the device is equiped with bluetooth.
     * @return
     */
    public Boolean deviceHasBluetoothAdapter() {
        return (BluetoothAdapter.getDefaultAdapter() != null);
    }

    // show the user that we are waiting on bluetooth enabling
    private void displayWaitingUI() {
        findViewById(R.id.userWarning).setVisibility(View.INVISIBLE);
        findViewById(R.id.bluetoothBar).setVisibility(View.VISIBLE);
        findViewById(R.id.enableButton).setEnabled(false);
    }

    // pull up UI for beacon search
    private void displayBeaconSearch() {
        // out the old
        findViewById(R.id.bluetoothIcon).setVisibility(View.INVISIBLE);
        findViewById(R.id.btDescription).setVisibility(View.INVISIBLE);
        findViewById(R.id.userWarning).setVisibility(View.INVISIBLE);
        findViewById(R.id.bluetoothBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.enableButton).setVisibility(View.INVISIBLE);

        // in with the new!
        findViewById(R.id.scanningDevicesBar).setVisibility(View.VISIBLE);
        findViewById(R.id.noDevicesYetText).setVisibility(View.VISIBLE);
        findViewById(R.id.scannedBeaconsTitle).setVisibility(View.VISIBLE);
        findViewById(R.id.scannedBeaconsList).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.noDevicesYetText)).setText("Seaching for com.physical_web.cms.physicalwebcms.beacons...");
    }

    // warn the user that they have made an error by not allowing BT or location access
    private void displayUserWarning() {
        findViewById(R.id.bluetoothBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.userWarning).setVisibility(View.VISIBLE);
        findViewById(R.id.enableButton).setEnabled(true);
    }

    // this class is a list adapter that allows BluetoothDevice info be to placed into a UI list
    private class BeaconAdapter extends BaseAdapter {
        private LayoutInflater inflater = (LayoutInflater) EnrollmentActivity.this
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        private List<BluetoothDevice> bluetoothDevices = EnrollmentActivity.this.bluetoothDevices;

        @Override
        public int getCount() {
            return bluetoothDevices.size();
        }

        @Override
        public Object getItem(int position) {
            return bluetoothDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BluetoothDevice beacon = (BluetoothDevice) getItem(position);
            ViewHolder holder;

            // standard holder optimizing code
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.simple_beacon_item, parent, false);

                holder = new ViewHolder();
                holder.nameTextView = (TextView) convertView.findViewById(R.id.beaconName);
                holder.addressTextView = (TextView) convertView.findViewById(R.id.beaconAddress);
                holder.tickView = (ImageView) convertView.findViewById(R.id.tickView);

                convertView.setTag(holder);
            } else {
                // if we've seen view before, don't need to inflate again
                holder = (ViewHolder) convertView.getTag();
            }

            if(beacon.getName() == null || beacon.getName().equals(""))
                holder.nameTextView.setText("Unnamed Beacon");
            else
               holder.nameTextView.setText(beacon.getName());

            holder.addressTextView.setText(beacon.getAddress());

            return convertView;
        }
    }

    private static class ViewHolder {
        public TextView nameTextView;
        public TextView addressTextView;
        public ImageView tickView;
    }
}
