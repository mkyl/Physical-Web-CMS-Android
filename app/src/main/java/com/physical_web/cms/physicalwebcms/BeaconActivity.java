package com.physical_web.cms.physicalwebcms;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Iterator;

public class BeaconActivity extends AppCompatActivity {
    private final static int COLUMN_COUNT = 2;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView.Adapter adapter;

    private DatabaseManager databaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        databaseManager = new DatabaseManager(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBeaconEnrollment();
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.installedBeaconsView);
        recyclerView.setHasFixedSize(true);

        layoutManager = new GridLayoutManager(this, COLUMN_COUNT);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new InstalledBeaconAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        databaseManager.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        databaseManager.close();
    }

    // start activity to add beacons to app
    private void startBeaconEnrollment() {
        Intent enrollmentIntent = new Intent(BeaconActivity.this, EnrollmentActivity.class);
        startActivity(enrollmentIntent);
    }

    class InstalledBeaconAdapter extends RecyclerView.Adapter<InstalledBeaconAdapter.ViewHolder> {
        Iterator<Beacon> beaconIterator = BeaconActivity.this.databaseManager.getAllBeacons().iterator();

        class ViewHolder extends RecyclerView.ViewHolder {
            public TextView title;
            public ImageView backgroundImage;
            public Button editButton;
            public Button deleteButton;

            public ViewHolder(View card) {
                super(card);
                this.title = (TextView) card.findViewById(R.id.info_text);
            }
        }

        @Override
        public InstalledBeaconAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View cardView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.beacon_card, parent, false);

            ViewHolder vh = new ViewHolder(cardView);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // TODO set images
            // TODO BROKEN CODE!
            holder.title.setText(beaconIterator.next().getFriendlyName());
        }

        @Override
        public int getItemCount() {
            return BeaconActivity.this.databaseManager.getAllBeacons().size();
        }
    }
}