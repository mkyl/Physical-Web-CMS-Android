package org.physical_web.cms.exhibits;


import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.physical_web.cms.R;
import org.physical_web.cms.beacons.Beacon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * Manages content for a single beacon within an exhibit.
 */
public class ExhibitContentFragment extends Fragment {
    public final static String TAG = ExhibitContentFragment.class.getSimpleName();
    private final static int BUFFER_SIZE = 8 * 1024;
    private final static int FILE_PICKER_ROUTING_CODE = 1032;

    private Beacon workingBeacon;
    private File contentFolder;

    public ExhibitContentFragment() {
        Bundle passedArguments = getArguments();
        if(passedArguments == null)
            throw new IllegalArgumentException("beacon name to work on must be provided");

        String beaconName = passedArguments.getString("beacon-name");
        if(beaconName == null || beaconName.isEmpty())
            throw new IllegalArgumentException("beacon name to work on must be provided");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View result = inflater.inflate(R.layout.fragment_exhibit_content, container, false);

        FloatingActionButton addContentButton =
                (FloatingActionButton) result.findViewById(R.id.fragment_exhibit_content_add);
        addContentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addContent();
            }
        });

        return result;
    }

    private void addContent() {
        Intent filePickerIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        filePickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
        // must be set to */* to allow multiple MIME types
        filePickerIntent.setTypeAndNormalize("*/*");
        String[] acceptableMimeTypes = {"image/*", "video/*", "audio/*"};
        filePickerIntent.putExtra("EXTRA_MIME_TYPES", acceptableMimeTypes);

        getActivity().startActivityForResult(filePickerIntent, FILE_PICKER_ROUTING_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case FILE_PICKER_ROUTING_CODE:
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
        Log.d(TAG, "received URI: " + uri);
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
            // TODO find reliable way to get filename from URI
            String localCopyName = UUID.randomUUID().toString().replaceAll("-", "");
            File localCopy = new File(contentFolder, localCopyName);
            FileOutputStream outputStream = new FileOutputStream(localCopy);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
        } catch (Exception e) {
            Log.d(TAG, "Error copying file with URI " + uri + ": " + e);
        }
    }
}

