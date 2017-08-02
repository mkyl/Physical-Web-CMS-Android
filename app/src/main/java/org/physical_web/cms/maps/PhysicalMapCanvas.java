package org.physical_web.cms.maps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

import org.physical_web.cms.beacons.Beacon;
import org.physical_web.cms.beacons.BeaconManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Canvas to draw map of space on, along with the location of the beacons
 */

public class PhysicalMapCanvas extends View {
    private static final String TAG = PhysicalMapCanvas.class.getSimpleName();

    private Rect viewSizedRectangle = null;
    private PhysicalMap displayedMap = null;
    private List<Beacon> beaconList;
    private BeaconManager beaconManager;
    private Set<CircleArea> pinAreas;
    private Map<CircleArea, Beacon> pinBeaconMap;
    private List<Paint> colors;
    private Random randomGenerator = null;

    public PhysicalMapCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public PhysicalMapCanvas(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        beaconManager = BeaconManager.getInstance();
        int maxPins = beaconManager.getAllBeacons().size();
        pinAreas = new HashSet<>(maxPins);
        pinBeaconMap = new HashMap<>(maxPins);

        colors = new ArrayList<>(6);
        for(int i = 0; i < 6; i++)
            colors.add(new Paint());

        for(int i = 0; i < 6; i++)
            colors.get(i).setTextSize(30);

        colors.get(0).setColor(Color.BLUE);
        colors.get(1).setColor(Color.YELLOW);
        colors.get(2).setColor(Color.GRAY);
        colors.get(3).setColor(Color.GREEN);
        colors.get(4).setColor(Color.MAGENTA);
        colors.get(5).setColor(Color.RED);
    }

    public void loadMap(PhysicalMap physicalMap) {
        displayedMap = physicalMap;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (displayedMap != null) {
            canvas.drawBitmap(displayedMap.getFloorPlan(), null, viewSizedRectangle, null);

            beaconList = beaconManager.getAllBeacons();
            for (Beacon beaconToDraw : beaconList) {
                Point beaconLocation = displayedMap.getBeaconLocation(beaconToDraw);
                if (beaconLocation != null) {
                    final Paint paint = getRandomColoredPaint(beaconToDraw.address.toString());
                    canvas.drawCircle(beaconLocation.x, beaconLocation.y, 100, paint);
                    canvas.drawText(beaconToDraw.friendlyName, beaconLocation.x - 50,
                            beaconLocation.y + 130, paint);
                    CircleArea pinArea = new CircleArea(beaconLocation.x, beaconLocation.y, 100);
                    pinAreas.add(pinArea);
                    pinBeaconMap.put(pinArea, beaconToDraw);
                }
            }
        }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (viewSizedRectangle == null) {
            viewSizedRectangle = new Rect(0, 0, getMeasuredWidth(), getMeasuredHeight());
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                // adding a beacon
                int xTouch = (int) event.getX(0);
                int yTouch = (int) event.getY(0);

                for(CircleArea pinArea : pinAreas) {
                    if (pinArea.contains(xTouch, yTouch)) {
                        Log.d(TAG, "Touched pin");
                        removePin(pinArea);
                        return true;
                    }
                }

                askToPlacePin(xTouch, yTouch);
                return true;
            default:
                return false;
        }
    }

    private void askToPlacePin(final int x, final int y) {
        final List<Beacon> unpinnedBeacons = new LinkedList<>(beaconList);

        for (CircleArea area : pinAreas) {
            Beacon pinnedBeacon = pinBeaconMap.get(area);
            unpinnedBeacons.remove(pinnedBeacon);
        }

        if (unpinnedBeacons.size() == 0) {
            Toast.makeText(getContext(),
                    "All beacons are already placed",
                    Toast.LENGTH_LONG).show();
        } else {
            final String[] beaconNamesToOffer = new String[unpinnedBeacons.size()];

            for (int i = 0; i < beaconNamesToOffer.length; i++) {
                beaconNamesToOffer[i] = unpinnedBeacons.get(i).friendlyName;
            }

            AlertDialog.Builder alt_bld = new AlertDialog.Builder(getContext());
            alt_bld.setTitle("Choose beacon to place");
            alt_bld.setSingleChoiceItems(beaconNamesToOffer, -1, new DialogInterface
                    .OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    Toast.makeText(getContext(),
                            "To remove a beacon, press the 'X' on the pin",
                            Toast.LENGTH_SHORT).show();

                    placePin(x, y, unpinnedBeacons.get(item));
                    dialog.dismiss(); // dismiss the alertbox after chose option
                }
            });
            AlertDialog alert = alt_bld.create();
            alert.show();
        }
    }

    private void placePin(int x, int y, Beacon beacon) {
        Point location = new Point(x, y);
        displayedMap.setBeaconLocation(beacon, location);
        invalidate();
    }

    private void removePin(CircleArea pinArea) {
        Beacon beaconToUnpin = pinBeaconMap.get(pinArea);
        if (beaconToUnpin == null)
            throw new IllegalArgumentException("No such pin found");

        pinAreas.remove(pinArea);
        pinBeaconMap.remove(pinArea);

        displayedMap.removeBeacon(beaconToUnpin);
        invalidate();
    }

    private Paint getRandomColoredPaint(String seed) {
        if (randomGenerator == null)
            randomGenerator = new Random();

        // multiply by self to fix negative indexes :P
        return colors.get(seed.hashCode() * seed.hashCode() % colors.size());
    }

    private static class CircleArea {
        int radius;
        int centerX;
        int centerY;

        CircleArea(int centerX, int centerY, int radius) {
            this.radius = radius;
            this.centerX = centerX;
            this.centerY = centerY;
        }

        boolean contains(int x, int y) {
            // pythagorean theorem to find distance between center of circle and point of touch
            return Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2)) < radius;
        }

        @Override
        public String toString() {
            return "Circle[" + centerX + ", " + centerY + ", " + radius + "]";
        }

        @Override
        public int hashCode() {
            int hash = centerX;
            hash = hash * 31 + centerY;
            hash = hash * 23 + radius;
            return hash;
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;
            else if (getClass() != object.getClass())
                return false;
            else
                return object.hashCode() == this.hashCode();
        }
    }
}


