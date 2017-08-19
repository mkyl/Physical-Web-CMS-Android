package org.physical_web.cms.setup;


import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.physical_web.cms.R;
import org.physical_web.cms.bluetooth.BeaconEventListener;
import org.physical_web.cms.bluetooth.BluetoothManager;

/**
 * Allows the user to configure the settings of the application
 */
public class SettingsFragment extends PreferenceFragment implements BeaconEventListener{
    private static final String FRAGMENT_TITLE = "Settings";
    private static final String TAG = SettingsFragment.class.getSimpleName();

    private BluetoothManager manager;
    private int configuredBeacons = 0;
    private String uri = null;
    EditTextPreference serverUriPreference;

    public SettingsFragment() {
        // Required empty public constructor
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        manager = new BluetoothManager(getActivity());
        serverUriPreference = (EditTextPreference) findPreference("server_uri_preference");
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(FRAGMENT_TITLE);
    }

    private void updateHint() {
        final String hint;
        if (configuredBeacons == 0)
            hint = "Scanning for beacons...";
        else if (configuredBeacons == 1)
            hint = "One beacon successfully configured";
        else
            hint = "" + configuredBeacons + " beacons successfully configured";

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView)getView().findViewById(R.id.preferences_status_text)).setText(hint);
                getView().findViewById(R.id.preferences_status_text).setVisibility(View.VISIBLE);
            }
        });
    }

    public void scanAndSetup(View v) {
        if (setupUriAndValidate()) {
            updateHint();
            getView().findViewById(R.id.preferences_scan_button).setEnabled(false);
            getView().findViewById(R.id.preferences_scan_progress).setVisibility(View.VISIBLE);
            manager.listConfigurableEddystoneBeacons(SettingsFragment.this);
        }
    }

    private Boolean setupUriAndValidate() {
        String inputText = serverUriPreference.getText();
        Boolean bluetoothOn = manager.bluetoothIsEnabled();
        if (!bluetoothOn) {
            getView().findViewById(R.id.preferences_warning_bt).setVisibility(View.VISIBLE);
            getView().findViewById(R.id.preferences_warning_text).setVisibility(View.GONE);
            return false;
        } else if (!inputText.startsWith("https://")) {
            getView().findViewById(R.id.preferences_warning_text).setVisibility(View.VISIBLE);
            getView().findViewById(R.id.preferences_warning_bt).setVisibility(View.GONE);
            return false;
        } else {
            // all good, continue
            uri = inputText;
            getView().findViewById(R.id.preferences_warning_text).setVisibility(View.GONE);
            getView().findViewById(R.id.preferences_warning_bt).setVisibility(View.GONE);
            return true;
        }
    }

    @Override
    public void onConfigurableBeaconFound(BluetoothDevice device) {
        if (uri == null)
            throw new IllegalStateException();

        Log.d(TAG, "Trying to update beacon to URI: " + uri);
        manager.updateBeaconUri(device, uri, this);
    }

    @Override
    public void uriWriteCallback(BluetoothDevice device, int status) {
        if (status == BeaconEventListener.WRITE_SUCCESS)
            configuredBeacons++;
        else
            Log.e(TAG, "Beacon URI configuration failed with error code: " + status);

        updateHint();
    }

    @Override
    public void onScanComplete() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (configuredBeacons == 0) {
                    ((TextView)getView().findViewById(R.id.preferences_status_text))
                            .setText("No configurable beacons found");
                    getView().findViewById(R.id.preferences_status_text)
                            .setVisibility(View.VISIBLE);
                }
                getView().findViewById(R.id.preferences_scan_button).setEnabled(true);
                getView().findViewById(R.id.preferences_scan_progress).setVisibility(View.GONE);
            }
        });
    }
}
