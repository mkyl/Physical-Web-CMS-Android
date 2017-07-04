package layout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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
    private RecyclerView.Adapter adapter;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(adapter != null) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    adapter = new BeaconFragment.InstalledBeaconAdapter();

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView.setAdapter(adapter);
                        }
                    });
                }
            });
            thread.start();
        }
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

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                adapter = new BeaconFragment.InstalledBeaconAdapter();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.setAdapter(adapter);
                        result.findViewById(R.id.progressBar2).setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
        thread.start();

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

        public InstalledBeaconAdapter() {
            BeaconDatabase db = BeaconDatabase.getDatabase(getActivity());
            beacons = db.beaconDao().getAllBeacons();
            db.close();
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
            holder.title.setText(beacons.get(position).friendlyName);
        }

        @Override
        public int getItemCount() {
            return beacons.size();
        }
    }
}
