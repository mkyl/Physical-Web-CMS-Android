package org.physical_web.cms.maps;

import android.app.Activity;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import util.BitmapResampling;
import util.MiscFile;

/**
 * Represents a map of a physical space, such as a gallery, and the locations of the beacons
 * it contains.
 */

public class PhysicalMap {
    private final static String TAG = PhysicalMap.class.getSimpleName();

    private final static String MAP_INFO_FILE = "map.json";
    private final static String MAP_IMAGE_FILE = "floor_plan_image";

    private int x;
    private int y;
    private Map<Beacon, Point> beaconLocations;
    private JSONObject metadata;
    private Bitmap floorPlan;
    private File mapInfoStorage;
    private File mapImageStorage;

    /**
     * Load the PhysicalMap, if any, from disk.
     *
     * @param context to search for PhysicalMap with
     * @return the stored PhysicalMap; null if none stored
     */
    public static PhysicalMap loadFromDisk(Activity context) {
        PhysicalMap result = new PhysicalMap();

        result.mapInfoStorage = new File(context.getFilesDir(), MAP_INFO_FILE);
        result.mapImageStorage = new File(context.getFilesDir(), MAP_IMAGE_FILE);

        if (!(result.mapInfoStorage.exists() && result.mapImageStorage.exists()))
            return null;

        // sample the bitmap down to resolution of the device if it's too large
        Point display = new Point();
        context.getWindowManager().getDefaultDisplay().getRealSize(display);
        int height = display.y;
        int width = display.x;
        result.floorPlan = BitmapResampling.decodeSampledBitmapFromFile(result.mapImageStorage,
                width, height);

        try {
            result.metadata = new JSONObject(MiscFile.readFile(result.mapInfoStorage));
            if (result.metadata == JSONObject.NULL)
                return null;
        } catch (IOException e) {
            Log.e(TAG, "Couldn't read map metadata file: " + e);
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse map metadata file: " + e);
            return null;
        }

        result.beaconLocations = result.loadBeaconLocations();

        return result;
    }

    /**
     * Create a new PhysicalMap, discarding the older one. This will write the new PhysicalMap to
     * disk so that it may be loaded with {@link #loadFromDisk(Activity)} later.
     *
     * @param floorPlan points to image of space floor map, to be used as background for map
     * @param context to create map in and store data in
     * @return newly created PhysicalMap, null if creation failed
     */
    public static PhysicalMap newMap(Uri floorPlan, Activity context) {
        PhysicalMap result = new PhysicalMap();

        File imageFile = MiscFile.copyURIContentsToFolder(floorPlan, MAP_IMAGE_FILE,
                context.getFilesDir(), context);

        Point imageDimensions = BitmapResampling.getDimensions(imageFile);

        JSONObject mapInfo = generateJSONMetadata(imageDimensions.x, imageDimensions.y);
        File mapMetadataStorage = new File(context.getFilesDir(), MAP_INFO_FILE);

        try {
            MiscFile.writeToFile(mapMetadataStorage, mapInfo.toString());
        } catch (IOException e) {
            Log.e(TAG, "Couldn't initialize metadata for map");
            return null;
        }

        return loadFromDisk(context);
    }

    /**
     * Store the location of a beacon in this PhysicalMap
     *
     * @param beacon whose location should be stored/updated
     * @param location location of the beacon
     */
    public void setBeaconLocation(Beacon beacon, Point location) {
        if (location == null)
            throw new IllegalArgumentException("Invalid point provided");

        try {
            JSONObject beaconEntry = new JSONObject();
            beaconEntry.put("address", beacon.address.toString());
            beaconEntry.put("x", location.x);
            beaconEntry.put("y", location.y);
            metadata.getJSONArray("beacons").put(beaconEntry);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to insert beacon into location");
        }

        saveMetadata();
    }

    public void removeBeacon(Beacon beacon) {
        String formattedBeaconMac = beacon.address.toString();
        try {
            JSONArray beacons = metadata.getJSONArray("beacons");
            int removalTarget = -1;
            for (int i = 0; i < beacons.length(); i++) {
                if (formattedBeaconMac.equals(beacons.getJSONObject(i).getString("address"))) {
                    removalTarget = i;
                }
            }

            if (removalTarget == -1)
                throw new IllegalArgumentException("No such beacon to remove");
            else {
                beacons.remove(removalTarget);
                saveMetadata();
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON problems while removing beacon from map: " + e);
        }
    }

    /**
     * Get the location of a beacon
     *
     * @param beacon target of the search
     * @return Point representing the location of the beacon on the map or null if no beacon found
     * @throws IllegalArgumentException if no such beacon is found
     */
    public Point getBeaconLocation(Beacon beacon) {
        try {
            JSONArray beaconsInfo = metadata.getJSONArray("beacons");
            for (int i = 0; i < beaconsInfo.length(); i++) {
                JSONObject beaconEntry = beaconsInfo.getJSONObject(i);
                if (beacon.address.toString().equals(beaconEntry.getString("address"))) {
                    if (beaconEntry.get("x") == JSONObject.NULL ||
                            beaconEntry.get("y") == JSONObject.NULL)
                        return null;
                    else
                        return new Point(beaconEntry.getInt("x"), beaconEntry.getInt("y"));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Trouble parsing JSON: " + e);
            return null;
        }

        return null;
        //throw new IllegalArgumentException("No such beacon found");
    }

    public Bitmap getFloorPlan() {
        return floorPlan;
    }

    // for use in by other, public constructors
    private PhysicalMap() {
        // empty constructor
    }

    // return template, empty JSON for a map
    private static JSONObject generateJSONMetadata(int width, int height) {
        try {
            JSONObject mapData = new JSONObject();

            mapData.put("map-width", width);
            mapData.put("map-height", height);
            mapData.put("beacons", new JSONArray());


            return mapData;
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't create JSON for new map:" + e);
            throw new RuntimeException(e.toString());
        }
    }

    // load the locations of the beacons from the JSON metadata file
    private Map<Beacon, Point> loadBeaconLocations() {
        Map<Beacon, Point> result = new HashMap<>();
        for (Beacon beacon : BeaconManager.getInstance().getAllBeacons()) {
            result.put(beacon, getBeaconLocation(beacon));
        }
        return result;
    }

    // write metadata to disk
    private void saveMetadata() {
        try {
            MiscFile.writeToFile(mapInfoStorage, metadata.toString());
        } catch (IOException e) {
            Log.e(TAG, "Failed to save metadata: " + e);
        }
    }
}
