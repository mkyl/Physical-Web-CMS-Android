package org.physical_web.cms.exhibits;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.physical_web.cms.beacons.Beacon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static util.MiscFile.deleteDir;

/**
 * Represents an exhibition, or a set of content assigned to a number of beacons. These sets can
 * be composed, deployed and swapped from the app.
 */
public class Exhibit {
    private static final String TAG = Exhibit.class.getSimpleName();
    private static final String METADATA_FILE_NAME = "metadata.json";

    private String title;
    private Boolean active;
    private Map<Beacon, List<ExhibitContent>> contentsPerBeacon;
    private JSONObject metadata;
    private File exhibitFolder;

    public Exhibit(String title) {
        this.title = title;
        this.active = false;
        this.contentsPerBeacon = new HashMap<>();
    }

    public static Exhibit fromFolder(File folder) {
        String name = folder.getName();
        Exhibit result = new Exhibit(name);

        result.active = false;
        result.contentsPerBeacon = result.loadExhibitContentsFromFolder(folder);
        result.metadata = result.loadMetadataFromFolder(folder);
        result.exhibitFolder = folder;

        return result;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        try {
            return this.metadata.getString("description");
        } catch (JSONException e) {
            Log.e(TAG, "JSONEXCEPTION while getting description");
            return null;
        }
    }

    public void setDescription(String newDescription) {
        try {
            this.metadata.put("description", newDescription);
            writeMetaDataToFile(new File(exhibitFolder, METADATA_FILE_NAME));
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /*
    public void addContent(ExhibitContent content) {
        this.exhibitContents.add(content);
    }

    public void removeContent(ExhibitContent content) {
        if (this.exhibitContents.contains(content))
            this.exhibitContents.remove(content);
        else
            throw new IllegalArgumentException("Content not found in exhibit");
    }
    */

    public void makeActive() {
        this.active = true;
        // TODO complete method
    }

    private Map<Beacon, List<ExhibitContent>> loadExhibitContentsFromFolder(File exhibitFolder) {
        if(exhibitFolder.isFile())
            throw new IllegalArgumentException("Passed file, not folder");

        Map<Beacon, List<ExhibitContent>> result = new HashMap<>();

        for(File child : exhibitFolder.listFiles()) {
            if (!child.isFile()) {
                Beacon beacon = new Beacon("", child.getName());
                List<ExhibitContent> beaconContents = loadBeaconContentsFromFolder(child);
                result.put(beacon, beaconContents);
            }
        }

        return result;
    }

    private List<ExhibitContent> loadBeaconContentsFromFolder(File beaconFolder) {
        if(beaconFolder.isFile())
            throw new IllegalArgumentException("Passed file, not folder");

        List<ExhibitContent> beaconContents = new LinkedList<>();
        for(File child : beaconFolder.listFiles()) {
            if (!child.isFile())
                beaconContents.add(ExhibitContent.fromFile(child));
        }
        return beaconContents;
    }

    private JSONObject loadMetadataFromFolder(File exhibitFolder) {
        JSONObject result = null;
        File metadataFile = new File(exhibitFolder, METADATA_FILE_NAME);

        try {
            if (!metadataFile.exists()) {
                createMetaDataFile(metadataFile);
            }
            String metadataContents = readFile(metadataFile);
            result = new JSONObject(metadataContents);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't read metadata: " + e);
        }

        return result;
    }

    private void writeMetaDataToFile(File metadataFile) throws IOException {
        writeToFile(metadataFile, this.metadata.toString());
    }

    private void createMetaDataFile(File metadataFile) throws IOException, JSONException {
        metadataFile.createNewFile();
        JSONObject freshContents = new JSONObject();
        freshContents.put("active", false);
        freshContents.put("description", "placeholder description");
        freshContents.put("beacons", new JSONArray());

        writeToFile(metadataFile, freshContents.toString());
    }

    public void configureForAdditionalBeacon(Beacon newBeacon) {
        String beaconName = newBeacon.friendlyName;
        File beaconContentFolder = new File(this.exhibitFolder, beaconName);
        if (beaconContentFolder.exists())
            throw new IllegalArgumentException("Beacon content folder already setup");

        try {
            JSONObject name = new JSONObject();
            name.put("beacon-name", beaconName);
            JSONArray contents = new JSONArray();

            JSONArray beaconContent = new JSONArray();
            beaconContent.put(name);
            beaconContent.put(contents);

            metadata.getJSONArray("beacons").put(beaconContent);
        } catch (JSONException e) {
            Log.e(TAG, "Error editing JSON: " + e);
        }

        beaconContentFolder.mkdir();
    }

    public void configureForRemovedBeacon(Beacon removedBeacon) {
        String beaconName = removedBeacon.friendlyName;
        File beaconContentFolder = new File(this.exhibitFolder, beaconName);
        if (!beaconContentFolder.exists())
            throw new IllegalArgumentException("Beacon content folder not found");

        try {
            JSONArray beacons = metadata.getJSONArray("beacons");
            int targetIndex = -1;
            Boolean foundBeacon = false;

            for(int i = 0; i < beacons.length(); i++) {
                JSONArray currentBeacon;
                Object object = beacons.get(i);
                if(object instanceof JSONArray)
                    currentBeacon = (JSONArray) object;
                else
                    throw new IllegalStateException("Corrupted metadata file");

                // name is stored in first field
                JSONObject currentBeaconName = currentBeacon.getJSONObject(0);
                if(currentBeaconName.getString("beacon-name").equals(removedBeacon.friendlyName)) {
                    foundBeacon = true;
                    targetIndex = i;
                }
            }

            if(foundBeacon) {
                if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
                    beacons.remove(targetIndex);
                else
                    Log.e(TAG, "Old android version doesn't support delete.");
                    // TODO do something back lack of delete in API version 18
            } else {
                throw new IllegalArgumentException("No such beacon found to delete");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Removing beacon from metadata failed: " + e);
        }
        deleteDir(beaconContentFolder);
    }

    private String readFile(File file) throws IOException {
        StringBuilder fileContents = new StringBuilder((int)file.length());
        Scanner scanner = new Scanner(file);
        String lineSeparator = System.getProperty("line.separator");

        try {
            while(scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine());
                // avoid extra line separator at end of file
                if(scanner.hasNextLine())
                    fileContents.append(lineSeparator);
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }

    private void writeToFile(File file, String string) throws IOException {
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file));
        outputStreamWriter.write(string);
        outputStreamWriter.close();
    }
}
