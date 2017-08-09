package org.physical_web.cms.setup;


import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.physical_web.cms.R;
import org.physical_web.cms.bluetooth.BluetoothManager;

/**
 * Allows the user to configure the settings of the application
 */
public class SettingsFragment extends PreferenceFragment {
    private static final String FRAGMENT_TITLE = "Settings";

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        BluetoothManager manager = new BluetoothManager(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(FRAGMENT_TITLE);
    }
}
