package org.physical_web.cms.exhibits;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.physical_web.cms.R;
import org.physical_web.cms.beacons.Beacon;


/**
 * Manages content for a single beacon within an exhibit.
 */
public class ExhibitContentFragment extends Fragment {
    public final static String TAG = ExhibitContentFragment.class.getSimpleName();
    private final static int FILE_PICKER_ROUTING_CODE = 1032;

    private Exhibit workingExhibit;
    private Beacon workingBeacon;
    private ExhibitContent[] exhibitContents;

    private ContentAdapter contentAdapter;

    public ExhibitContentFragment() {
        // required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle passedArguments = getArguments();
        if(passedArguments == null)
            throw new IllegalArgumentException("beacon name to work on must be provided");

        String exhibitName = passedArguments.getString("exhibit-name");
        workingExhibit = ExhibitManager.getInstance().getByName(exhibitName);

        String beaconName = passedArguments.getString("beacon-name");
        workingBeacon = new Beacon("", beaconName);

        exhibitContents = workingExhibit.getContentForBeacon(workingBeacon.friendlyName);

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

        RecyclerView contentList = (RecyclerView) result
                .findViewById(R.id.fragment_exhibit_content_list);

        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        contentList.setLayoutManager(linearLayoutManager);

        contentAdapter = new ContentAdapter();
        contentList.setAdapter(contentAdapter);

        return result;
    }

    private void addContent() {
        Intent filePickerIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        filePickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
        // must be set to */* to allow multiple MIME types
        filePickerIntent.setTypeAndNormalize("*/*");
        String[] acceptableMimeTypes = {"image/*", "video/*", "audio/*"};
        filePickerIntent.putExtra("EXTRA_MIME_TYPES", acceptableMimeTypes);

        startActivityForResult(filePickerIntent, FILE_PICKER_ROUTING_CODE);
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
        workingExhibit.insertContent(uri, workingBeacon.friendlyName, getActivity());
        refreshContentList();
    }

    private void refreshContentList() {
        exhibitContents = workingExhibit.getContentForBeacon(workingBeacon.friendlyName);
        contentAdapter.notifyDataSetChanged();
    }

    class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewtype) {
            View listItem = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.item_exhibit_editor_beacon, parent, false);

            return new ViewHolder(listItem);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            String contentName = ExhibitContentFragment
                    .this.exhibitContents[position].getContentName();
            viewHolder.beaconTitle.setText(contentName);
            /*
            viewHolder.background.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // to do
                }
            });
            */
        }

        @Override
        public int getItemCount() {
            return ExhibitContentFragment.this.exhibitContents.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView beaconTitle;
            View background;

            ViewHolder(View view) {
                super(view);
                beaconTitle = (TextView) view.findViewById(R.id.item_exhibit_editor_beacon_title);
                background = view;
            }
        }
    }
}
