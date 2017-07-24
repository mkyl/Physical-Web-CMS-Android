package org.physical_web.cms.exhibits;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.physical_web.cms.beacons.Beacon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

        result.exhibitFolder = folder;
        result.active = false;
        result.contentsPerBeacon = result.loadExhibitContentsFromFolder(folder);
        result.metadata = result.loadMetadataFromFolder(folder);
        result.loadBeaconsIntoMetadata();

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
            new File(exhibitFolder, METADATA_FILE_NAME);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

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

    private void saveMetadata() throws IOException {
        File target = new File(exhibitFolder, METADATA_FILE_NAME);
        writeToFile(target, this.metadata.toString());
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
            JSONObject beacon = new JSONObject();
            beacon.put("beacon-name", beaconName);
            JSONArray contents = new JSONArray();
            beacon.put("beacon-contents", contents);

            metadata.getJSONArray("beacons").put(beacon);
            saveMetadata();
        } catch (Exception e) {
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
                JSONObject currentBeacon = beacons.getJSONObject(i);

                // name is stored in first field
                String currentBeaconName = currentBeacon.getString("beacon-name");
                if(currentBeaconName.equals(removedBeacon.friendlyName)) {
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

            saveMetadata();
        } catch (Exception e) {
            Log.e(TAG, "Removing beacon from metadata failed: " + e);
        }
        deleteDir(beaconContentFolder);
    }

    public void loadBeaconsIntoMetadata() {
        try {
            JSONArray beaconArray = metadata.getJSONArray("beacons");
            if(beaconArray.length() == 0) {
                for(File file : exhibitFolder.listFiles()) {
                    if(!file.isFile()) {
                        JSONObject beacon = new JSONObject();
                        beacon.put("beacon-name", file.getName());

                        JSONArray contents = new JSONArray();
                        beacon.put("beacon-contents", contents);
                        beaconArray.put(beacon);
                    }
                }
                saveMetadata();
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    public String[] getBeaconNames() {
        try {
            JSONArray beaconArray = metadata.getJSONArray("beacons");
            String[] result = new String[beaconArray.length()];
            for(int i = 0; i < beaconArray.length(); i++) {
                JSONObject beacon = beaconArray.getJSONObject(i);
                result[i] = beacon.getString("beacon-name");
            }
            return result;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }

    public ExhibitContent[] getContentForBeacon(String beacon) {
        try {
            JSONArray beacons = metadata.getJSONArray("beacons");
            int targetIndex = -1;
            for (int i = 0; i < beacons.length(); i++) {
                // index of beacon-name is 0
                if (beacons.getJSONObject(i).getString("beacon-name").equals(beacon)) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1)
                throw new IllegalArgumentException("No such beacon found in metadata");

            JSONArray contents = beacons.getJSONObject(targetIndex).getJSONArray("beacon-contents");
            ExhibitContent[] result = new ExhibitContent[contents.length()];

            File beaconFolder = new File(exhibitFolder, beacon);
            for(int i = 0; i < contents.length(); i++) {
                String contentFilename = (String) contents.get(i);
                File targetFile = new File(beaconFolder, contentFilename);

                if (!targetFile.exists())
                    throw new IllegalStateException("Metadata references non-existent file: "
                            + contentFilename);

                result[i] = ExhibitContent.fromFile(targetFile);
            }

            return result;
        } catch (JSONException e) {
            Log.e(TAG, "failed to read metadata: " + e);
            return null;
        }
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

    public void insertContent(Uri uri, String beacon, Context ctx) {
        String displayName;
        InputStream inputStream;

        try {
            Cursor cursor = ctx.getContentResolver().query(uri, null, null, null, null, null);
            cursor.moveToFirst();
            displayName = cursor.getString(
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            cursor.close();

            inputStream = ctx.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "failed to find file: " + e);
            return;
        }

        writeToExhibitFolder(displayName, inputStream, beacon, ctx);
        appendContentToMetadata(displayName, beacon);
    }

    private void writeToExhibitFolder(String filename, InputStream inputStream,
                                      String beacon, Context ctx) {
        try {
            File chosenBeacon = new File(exhibitFolder, beacon);
            File localCopy = new File(chosenBeacon, filename);

            FileOutputStream outputStream = new FileOutputStream(localCopy);

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
        } catch (Exception e) {
            Log.d(TAG, "Error copying file with name " + filename + ": " + e);
        }
    }

    private void appendContentToMetadata(String filename, String beacon) {
        try {
            JSONArray beacons = metadata.getJSONArray("beacons");
            int targetIndex = -1;
            for(int i = 0; i < beacons.length(); i++) {
                // index of beacon-name is 0
                if (beacons.getJSONObject(i).getString("beacon-name").equals(beacon)) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1)
                throw new IllegalArgumentException("No such beacon found in metadata");

            JSONArray contents = beacons.getJSONObject(targetIndex).getJSONArray("beacon-contents");
            contents.put(filename);
            saveMetadata();
        } catch (Exception e) {
            Log.e(TAG, "modifying metadata failed for content with filename: " + filename);
        }
    }
}
