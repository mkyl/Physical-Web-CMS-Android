package org.physical_web.cms.setup;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import org.physical_web.cms.R;

/**
 * This class takes care of authourizing Google Drive for storage of the media that the user
 * will upload, as well as storage of information about the Physical Web com.physical_web.cms.physicalwebcms.beacons.
 */
public class DriveSetupActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = DriveSetupActivity.class.getSimpleName();

    final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    final int GOOGLE_PLAY_SERVICES_ERROR = 2;

    private GoogleApiClient apiClient;
    private Snackbar snackbar;
    private boolean busyAuthorizing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_setup);

        // Need a Google Drive client with access to the APPFOLDER, a hidden folder in Drive
        // only visible to us programatically
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        final Button setupButton = (Button) findViewById(R.id.start_setup_button);
        setupButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startSetup();
            }
        });

        // Services API is installed or not installed, only need to check on activity startup
        setupGoogleAPISnackbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // connectivity can change often, check whenever focus is lost
        setupConnectivitySnackbar();
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Drive connection successful");

        unlockInterface();

        Button button = (Button) findViewById(R.id.start_setup_button);
        button.setText("Next");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
        button.setEnabled(true);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // TODO check which cases this exception is encountered
                throw new RuntimeException("There is no resolution to the Drive API error");
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
            unlockInterfaceWithError();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    apiClient.connect();
                } else {
                    unlockInterfaceWithUserWarning();
                }
                break;
        }
    }

    private void startSetup() {
        lockInterface();
        apiClient.connect();
    }

    // disable button and show spinner to indicate background work
    private void lockInterface() {
        busyAuthorizing = true;
        findViewById(R.id.textViewWarning).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewOtherWarning).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewSuccess).setVisibility(View.INVISIBLE);
        findViewById(R.id.indeterminateBar).setVisibility(View.VISIBLE);
        findViewById(R.id.start_setup_button).setEnabled(false);
    }

    // enable interface interaction after a successful authorization
    private void unlockInterface() {
        findViewById(R.id.indeterminateBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewWarning).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewOtherWarning).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewSuccess).setVisibility(View.VISIBLE);
    }

    // enable button but warn user that authorization failed (user mistake)
    private void unlockInterfaceWithUserWarning() {
        findViewById(R.id.indeterminateBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewOtherWarning).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewSuccess).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewWarning).setVisibility(View.VISIBLE);
        findViewById(R.id.start_setup_button).setEnabled(true);
        busyAuthorizing = false;
    }

    private void unlockInterfaceWithError() {
        findViewById(R.id.indeterminateBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewWarning).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewSuccess).setVisibility(View.INVISIBLE);
        findViewById(R.id.textViewOtherWarning).setVisibility(View.VISIBLE);
        findViewById(R.id.start_setup_button).setEnabled(true);
        busyAuthorizing = false;
    }

    // this snackbar indicates to the user that there is no internet connection, disables next
    // button until problem is resolved
    private void setupConnectivitySnackbar() {
        if (!SetupManager.networkIsConnected(this)) {
            snackbar = Snackbar
                    .make(findViewById(R.id.googleDriveLogo), "No Internet Connectivity",
                            Snackbar.LENGTH_INDEFINITE)
                    .setAction("RETRY", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            setupConnectivitySnackbar();
                        }
                    });
            snackbar.show();

            findViewById(R.id.start_setup_button).setEnabled(false);
        } else {
            if (snackbar != null)
                snackbar.dismiss();
            if (!busyAuthorizing)
                findViewById(R.id.start_setup_button).setEnabled(true);
        }
    }

    // this snackbar warns user if Google Play isn't installed on device (some custom ROMS, etc.)
    // and disables further setup
    private void setupGoogleAPISnackbar() {
        if (snackbar == null) {
            if (!isGooglePlayServicesAvailable(this)) {
                snackbar = Snackbar
                        .make(findViewById(R.id.googleDriveLogo),
                                "Google Play services not installed",
                                Snackbar.LENGTH_INDEFINITE);
                snackbar.show();

                findViewById(R.id.start_setup_button).setEnabled(false);
            }
        }
    }

    // Google Play Services is mandatory for Drive support, and so is mandatory for the app.
    public boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if(status != ConnectionResult.SUCCESS) {
            if(googleApiAvailability.isUserResolvableError(status)) {
                // not much that we can do if Play Services not installed
                googleApiAvailability.getErrorDialog(activity, status,
                        GOOGLE_PLAY_SERVICES_ERROR).show();
            }
            return false;
        }
        return true;
    }
}
