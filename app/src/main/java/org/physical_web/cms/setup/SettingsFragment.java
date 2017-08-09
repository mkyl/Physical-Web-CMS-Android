package org.physical_web.cms.setup;


import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
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
        String hint;
        if (configuredBeacons == 0)
            hint = "Scanning for beacons...";
        else if (configuredBeacons == 1)
            hint = "One beacon successfully configured";
        else
            hint = "" + configuredBeacons + " beacons successfully configured";

        ((TextView)getView().findViewById(R.id.preferences_status_text)).setText(hint);
        getView().findViewById(R.id.preferences_status_text).setVisibility(View.VISIBLE);
    }

    private View.OnClickListener scanAndSetup = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getView().findViewById(R.id.preferences_scan_button).setEnabled(false);
            getView().findViewById(R.id.preferences_scan_progress).setVisibility(View.VISIBLE);
            manager.listConfigurableEddystoneBeacons(SettingsFragment.this);
        }
    };

    @Override
    public void onConfigurableBeaconFound(BluetoothDevice device) {
        if (uri == null)
            throw new IllegalStateException();

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
        if (configuredBeacons == 0) {
            ((TextView)getView().findViewById(R.id.preferences_status_text)).setText("No beacons found");
        }
        getView().findViewById(R.id.preferences_scan_button).setEnabled(true);
        getView().findViewById(R.id.preferences_scan_progress).setVisibility(View.GONE);
    }
}
