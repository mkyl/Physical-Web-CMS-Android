package layout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.physical_web.cms.physicalwebcms.Beacon;
import com.physical_web.cms.physicalwebcms.BeaconDatabase;
import com.physical_web.cms.physicalwebcms.EnrollmentActivity;
import com.physical_web.cms.physicalwebcms.R;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BeaconFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BeaconFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BeaconFragment extends ContentFragment {
    private final static String TAG = BeaconFragment.class.getSimpleName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private final static int COLUMN_COUNT = 2;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private InstalledBeaconAdapter adapter;

    private BeaconDatabase db;

    public BeaconFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BeaconFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BeaconFragment newInstance(String param1, String param2) {
        BeaconFragment fragment = new BeaconFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                db = BeaconDatabase.getDatabase(getContext());
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

        recyclerView = (RecyclerView) result.findViewById(R.id.installedBeaconsViewFragment);
        recyclerView.setHasFixedSize(true);

        layoutManager = new GridLayoutManager(getContext(), COLUMN_COUNT);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new BeaconFragment.InstalledBeaconAdapter();
        recyclerView.setAdapter(adapter);

        return result;
    }

    private void startBeaconEnrollment() {
        Intent enrollmentIntent = new Intent(getActivity(), EnrollmentActivity.class);
        startActivity(enrollmentIntent);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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

            private int cardIndex(View clickedButton) {
                View parentCard = (View) clickedButton.getParent().getParent();
                return recyclerView.indexOfChild(parentCard);
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
    }
}
