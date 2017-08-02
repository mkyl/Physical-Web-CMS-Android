package org.physical_web.cms.maps;


import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.physical_web.cms.R;

/**
 * This fragment allows users to upload a map of the physical space and mark the location of the
 * beacons within it.
 */
public class PhysicalMapFragment extends Fragment {
    private static final String FRAGMENT_TITLE = "Beacon Map";
    private static final String TAG = PhysicalMapFragment.class.getSimpleName();
    // Fun fact: Jupiter has 53 moons
    private static final int FLOORPLAN_PICKER_ROUTING_CODE = 53;

    private PhysicalMap physicalMap;
    private PhysicalMapCanvas canvas;

    public PhysicalMapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_map, container, false);

        canvas = (PhysicalMapCanvas) fragmentView.findViewById(R.id.fragment_map_canvas);
        fragmentView.findViewById(R.id.fragment_map_warn_button)
                .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFloorPlan(v);
            }
        });

        physicalMap = PhysicalMap.loadFromDisk(getActivity());
        displayMap(fragmentView);

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(FRAGMENT_TITLE);
    }

    private void displayMap() {
        displayMap(getView());
    }

    private void displayMap(View fragmentView) {
        Boolean physicalMapNotLoaded = physicalMap == null;
        showNoMapWarning(physicalMapNotLoaded, fragmentView);
        if (!physicalMapNotLoaded)
            canvas.loadMap(physicalMap);
    }

    private void showNoMapWarning(Boolean showWarning, View view) {
        View warning = view.findViewById(R.id.fragment_map_warn);
        if (showWarning) {
            warning.setVisibility(View.VISIBLE);
        } else {
            warning.setVisibility(View.INVISIBLE);
        }
    }

    public void uploadFloorPlan(View view) {
        Intent filePickerIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        filePickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
        // must be set to */* to allow multiple MIME types
        filePickerIntent.setTypeAndNormalize("image/*");

        startActivityForResult(filePickerIntent, FLOORPLAN_PICKER_ROUTING_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case FLOORPLAN_PICKER_ROUTING_CODE:
                    handleURI(resultData.getData());
                    break;
                default:
                    throw new UnsupportedOperationException("Unrecognized routing code received");
            }
        } else {
            Log.e(TAG, "Activity request with request code " + requestCode
                    + " failed with error code " + resultCode);
        }
    }

    private void handleURI(Uri uri) {
        physicalMap = PhysicalMap.newMap(uri, getActivity());
        displayMap();
        Toast.makeText(getActivity(),
                "To place a beacon, touch a location on map",
                Toast.LENGTH_LONG).show();
    }
}
