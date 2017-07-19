package org.physical_web.cms;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.physical_web.cms.sync.ContentSynchronizer;
import org.physical_web.cms.sync.SyncStatusListener;


/**
 * Home page fragment: tells user about the currently deployed exhibition, as well as the
 * status of Google Drive synchronization.
 */
public class WelcomeFragment extends Fragment implements SyncStatusListener {
    ContentSynchronizer contentSynchronizer;

    public WelcomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View welcomeFragmentView = inflater.inflate(R.layout.fragment_welcome, container, false);
        // register for changes in sync status
        contentSynchronizer = ContentSynchronizer.getInstance();
        contentSynchronizer.registerSyncStatusListener(this);

        return welcomeFragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        contentSynchronizer.kickStartSync();
    }

    @Override
    public void syncStatusChanged(int status) {
        if(getActivity() == null ||
                getActivity().findViewById(R.id.welcome_sync_text) == null)
            return;

        final int widgetColorName;
        final String widgetText;
        final int progressBarVisibility;
        final int statusIcon;
        final int statusIconVisibility;

        switch (status) {
            case ContentSynchronizer.SYNC_IN_PROGRESS:
                widgetText = "Sync in progress";
                widgetColorName = android.R.color.holo_orange_light;
                progressBarVisibility = View.VISIBLE;
                statusIcon = -1;
                statusIconVisibility = View.INVISIBLE;
                break;
            case ContentSynchronizer.SYNC_COMPLETE:
                widgetText = "Sync complete";
                widgetColorName = android.R.color.holo_green_dark;
                progressBarVisibility = View.INVISIBLE;
                statusIcon = R.mipmap.cloud_done;
                statusIconVisibility = View.VISIBLE;
                break;
            case ContentSynchronizer.NO_SYNC_NETWORK_DOWN:
                widgetText = "No network connection";
                widgetColorName = android.R.color.darker_gray;
                progressBarVisibility = View.INVISIBLE;
                statusIcon = R.mipmap.cloud_offline;
                statusIconVisibility = View.VISIBLE;
                break;
            case ContentSynchronizer.NO_SYNC_DRIVE_ERROR:
                widgetText = "Sync failed";
                widgetColorName = android.R.color.holo_red_dark;
                progressBarVisibility = View.INVISIBLE;
                statusIcon = R.mipmap.cloud_error;
                statusIconVisibility = View.VISIBLE;
                break;
            default:
                throw new IllegalArgumentException("Unknown sync status received");
        }

        // changes to UI must be done on UI thread
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) getActivity().findViewById(R.id.welcome_sync_text))
                        .setText(widgetText);

                int widgetColor = getResources().getColor(widgetColorName);
                ((CardView) getActivity().findViewById(R.id.welcome_sync_card))
                        .setCardBackgroundColor(widgetColor);

                getActivity().findViewById(R.id.welcome_sync_progress)
                        .setVisibility(progressBarVisibility);

                getActivity().findViewById(R.id.welcome_sync_status)
                        .setVisibility(statusIconVisibility);

                if (statusIcon != -1) {
                    ((ImageView) getActivity().findViewById(R.id.welcome_sync_status))
                            .setImageResource(statusIcon);
                    ((ImageView) getActivity().findViewById(R.id.welcome_sync_status))
                            .setBackgroundColor(widgetColor);
                }
            }
        });
    }
}
