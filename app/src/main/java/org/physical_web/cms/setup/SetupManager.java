package org.physical_web.cms.setup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

/**
 * Created by Kayali on 2017-07-12.
 */
public class SetupManager {
    private SharedPreferences sharedPreferences;
    private Context context;

    public SetupManager(Context activityContext) {
        this.context = activityContext;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void checkRequirements() {
        if (isFirstRun()) {
            Intent welcomeIntent = new Intent(this.context, WelcomeActivity.class);
            context.startActivity(welcomeIntent);
        } else {
            // TODO: check if connection lost while user is in app, warn them
        }
    }

    // returns whether the app has been run before. will return false if the WelcomeActivity
    // runs once successfully
    private Boolean isFirstRun() {
        // if variable doesn't exist in sharedPreferences, returns true (vacuous truth)
        return sharedPreferences.getBoolean("drive-setup-completed", true);
    }

    // check if we are connected to a network with internet access.
    public static Boolean networkIsConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
