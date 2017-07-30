package org.physical_web.cms.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.physical_web.cms.beacons.Beacon;
import org.physical_web.cms.beacons.BeaconManager;

import java.io.File;
import java.util.Map;

/**
 * Represents a map of a physical space, such as a gallery, and the locations of the beacons
 * it contains.
 */

public class PhysicalMap {
    private final static String TAG = PhysicalMap.class.getSimpleName();

    private final static String MAP_FILE = "map.json";
    private final static String FLOOR_PLAN_FILE = "floor_plan";

    Map<Beacon, Point> beaconLocations;
    Bitmap floorPlan;
    File mapStorage;

    public static PhysicalMap loadFromDisk(Context context) {
        PhysicalMap result = new PhysicalMap();

        result.mapStorage = new File(context.getFilesDir(), MAP_FILE);

        return result;
    }

    /*
    public static PhysicalMap newMap(Uri floorPlan) {
        PhysicalMap result = new PhysicalMap();

        JSONObject mapInfo = initJSONMetadata(imageWidth, imageHeight);

        return result;
    }
    */

    private PhysicalMap() {}

    private static JSONObject initJSONMetadata(int floorPlanWidth, int floorPlanHeight) {
        try {
            JSONObject mapData = new JSONObject();

            mapData.put("width", floorPlanWidth);
            mapData.put("height", floorPlanHeight);

            JSONArray beaconData = new JSONArray();
            for(Beacon beacon : BeaconManager.getInstance().getAllBeacons()) {
                JSONObject beaconEntry = new JSONObject();
                beaconEntry.put("address", beacon.address);
                beaconEntry.put("location", JSONObject.NULL);
                beaconData.put(beaconEntry);
            }
            mapData.put("beacons", beaconData);


            return mapData;
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't create JSON for new map:" + e);
            throw new RuntimeException(e.toString());
        }
    }

    public void setBeaconLocation(Beacon beacon, Point location) {

    }

    /*
    public Point getBeaconLocation(Beacon beacon) {
    }
    */
}
