package org.physical_web.cms.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.physical_web.cms.beacons.Beacon;
import org.physical_web.cms.beacons.BeaconManager;

import java.util.List;

/**
 * Canvas to draw map of space on, along with the location of the beacons
 */

public class PhysicalMapCanvas extends View {
    private static final String TAG = PhysicalMapCanvas.class.getSimpleName();

    private Bitmap bitmap;
    private Rect viewSizedRectangle = null;
    private PhysicalMap displayedMap = null;
    private List<Beacon> beaconList;

    public PhysicalMapCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public PhysicalMapCanvas(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        beaconList = BeaconManager.getInstance().getAllBeacons();
    }

    public void loadMap(PhysicalMap physicalMap) {
        displayedMap = physicalMap;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (displayedMap != null) {
            canvas.drawBitmap(displayedMap.getFloorPlan(), null, viewSizedRectangle, null);
            for (Beacon beaconToDraw : beaconList) {
                Point beaconLocation = displayedMap.getBeaconLocation(beaconToDraw);
                if (beaconLocation != null) {
                    canvas.drawCircle(beaconLocation.x, beaconLocation.y, 50, new Paint());
                }
            }
        }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (viewSizedRectangle == null) {
            viewSizedRectangle = new Rect(0, 0, getMeasuredWidth(), getMeasuredHeight());
            bitmap = Bitmap.createBitmap(viewSizedRectangle.width(), viewSizedRectangle.height(),
                    Bitmap.Config.ARGB_8888);
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                return true;
            case MotionEvent.ACTION_DOWN:
                return true;
            default:
                return false;
        }
    }
}
