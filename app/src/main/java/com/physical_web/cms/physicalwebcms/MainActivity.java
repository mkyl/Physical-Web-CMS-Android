package com.physical_web.cms.physicalwebcms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setupDrive();
    }

    private void setupDrive() {
        Boolean driveSetupComplete = sharedPreferences.getBoolean("drive-setup-complete", true);
        if (!driveSetupComplete) {
            Intent driveSetupIntent = new Intent(this, DriveSetupActivity.class);
            startActivity(driveSetupIntent);
            sharedPreferences.edit().putBoolean("drive-setup-complete", true).apply();
        }
    }
}
