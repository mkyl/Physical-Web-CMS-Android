package org.physical_web.cms.setup;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import org.physical_web.cms.R;

/**
 * This class introduces the user to the app and gets them set up
 */
public class WelcomeActivity extends AppCompatActivity {
    private static final String TAG = WelcomeActivity.class.getSimpleName();

    // arbitrary constant, used within class to track created Activities
    private final static int SETUP_DRIVE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button next = (Button) findViewById(R.id.nextButton);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDrive();
            }
        });
    }

    /**
     * Close the app if the user tries to go back from the welcome screen
     */
    @Override
    public void onBackPressed() {
        this.finishAffinity();
    }

    // start activity to setup Google Drive
    private void startDrive() {
        Intent driveSetupIntent = new Intent(this, DriveSetupActivity.class);
        // hooks up callback to onActivityResult
        this.startActivityForResult(driveSetupIntent, SETUP_DRIVE);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
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

