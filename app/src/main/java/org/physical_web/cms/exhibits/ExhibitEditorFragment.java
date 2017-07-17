package org.physical_web.cms.exhibits;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.physical_web.cms.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ExhibitEditorFragment extends Fragment {
    private final static String TAG = ExhibitEditorFragment.class.getSimpleName();
    private ExhibitManager exhibitManager = ExhibitManager.getInstance();
    private ExhibitEditor exhibitEditor;

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
        Exhibit workingExhibit = exhibitManager.getByName(exhibitName);

        exhibitEditor = new ExhibitEditor(workingExhibit);

        ((EditText) editorView.findViewById(R.id.exhibit_editor_title))
                .setText(workingExhibit.getTitle());

        return editorView;
    }

    class ExhibitEditor {
        private Exhibit workingExhibit;

        public ExhibitEditor(Exhibit exhibit) {
            this.workingExhibit = exhibit;
        }
    }
}
