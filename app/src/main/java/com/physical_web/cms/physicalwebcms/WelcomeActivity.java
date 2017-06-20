package com.physical_web.cms.physicalwebcms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

/**
 * This class introduces the user to the app and gets them set up
 */
public class WelcomeActivity extends AppCompatActivity {
    // arbitrary constant, used within class to track created Activities
    private final static int SETUP_DRIVE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button next = (Button) findViewById(R.id.nextButton);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDrive();
            }
        });
    }

    // start activity to setup Google Drive
    private void startDrive() {
        Intent driveSetupIntent = new Intent(this, DriveSetupActivity.class);
        // hooks up callback to onActivityResult
        this.startActivityForResult(driveSetupIntent, SETUP_DRIVE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        switch(requestCode) {
            case (SETUP_DRIVE):
                if (resultCode == RESULT_OK) {
                    // this will cause SetupManager.isFirstRun() to return false hereafter
                    SharedPreferences sharedPreferences = PreferenceManager
                            .getDefaultSharedPreferences(this);
                    sharedPreferences.edit().putBoolean("drive-setup-completed", false).commit();
                    setResult(Activity.RESULT_OK);
                    finish();
                }
                break;
        }
    }
}
