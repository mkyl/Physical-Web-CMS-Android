package org.physical_web.cms.exhibits;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.physical_web.cms.R;
import org.physical_web.cms.beacons.BeaconManager;

/**
 * Handles editing the exhibit metadata as well as listing the beacons involved in
 * exhibit
 */
public class ExhibitEditorFragment extends Fragment {
    private final static String TAG = ExhibitEditorFragment.class.getSimpleName();
    private final static String FRAGMENT_TITLE = "Edit Exhibit";

    private ExhibitManager exhibitManager = ExhibitManager.getInstance();
    private BeaconManager beaconManager = BeaconManager.getInstance();
    private Exhibit workingExhibit;

    private BeaconAdapter beaconAdapter;

    public ExhibitEditorFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View editorView = inflater.inflate(R.layout.fragment_exhibit_editor, container, false);

        // get arguments passed in
        Bundle bundle = getArguments();
        if (bundle == null)
            throw new IllegalArgumentException("No exhibit provided to edit");

        // get exhibit we are editing, by ID
        Long exhibitId = bundle.getLong("exhibit-id");
        workingExhibit = exhibitManager.getById(exhibitId);

        // setup the metadata section of the editor
        ((EditText) editorView.findViewById(R.id.exhibit_editor_title))
                .setText(workingExhibit.getTitle());
        ((EditText) editorView.findViewById(R.id.exhibit_editor_description))
                .setText(workingExhibit.getDescription());

        editorView.findViewById(R.id.exhibit_editor_edit_info)
                .setOnClickListener(onEditButtonPress);
        editorView.findViewById(R.id.exhibit_editor_save).setOnClickListener(onSaveButtonPress);

        // setup the list of beacons
        RecyclerView beaconList = (RecyclerView) editorView
                .findViewById(R.id.exhibit_editor_beacon_list);

        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        beaconList.setLayoutManager(linearLayoutManager);

        beaconAdapter = new BeaconAdapter();
        beaconList.setAdapter(beaconAdapter);

        // place divider between beacon items
        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(beaconList.getContext(), DividerItemDecoration.VERTICAL);
        beaconList.addItemDecoration(dividerItemDecoration);

        return editorView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // set the app bar title
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(FRAGMENT_TITLE);
    }

    // called when the "EDIT" button is pressed
    private View.OnClickListener onEditButtonPress = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View fragmentView = getView();
            if (fragmentView == null)
                throw new RuntimeException("Looks like we aren't in the fragment");

            v.setVisibility(View.INVISIBLE);
            fragmentView.findViewById(R.id.exhibit_editor_save).setVisibility(View.VISIBLE);
            fragmentView.findViewById(R.id.exhibit_editor_description_layout).setEnabled(true);
        }
    };

    // called when the save button is pressed, after editing
    private View.OnClickListener onSaveButtonPress = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View fragmentView = getView();
            if (fragmentView == null)
                throw new RuntimeException("Looks like we aren't in the fragment");

            v.setVisibility(View.INVISIBLE);
            fragmentView.findViewById(R.id.exhibit_editor_edit_info).setVisibility(View.VISIBLE);
            fragmentView.findViewById(R.id.exhibit_editor_description_layout).setEnabled(false);

            String description = ((EditText) fragmentView
                    .findViewById(R.id.exhibit_editor_description))
                    .getText().toString();
            workingExhibit.setDescription(description);
        }
    };

    class BeaconAdapter extends RecyclerView.Adapter<BeaconAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewtype) {
            View listItem = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.item_exhibit_editor_beacon, parent, false);

            return new ViewHolder(listItem);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            String beaconName = beaconManager.getBeaconByIndex(position).friendlyName;
            final long beaconId = beaconManager.getBeaconByIndex(position).id;

            viewHolder.beaconTitle.setText(beaconName);
            viewHolder.background.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Fragment contentEditor = new ExhibitContentFragment();
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    Bundle bundle = new Bundle();
                    bundle.putLong("exhibit-id", workingExhibit.getId());
                    bundle.putLong("beacon-id", beaconId);
                    contentEditor.setArguments(bundle);
                    transaction.replace(R.id.fragment_container, contentEditor);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            });
        }

        @Override
        public int getItemCount() {
            return beaconManager.getAllBeacons().size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView beaconTitle;
            View background;

            ViewHolder(View view) {
                super(view);
                beaconTitle = (TextView) view.findViewById(R.id.item_exhibit_editor_beacon_title);
                background = view;
            }
        }
    }
}
