package org.physical_web.cms.exhibits;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.physical_web.cms.R;

/**
 * Handles editing the exhibit metadata as well as listing the beacons involved in
 * exhibit
 */
public class ExhibitEditorFragment extends Fragment {
    private final static String TAG = ExhibitEditorFragment.class.getSimpleName();
    private final static String FRAGMENT_TITLE = "Exhibit Editor";

    private ExhibitManager exhibitManager = ExhibitManager.getInstance();
    private Exhibit workingExhibit;
    private ExhibitEditor exhibitEditor;

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

        String exhibitName = bundle.getString("exhibit-name");
        workingExhibit = exhibitManager.getByName(exhibitName);

        exhibitEditor = new ExhibitEditor(workingExhibit);

        ((EditText) editorView.findViewById(R.id.exhibit_editor_title))
                .setText(workingExhibit.getTitle());
        ((EditText) editorView.findViewById(R.id.exhibit_editor_description))
                .setText(workingExhibit.getDescription());

        ((Button) editorView.findViewById(R.id.exhibit_editor_edit_info))
                .setOnClickListener(onEditButtonPress);
        ((Button) editorView.findViewById(R.id.exhibit_editor_save))
                .setOnClickListener(onSaveButtonPress);

        RecyclerView beaconList = (RecyclerView) editorView
                .findViewById(R.id.exhibit_editor_beacon_list);

        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        beaconList.setLayoutManager(linearLayoutManager);

        beaconAdapter = new BeaconAdapter();
        beaconList.setAdapter(beaconAdapter);

        return editorView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(FRAGMENT_TITLE);
    }

    private View.OnClickListener onEditButtonPress = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View fragmentView = getView();
            if(fragmentView == null)
                throw new RuntimeException("Looks like we aren't in the fragment");

            v.setVisibility(View.INVISIBLE);
            fragmentView.findViewById(R.id.exhibit_editor_save).setVisibility(View.VISIBLE);
            fragmentView.findViewById(R.id.exhibit_editor_description_layout).setEnabled(true);
        }
    };

    private View.OnClickListener onSaveButtonPress = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            View fragmentView = getView();
            if(fragmentView == null)
                throw new RuntimeException("Looks like we aren't in the fragment");

            v.setVisibility(View.INVISIBLE);
            fragmentView.findViewById(R.id.exhibit_editor_edit_info).setVisibility(View.VISIBLE);
            fragmentView.findViewById(R.id.exhibit_editor_description_layout).setEnabled(false);

            String description = ((EditText) fragmentView
                    .findViewById(R.id.exhibit_editor_description))
                    .getText().toString();
            exhibitEditor.setDescription(description);
        }
    };

    class ExhibitEditor {
        private Exhibit workingExhibit;

        public ExhibitEditor(Exhibit exhibit) {
            this.workingExhibit = exhibit;
        }

        public void setDescription(String newDescription) {
            this.workingExhibit.setDescription(newDescription);
        }
    }

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
            final String beaconName = ExhibitEditorFragment.this.exhibitEditor.workingExhibit
                    .getBeaconNames()[position];
            viewHolder.beaconTitle.setText(beaconName);
            viewHolder.background.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Fragment contentEditor = new ExhibitContentFragment();
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    Bundle bundle = new Bundle();
                    bundle.putString("exhibit-name", workingExhibit.getTitle());
                    bundle.putString("beacon-name", beaconName);
                    contentEditor.setArguments(bundle);
                    transaction.replace(R.id.fragment_container, contentEditor);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            });
        }

        @Override
        public int getItemCount() {
            return ExhibitEditorFragment.this.exhibitEditor.workingExhibit.getBeaconNames().length;
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
