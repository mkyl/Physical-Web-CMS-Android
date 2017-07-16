package org.physical_web.cms.exhibits;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.physical_web.cms.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ExhibitEditor extends Fragment {
    private final static String TAG = ExhibitEditor.class.getSimpleName();
    public ExhibitEditor() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle b = getArguments();
        if (b == null)
            throw new IllegalArgumentException("No exhibit provided to edit");

        String s = b.getString("exhibit-name");
        Log.d(TAG, "Editing exhibit with name: " + s);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_exhibit_editor, container, false);
    }
}
