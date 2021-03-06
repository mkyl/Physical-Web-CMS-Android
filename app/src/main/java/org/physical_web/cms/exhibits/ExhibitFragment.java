package org.physical_web.cms.exhibits;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.flipboard.bottomsheet.BottomSheetLayout;

import org.physical_web.cms.R;

/**
 * Fragment for displaying information about exhibits
 */
public class ExhibitFragment extends Fragment {
    private final static String TAG = ExhibitFragment.class.getSimpleName();
    private final static String FRAGMENT_TITLE = "Exhibits";

    private ExhibitManager exhibitManager = ExhibitManager.getInstance();
    private ExhibitAdapter exhibitAdapter;

    private BottomSheetLayout bottomSheet;

    public ExhibitFragment() {
        // required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragment = inflater.inflate(R.layout.fragment_exhibit, container, false);

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

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(FRAGMENT_TITLE);
        updateActiveExhibitCard();
    }

    private void updateActiveExhibitCard() {
        Exhibit activeExhibit = exhibitManager.getActiveExhibit();
        if (activeExhibit != null) {
            getActivity().findViewById(R.id.exhibit_active_warning).setVisibility(View.GONE);

            TextView title = (TextView) getActivity().findViewById(R.id.exhibit_active_title);
            title.setText(activeExhibit.getTitle());
            title.setVisibility(View.VISIBLE);

            getActivity().findViewById(R.id.exhibit_active_subtitle).setVisibility(View.VISIBLE);
        }
    }

    // pulls up a sheet from the bottom of the screen to allow user to input details of new exhibit
    private void showNewExhibitSheet() {
        bottomSheet = (BottomSheetLayout) getView().findViewById(R.id.exhibit_sheet);

        if (bottomSheet == null)
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

    // called by new exhibit sheet when user hits "add exhibit" button
    private View.OnClickListener addExhibit = new View.OnClickListener() {
        @Override
        public void onClick(View exhibitSheet) {
            bottomSheet.findViewById(R.id.sheet_new_exhibit_name).setEnabled(false);
            String exhibitName = ((TextInputEditText) bottomSheet
                    .findViewById(R.id.sheet_new_exhibit_name)).getText().toString();

            exhibitManager.createNewExhibit(exhibitName);

            exhibitAdapter.notifyDataSetChanged();
            bottomSheet.dismissSheet();
        }
    };

    // allows for undeletion of exhibit
    private void showDeletionUndoBar(final String exhibitName) {
        Snackbar undoSnackbar = Snackbar.
                make(getView(), "Deleted exhibit '" + exhibitName + "'",
                        Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Undeleting exhibit: " + exhibitName);
                        // TODO FIND BETTER WAY THAT DOESN'T DELETE CONTENT
                        exhibitManager.createNewExhibit(exhibitName);
                        exhibitAdapter.notifyDataSetChanged();
                    }
                });
        undoSnackbar.show();
    }

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
            final Exhibit exhibitToDraw = exhibitManager.getExhibit(position);

            // contents of overflow menu
            final PopupMenu.OnMenuItemClickListener menuListener =
                    new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.exhibit_overflow_delete:
                                    Log.d(TAG, "Deleting exhibit with name: " + exhibitToDraw.getTitle());
                                    showDeletionUndoBar(exhibitToDraw.getTitle());
                                    exhibitManager.removeExhibit(exhibitToDraw);
                                    exhibitAdapter.notifyDataSetChanged();
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    };

            viewHolder.exhibitTitle.setText(exhibitToDraw.getTitle());
            // called when user presses overflow button
            viewHolder.overflowListener.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(getActivity(), v);
                    popup.inflate(R.menu.exhibit_overflow);
                    popup.setOnMenuItemClickListener(menuListener);
                    popup.show();
                }
            });

            // called when user clicks on any part that's not a button
            viewHolder.background.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // switch to exhibit editor fragment
                    Fragment exhibitEditor = new ExhibitEditorFragment();
                    Bundle args = new Bundle();
                    args.putLong("exhibit-id", exhibitToDraw.getId());
                    exhibitEditor.setArguments(args);

                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.fragment_container, exhibitEditor);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            });
        }

        @Override
        public int getItemCount() {
            int itemCount = exhibitManager.getExhibitCount();

            // TODO find more idiomatic way to do this
            if (itemCount == 0)
                getView().findViewById(R.id.fragment_exhibit_warning).setVisibility(View.VISIBLE);
            else
                getView().findViewById(R.id.fragment_exhibit_warning).setVisibility(View.GONE);

            return itemCount;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView exhibitTitle;
            ImageButton overflowListener;
            View background;

            ViewHolder(View view) {
                super(view);
                exhibitTitle = (TextView) view.findViewById(R.id.item_exhibit_list_title);
                overflowListener = (ImageButton) view.findViewById(R.id.item_exhibit_list_overflow);
                background = view;
            }
        }
    }
}
