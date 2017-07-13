package org.physical_web.cms.exhibits;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flipboard.bottomsheet.BottomSheetLayout;

import org.physical_web.cms.R;

/**
 * Fragment for displaying information about exhibits
 */
public class ExhibitFragment extends Fragment {
    private ExhibitManager exhibitManager;
    private ExhibitAdapter exhibitAdapter;

    private BottomSheetLayout bottomSheet;

    public ExhibitFragment() {
        // required empty constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ExhibitFragment.
     */
    public static ExhibitFragment newInstance() {
        ExhibitFragment fragment = new ExhibitFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragment = inflater.inflate(R.layout.fragment_exhibit, container, false);

        exhibitManager = new ExhibitManager(getActivity());

        FloatingActionButton fab = (FloatingActionButton) fragment
                .findViewById(R.id.exhibit_add_item);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewExhibitSheet();
            }
        });

        RecyclerView exhibitList = (RecyclerView) fragment.findViewById(R.id.exhibit_stored_list);

        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        exhibitList.setLayoutManager(linearLayoutManager);

        exhibitAdapter = new ExhibitAdapter();
        exhibitList.setAdapter(exhibitAdapter);

        return fragment;
    }

    private void showNewExhibitSheet() {
        bottomSheet = (BottomSheetLayout) getView().findViewById(R.id.exhibit_sheet);

        if(bottomSheet == null)
            throw new IllegalStateException("No bottom sheet in current view");

        View sheetView = LayoutInflater.from(getActivity())
                .inflate(R.layout.sheet_new_exhibit, bottomSheet, false);
        sheetView.findViewById(R.id.sheet_new_exhibit_confirm).setOnClickListener(addExhibit);

        sheetView.findViewById(R.id.sheet_new_exhibit_close)
                .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheet.dismissSheet();
            }
        });

        bottomSheet.showWithSheetView(sheetView);
    }

    private View.OnClickListener addExhibit = new View.OnClickListener() {
        @Override
        public void onClick(View exhibitSheet) {
            String exhibitName = ((TextInputEditText) bottomSheet
                    .findViewById(R.id.sheet_new_exhibit_name)).getText().toString();

            Exhibit newExhibit = new Exhibit(exhibitName);
            exhibitManager.insertExhibit(newExhibit);
            exhibitManager.refresh();

            exhibitAdapter.notifyDataSetChanged();
            bottomSheet.dismissSheet();
        }
    };

    class ExhibitAdapter extends RecyclerView.Adapter<ExhibitAdapter.ViewHolder> {
        @Override
        public ExhibitAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewtype) {
            View listItem = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.item_exhibit_list, parent, false);

            return new ViewHolder(listItem);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            Exhibit exhibitToDraw = exhibitManager.getExhibit(position);
            viewHolder.exhibitTitle.setText(exhibitToDraw.getTitle());
        }

        @Override
        public int getItemCount() {
            return exhibitManager.getExhibitCount();
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            TextView exhibitTitle;

            ViewHolder(View view) {
                super(view);
                exhibitTitle = (TextView) view.findViewById(R.id.item_exhibit_list_title);
            }
        }
    }
}
