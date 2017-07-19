package org.physical_web.cms.beacons;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.flipboard.bottomsheet.BottomSheetLayout;

import org.physical_web.cms.R;

import java.util.List;

/**
 * Lists beacons that have been enrolled through enrollment activity. Allows modifying the
 * details on enrolled beacons, as well as their removal.
 */
public class BeaconFragment extends Fragment {
    private final static String TAG = BeaconFragment.class.getSimpleName();
    private final static String FRAGMENT_TITLE = "Beacons";
    private final static int COLUMN_COUNT = 2;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private InstalledBeaconAdapter adapter;
    private View sheetView;

    private BeaconDatabase db;
    private Beacon selectedBeacon;

    public BeaconFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                db = BeaconDatabase.getDatabase(getActivity());
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.refresh();
                        getActivity().findViewById(R.id.progressBar2).
                                setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
        thread.start();

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(FRAGMENT_TITLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        db.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View result = inflater.inflate(R.layout.fragment_beacon, container, false);

        FloatingActionButton fab = (FloatingActionButton) result.findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBeaconEnrollment();
            }
        });

        BottomSheetLayout bottomSheet = (BottomSheetLayout) result.
                findViewById(R.id.beacon_bottomsheet);
        sheetView = inflater.inflate(R.layout.sheet_beacon_edit, bottomSheet, false);
        sheetView.findViewById(R.id.edit_beacon_close).setOnClickListener(updateBeacon);

        recyclerView = (RecyclerView) result.findViewById(R.id.installedBeaconsViewFragment);
        // TODO pretty sure this is bug, prevents scrolling
        recyclerView.setHasFixedSize(true);

        layoutManager = new GridLayoutManager(getActivity(), COLUMN_COUNT);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new BeaconFragment.InstalledBeaconAdapter();
        recyclerView.setAdapter(adapter);

        return result;
    }

    private View.OnClickListener updateBeacon = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sheetView.findViewById(R.id.edit_beacon_progress).setVisibility(View.VISIBLE);
            selectedBeacon.friendlyName = ((EditText) sheetView.
                    findViewById(R.id.edit_beacon_name)).getText().toString();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    db.beaconDao().updateBeacons(selectedBeacon);
                    selectedBeacon = null;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sheetView.findViewById(R.id.edit_beacon_progress)
                                    .setVisibility(View.INVISIBLE);
                            BottomSheetLayout bottomSheet = (BottomSheetLayout) getActivity().
                                    findViewById(R.id.beacon_bottomsheet);
                            bottomSheet.dismissSheet();
                            adapter.refresh();
                        }
                    });
                }
            }).start();
        }
    };

    private void startBeaconEnrollment() {
        Intent enrollmentIntent = new Intent(getActivity(), EnrollmentActivity.class);
        startActivity(enrollmentIntent);
    }

    class InstalledBeaconAdapter extends RecyclerView.Adapter<InstalledBeaconAdapter.ViewHolder> {
        private List<Beacon> beacons;

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

        public void refresh() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    beacons = db.beaconDao().getAllBeacons();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            InstalledBeaconAdapter.this.notifyDataSetChanged();
                        }
                    });
                }
            }).start();
        }

        @Override
        public InstalledBeaconAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View cardView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.beacon_card, parent, false);

            cardView.findViewById(R.id.delete_button).setOnClickListener(removeBeacons);
            cardView.findViewById(R.id.edit_button).setOnClickListener(editBeacon);

            ViewHolder vh = new ViewHolder(cardView);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // TODO set images
            holder.title.setText(beacons.get(position).friendlyName);
        }

        @Override
        public int getItemCount() {
            if (beacons == null)
                return 0;
            else
                return beacons.size();
        }

        private View.OnClickListener removeBeacons = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int beaconIndex = cardIndex(v);
                final Beacon beaconToDelete = beacons.get(beaconIndex);

                Log.d(TAG, "Deleting beacon with name: " + beaconToDelete.friendlyName);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        db.beaconDao().deleteBeacons(beaconToDelete);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                InstalledBeaconAdapter.this.refresh();
                                showUndoSnackbar(beaconToDelete);
                            }
                        });
                    }
                }).start();
            }

            private void showUndoSnackbar(final Beacon deletedBeacon) {
                Snackbar undoSnackbar = Snackbar.
                        make(getView(), "Deleted beacon '" + deletedBeacon.friendlyName + "'",
                                Snackbar.LENGTH_LONG).
                        setAction("UNDO", new View.OnClickListener(){
                            @Override
                            public void onClick(View view) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        db.beaconDao().insertBeacons(deletedBeacon);
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                InstalledBeaconAdapter.this.refresh();
                                            }
                                        });
                                    }
                                }).start();
                            }
                        });
                undoSnackbar.show();
            }
        };

        private View.OnClickListener editBeacon = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int beaconIndex = cardIndex(v);
                selectedBeacon = beacons.get(beaconIndex);

                BottomSheetLayout bottomSheet = (BottomSheetLayout) getActivity().
                        findViewById(R.id.beacon_bottomsheet);
                ((EditText) sheetView.findViewById(R.id.edit_beacon_name)).
                        setText(selectedBeacon.friendlyName);
                ((EditText) sheetView.findViewById(R.id.edit_beacon_mac)).
                        setText(selectedBeacon.address);
                bottomSheet.showWithSheetView(sheetView);
            }
        };

        private int cardIndex(View clickedButton) {
            View parentCard = (View) clickedButton.getParent().getParent();
            return recyclerView.indexOfChild(parentCard);
        }
    }
}
