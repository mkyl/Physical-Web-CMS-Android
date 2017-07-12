package org.physical_web.cms;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.physical_web.cms.sync.ContentSynchronizer;
import org.physical_web.cms.sync.SyncStatusListener;


/**
 * Home page fragment: tells user about the currently deployed exhibition, as well as the
 * status of Google Drive synchronization.
 */
public class WelcomeFragment extends Fragment implements SyncStatusListener {
    public WelcomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WelcomeFragment.
     */
    public static WelcomeFragment newInstance(String param1, String param2) {
        WelcomeFragment fragment = new WelcomeFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void syncStatusChanged(int status) {
        if(getActivity().findViewById(R.id.welcome_sync_text) == null)
            return;

        int widgetColorName;
        String widgetText;
        int progressBarVisibility;

        switch (status) {
            case ContentSynchronizer.SYNC_IN_PROGRESS:
                widgetText = "Sync in progress";
                widgetColorName = android.R.color.holo_orange_light;
                progressBarVisibility = View.VISIBLE;
                break;
            case ContentSynchronizer.SYNC_COMPLETE:
                widgetText = "Sync complete";
                widgetColorName = android.R.color.holo_green_dark;
                progressBarVisibility = View.INVISIBLE;
                break;
            case ContentSynchronizer.NO_SYNC_NETWORK_DOWN:
                widgetText = "Connect to internet to sync";
                widgetColorName = android.R.color.darker_gray;
                progressBarVisibility = View.INVISIBLE;
                break;
            case ContentSynchronizer.NO_SYNC_DRIVE_ERROR:
                widgetText = "Sync failed";
                widgetColorName = android.R.color.holo_red_dark;
                progressBarVisibility = View.INVISIBLE;
                break;
            default:
                throw new IllegalArgumentException("Unknown sync status received");
        }

        ((TextView) getActivity().findViewById(R.id.welcome_sync_text))
                .setText(widgetText);

        int widgetColor = getResources().getColor(widgetColorName);
        ((CardView) getActivity().findViewById(R.id.welcome_sync_card))
                .setCardBackgroundColor(widgetColor);

        getActivity().findViewById(R.id.welcome_sync_progress)
                .setVisibility(progressBarVisibility);
    }
}
