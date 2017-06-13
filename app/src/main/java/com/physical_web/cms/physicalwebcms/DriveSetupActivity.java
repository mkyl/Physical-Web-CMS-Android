package com.physical_web.cms.physicalwebcms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

public class DriveSetupActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    private GoogleApiClient apiClient;
    private Snackbar snackbar;
    private boolean busyAuthourizing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_setup);

        apiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        final Button setupButton = (Button) findViewById(R.id.start_setup_button);
        setupButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // start setup
                apiClient.connect();
                findViewById(R.id.textViewWarning).setVisibility(View.INVISIBLE);
                findViewById(R.id.indeterminateBar).setVisibility(View.VISIBLE);
                findViewById(R.id.start_setup_button).setEnabled(false);
                busyAuthourizing = true;
            }
        });

        setupGoogleAPISnackbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupConnectivitySnackbar();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(MainActivity.TAG, "Drive connection successful");

        findViewById(R.id.indeterminateBar).setVisibility(View.INVISIBLE);
        this.findViewById(R.id.textViewWarning).setVisibility(View.INVISIBLE);
        this.findViewById(R.id.textViewSuccess).setVisibility(View.VISIBLE);

        Button button = (Button) this.findViewById(R.id.start_setup_button);
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
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // TODO Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
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
                    findViewById(R.id.indeterminateBar).setVisibility(View.INVISIBLE);
                    this.findViewById(R.id.textViewWarning).setVisibility(View.VISIBLE);
                    this.findViewById(R.id.start_setup_button).setEnabled(true);
                    busyAuthourizing = false;
                }
                break;
        }
    }

    private void setupConnectivitySnackbar() {
        if (!SetupManager.networkIsConnected(this)) {
            snackbar = Snackbar
                    .make(this.findViewById(R.id.imageView3), "No Internet Connectivity", Snackbar.LENGTH_INDEFINITE)
                    .setAction("RETRY", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            setupConnectivitySnackbar();
                        }
                    });
            snackbar.show();

            this.findViewById(R.id.start_setup_button).setEnabled(false);
        } else {
            if (snackbar != null)
                snackbar.dismiss();
            if (!busyAuthourizing)
                this.findViewById(R.id.start_setup_button).setEnabled(true);
        }
    }

    private void setupGoogleAPISnackbar() {
        if (snackbar == null) {
            if (!isGooglePlayServicesAvailable(this)) {
                snackbar = Snackbar
                        .make(this.findViewById(R.id.imageView3),
                                "Google Play services not installed",
                                Snackbar.LENGTH_INDEFINITE);
                snackbar.show();

                this.findViewById(R.id.start_setup_button).setEnabled(false);
            }
        }
    }

    public boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if(status != ConnectionResult.SUCCESS) {
            if(googleApiAvailability.isUserResolvableError(status)) {
                // 2404 is arbitrary code
                googleApiAvailability.getErrorDialog(activity, status, 2404).show();
            }
            return false;
        }
        return true;
    }
}
