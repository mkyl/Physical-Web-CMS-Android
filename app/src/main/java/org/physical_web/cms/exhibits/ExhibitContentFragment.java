package org.physical_web.cms.exhibits;


import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import org.physical_web.cms.R;
import org.physical_web.cms.beacons.Beacon;
import org.physical_web.cms.beacons.BeaconManager;

import java.util.Collections;
import java.util.List;

import nl.changer.audiowife.AudioWife;


/**
 * Manages content for a single beacon within an exhibit.
 */
public class ExhibitContentFragment extends Fragment {
    public final static String TAG = ExhibitContentFragment.class.getSimpleName();
    private final static String FRAGMENT_TITLE = "Editing ";

    private final static int FILE_PICKER_ROUTING_CODE = 1032;
    private int IMAGE_CARD_MAX_WIDTH;
    private int IMAGE_CARD_MAX_HEIGHT;



    private Exhibit workingExhibit;
    private Beacon workingBeacon;

    private ContentAdapter contentAdapter;
    private ItemTouchHelper dragHelper;

    public ExhibitContentFragment() {
        // required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle passedArguments = getArguments();
        if(passedArguments == null)
            throw new IllegalArgumentException("beacon name to work on must be provided");

        Long exhibitId = passedArguments.getLong("exhibit-id");
        workingExhibit = ExhibitManager.getInstance().getById(exhibitId);

        Long beaconId = passedArguments.getLong("beacon-id");
        workingBeacon = BeaconManager.getInstance().getBeaconById(beaconId);

        // Inflate the layout for this fragment
        View result = inflater.inflate(R.layout.fragment_exhibit_content, container, false);

        // set up the floating action button
        FloatingActionButton addContentButton =
                (FloatingActionButton) result.findViewById(R.id.fragment_exhibit_content_add);
        addContentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addContent();
            }
        });

        // setup the list of content
        RecyclerView contentList = (RecyclerView) result
                .findViewById(R.id.fragment_exhibit_content_list);

        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        contentList.setLayoutManager(linearLayoutManager);

        contentAdapter = new ContentAdapter();
        contentList.setAdapter(contentAdapter);

        // set up card dragging code
        DragHelperCallback dragHelperCallback = new DragHelperCallback(contentAdapter);
        dragHelper = new ItemTouchHelper(dragHelperCallback);
        dragHelper.attachToRecyclerView(contentList);

        IMAGE_CARD_MAX_WIDTH = getResources().getDisplayMetrics().widthPixels;
        IMAGE_CARD_MAX_HEIGHT = (int) (200 * getResources().getDisplayMetrics()
                .density + 0.5f);

        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(FRAGMENT_TITLE
                + workingBeacon.friendlyName);
    }

    private void addContent() {
        Intent filePickerIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        filePickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
        // must be set to */* to allow multiple MIME types
        filePickerIntent.setTypeAndNormalize("*/*");
        String[] acceptableMimeTypes = {"image/*", "video/*", "audio/*"};
        filePickerIntent.putExtra("EXTRA_MIME_TYPES", acceptableMimeTypes);

        startActivityForResult(filePickerIntent, FILE_PICKER_ROUTING_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case FILE_PICKER_ROUTING_CODE:
                    // we just got a new content file
                    handleURI(resultData.getData());
                    break;
                default:
                    throw new UnsupportedOperationException("Unrecognized routing code received");
            }
        } else {
            Log.e(TAG, "Activity request with request code " + requestCode
                    + " failed with error code " + resultCode);
        }
    }

    // given a URI to a file, add it to the content for the working beacon
    private void handleURI(Uri uri) {
        Log.d(TAG, "received URI: " + uri);
        workingExhibit.insertContent(uri, workingBeacon, getActivity());
        // inserted at very end
        contentAdapter.notifyItemInserted(contentAdapter.getItemCount() - 1);
    }

    class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ViewHolder>
            implements CardDragNotifier {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewtype) {
            View listItem = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.item_exhibit_content, parent, false);

            return new ViewHolder(listItem);
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, int position) {
            final ExhibitContent content = workingExhibit.getContentForBeacon(workingBeacon)
                    .get(position);
            String contentName = content.getContentName();
            viewHolder.contentTitle.setText(contentName);

            // in case view is recycled
            viewHolder.videoView.setVisibility(View.GONE);
            viewHolder.imageView.setVisibility(View.GONE);
            viewHolder.soundView.setVisibility(View.GONE);
            viewHolder.textView.setVisibility(View.GONE);

            if(content instanceof ImageContent) {
                viewHolder.imageView.setImageBitmap(((ImageContent) content)
                        .getSampledBitmap(IMAGE_CARD_MAX_HEIGHT, IMAGE_CARD_MAX_WIDTH));
                viewHolder.imageView.setVisibility(View.VISIBLE);
            } else if (content instanceof VideoContent) {
                viewHolder.videoView.setVideoPath(((VideoContent) content).getVideoPath());
                // TODO figure out how to make this scroll along video
                viewHolder.videoView.seekTo(10);
                MediaController mediaController = new MediaController(getActivity());
                mediaController.setAnchorView(viewHolder.videoView);
                viewHolder.videoView.setMediaController(mediaController);
                viewHolder.videoView.setVisibility(View.VISIBLE);
            } else if (content instanceof  SoundContent) {
                Uri soundUri = ((SoundContent) content).getURI();
                AudioWife.getInstance().init(getActivity(), soundUri)
                        .useDefaultUi(viewHolder.soundView, getActivity().getLayoutInflater());
                viewHolder.soundView.setVisibility(View.VISIBLE);
            } else if (content instanceof TextContent) {
                viewHolder.textView.setText(((TextContent) content).getText());
                viewHolder.textView.setVisibility(View.VISIBLE);
            }

            viewHolder.dragHandle.setOnTouchListener(new View.OnTouchListener(){
                public boolean onTouch(View v, MotionEvent event) {
                    dragHelper.startDrag(viewHolder);
                    return false;
                }
            });

            viewHolder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    workingExhibit.removeContent(content, workingBeacon);
                    contentAdapter.notifyItemRemoved(viewHolder.getLayoutPosition());
                }
            });
        }

        @Override
        public int getItemCount() {
            int itemCount = workingExhibit.getContentForBeacon(workingBeacon).size();

            // TODO find more idiomatic way to do this
            if (itemCount == 0)
                getView().findViewById(R.id.fragment_exhibit_content_warning)
                        .setVisibility(View.VISIBLE);
            else
                getView().findViewById(R.id.fragment_exhibit_content_warning)
                        .setVisibility(View.GONE);

            return itemCount;
        }

        @Override
        public void onItemMove(int fromPosition, int toPosition) {
            List<ExhibitContent> contentList = workingExhibit.getContentForBeacon(workingBeacon);
            Collections.swap(contentList, fromPosition, toPosition);
            contentAdapter.notifyItemMoved(fromPosition, toPosition);
            workingExhibit.persistContentChanges(workingBeacon);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView contentTitle;
            ImageView imageView;
            VideoView videoView;
            LinearLayout soundView;
            TextView textView;
            ImageButton dragHandle;
            ImageButton delete;

            ViewHolder(View view) {
                super(view);
                contentTitle = (TextView) view.findViewById(R.id.item_exhibit_content_title);
                imageView = (ImageView) view.findViewById(R.id.item_exhibit_content_image);
                videoView = (VideoView) view.findViewById(R.id.item_exhibit_content_video);
                soundView = (LinearLayout) view.findViewById(R.id.item_exhibit_content_audio);
                textView = (TextView) view.findViewById(R.id.item_exhibit_content_text);
                dragHandle = (ImageButton) view.findViewById(R.id.item_exhibit_content_handle);
                delete = (ImageButton) view.findViewById(R.id.item_exhibit_content_delete);
            }
        }
    }
}

class DragHelperCallback extends ItemTouchHelper.Callback {
    private CardDragNotifier cardDragNotifier;

    public DragHelperCallback(CardDragNotifier helperAdapter) {
        this.cardDragNotifier = helperAdapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        // yes, we want to drag, but only from drag handles
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        cardDragNotifier.onItemMove(viewHolder.getAdapterPosition(),
                target.getAdapterPosition());
        return true;
    }
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder,
                         int direction) {
        // do nothing, swipe is disabled
    }
}

interface CardDragNotifier {
    void onItemMove(int fromPosition, int toPosition);
}
