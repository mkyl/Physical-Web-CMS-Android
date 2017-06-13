package com.physical_web.cms.physicalwebcms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

/**
 * Created by Kayali on 2017-06-11.
 */

public class SetupManager {
    SharedPreferences sharedPreferences;

    public SetupManager(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void checkRequirements(Context context) {
        if(isFirstRun()) {
            Intent welcomeIntent = new Intent(context, WelcomeActivity.class);
            context.startActivity(welcomeIntent);
        } else {
            // TODO
        }
    }

    private Boolean isFirstRun() {
        // if variable doesn't exist in sharedPreferences, returns true (vacuous truth)
        return sharedPreferences.getBoolean("drive-setup-completed", true);
    }

    public void checkDrive(Context context) {
        Boolean driveSetupComplete = sharedPreferences.getBoolean("drive-setup-complete", true);
        if (!driveSetupComplete) {
            Intent driveSetupIntent = new Intent(context, DriveSetupActivity.class);
            context.startActivity(driveSetupIntent);
            sharedPreferences.edit().putBoolean("drive-setup-complete", true).apply();
        }
    }

    public static Boolean networkIsConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }
}
