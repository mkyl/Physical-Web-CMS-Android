package org.physical_web.cms.exhibits;


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

/**
 * Manages content for a single beacon within an exhibit. Activities which contain this fragment
 * **MUST** override {@link android.app.Activity#onActivityResult} to call
 * {@link ContentPickerListener#onContentReturned} when a result with request code
 * {@link ContentPickerListener#FILE_PICKER_ROUTING_CODE} is passed to the Activity.
 */
public class ExhibitContentFragment extends Fragment implements ContentPickerListener {
    public final static String TAG = ExhibitContentFragment.class.getSimpleName();

    public ExhibitContentFragment() {
        // Required empty public constructor
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

        startActivityForResult(filePickerIntent, ContentPickerListener.FILE_PICKER_ROUTING_CODE);
    }

    public void onContentReturned(Uri uri) {
        Log.d(TAG, "Got a uri: " + uri);
    }
}

