package org.physical_web.cms.exhibits;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.physical_web.cms.beacons.Beacon;
import org.physical_web.cms.beacons.BeaconManager;
import org.physical_web.cms.sync.ContentSynchronizer;

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
import java.util.Random;
import java.util.Scanner;

import static util.MiscFile.deleteDir;

/**
 * Represents an exhibition, or a set of content assigned to a number of beacons. These sets can
 * be composed, deployed and swapped from the app.
 */
public class Exhibit {
    private static final String TAG = Exhibit.class.getSimpleName();
    private static final String METADATA_FILE_NAME = "metadata.json";

    private long id;
    private Map<Beacon, File> contentFolderForBeacon;
    private Map<Beacon, List<ExhibitContent>> contentsForBeacon;
    private JSONObject metadata;
    private File exhibitFolder;

    /**
     * Return an exhibit, given a folder that contains a metadata file and beacon folders
     * @param exhibitFolder
     * @return
     */
    public static Exhibit loadFromFolder(File exhibitFolder) {
        if(exhibitFolder.isFile())
            throw new IllegalArgumentException("Passed file, not folder");

        Exhibit loadedExhibit = new Exhibit();

        loadedExhibit.exhibitFolder = exhibitFolder;
        loadedExhibit.id = Long.valueOf(exhibitFolder.getName());

        File metadataFile = new File(exhibitFolder, METADATA_FILE_NAME);
        loadedExhibit.metadata = loadMetadataFile(metadataFile);

        loadedExhibit.contentFolderForBeacon = findFoldersForBeacons(exhibitFolder);
        loadedExhibit.contentsForBeacon =
                loadedExhibit.loadBeaconContentMap(loadedExhibit.contentFolderForBeacon);

        return loadedExhibit;
    }

    public static Exhibit initializeIntoFolder(String exhibitName, File parentFolder) {
        if(!parentFolder.exists() || parentFolder.isFile())
            throw new IllegalArgumentException("Invalid parent folder");

        Exhibit exhibit = new Exhibit();
        exhibit.id = new Random().nextLong();

        String exhibitFolderName = String.valueOf(exhibit.id);
        File exhibitFolder = new File(parentFolder, exhibitFolderName);

        Boolean createFolderSuccess = exhibitFolder.mkdir();
        if(!createFolderSuccess)
            throw new InternalError("Couldn't create exhibit folder");

        createBeaconContentFolders(exhibitFolder);
        createExhibitMetadataFile(exhibitName, exhibitFolder);

        return Exhibit.loadFromFolder(exhibitFolder);
    }

    private static void createBeaconContentFolders(File exhibitFolder) {
        if(!exhibitFolder.exists() || exhibitFolder.isFile())
            throw new IllegalArgumentException("Invalid exhibit folder");

        BeaconManager beaconManager = BeaconManager.getInstance();
        List<Beacon> beacons = beaconManager.getAllBeacons();

        for(Beacon beacon : beacons) {
            String beaconContentFolderName = String.valueOf(beacon.id);
            File beaconContentFolder = new File(exhibitFolder, beaconContentFolderName);
            Boolean createContentFolderSuccess = beaconContentFolder.mkdir();
            if(!createContentFolderSuccess)
                throw new InternalError("Couldn't create folder");
        }
    }

    private static void createExhibitMetadataFile(String exhibitName, File exhibitFolder) {
        if(!exhibitFolder.exists() || exhibitFolder.isFile())
            throw new IllegalArgumentException("Invalid exhibit folder");

        File metadataFile = new File(exhibitFolder, METADATA_FILE_NAME);
        if (metadataFile.exists())
            throw new IllegalStateException("Metadata file already exists");

        try {
            JSONObject newMetadata = new JSONObject();
            newMetadata.put("name", exhibitName);
            newMetadata.put("active", false);
            newMetadata.put("description", "");

            BeaconManager beaconManager = BeaconManager.getInstance();
            List<Beacon> beaconList = beaconManager.getAllBeacons();
            JSONArray beacons = new JSONArray();
            for(Beacon beacon : beaconList) {
                JSONObject beaconJSONMetadata = new JSONObject();
                beaconJSONMetadata.put("id", beacon.id);
                beaconJSONMetadata.put("contents", new JSONArray());
                beacons.put(beaconJSONMetadata);
            }
            newMetadata.put("beacons", beacons);

            writeToFile(metadataFile, newMetadata.toString());
        } catch (JSONException jsonException) {
            Log.e(TAG, "Couldn't create metadata JSON: " + jsonException);
        } catch (IOException iOException) {
            Log.e(TAG, "Couldn't create metadata file: " + iOException);
        }
    }

    private Exhibit() {}

    private static Map<Beacon, File> findFoldersForBeacons(File exhibitFolder) {
        Map<Beacon, File> beaconFileMap = new HashMap<>();

        if(!exhibitFolder.exists() || exhibitFolder.isFile())
            throw new IllegalArgumentException("Invalid exhibit folder");

        BeaconManager beaconManager = BeaconManager.getInstance();

        for(File folder : exhibitFolder.listFiles()) {
            if (!folder.isFile()) {
                // beacon folder names are the id's of the corresponding beacon
                Beacon correspondingBeacon = beaconManager
                        .getBeaconById(Long.valueOf(folder.getName()));
                if (correspondingBeacon != null)
                    beaconFileMap.put(correspondingBeacon, folder);
                else
                    Log.w(TAG, "Odd, no beacon for folder: " + folder.getAbsolutePath());
            }
        }

        return beaconFileMap;
    }

    /**
     * Returns title of the exhibit
     */
    public String getTitle() {
        try {
            return metadata.getString("name");
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't find name: " + e);
            return "Unknown";
        }
    }

    public File getExhibitFolder() {
        return exhibitFolder;
    }

    public long getId() {
        return id;
    }

    /**
     * Return the description of the exhibit
     */
    public String getDescription() {
        try {
            return this.metadata.getString("description");
        } catch (JSONException e) {
            Log.e(TAG, "JSONEXCEPTION while getting description");
            return "Unknown";
        }
    }

    /**
     * Sets the description of the exhibit, storing it persistently
     * @param newDescription
     */
    public void setDescription(String newDescription) {
        try {
            this.metadata.put("description", newDescription);
            saveMetadata();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void persistContentChanges(Beacon beacon) {
        List<ExhibitContent> changedContents = contentsForBeacon.get(beacon);
        JSONObject beaconMetadata = getBeaconMetadata(beacon);
        try {
            JSONArray changedContentList = new JSONArray();
            for(ExhibitContent exhibitContent : changedContents) {
                changedContentList.put(exhibitContent.getContentName());
            }
            beaconMetadata.put("contents", changedContentList);
            saveMetadata();
        } catch (JSONException e) {
            Log.e(TAG, "Error persisting content changes: " + e);
        } catch (IOException e) {
            Log.e(TAG, "Error writing new content order to JSON" + e);
        }
    }

    // fills this.result with contents from the beacon folders
    private Map<Beacon, List<ExhibitContent>> loadBeaconContentMap
        (Map<Beacon, File> beaconFileMap) {
        Map<Beacon, List<ExhibitContent>> beaconContentMap = new HashMap<>();

        BeaconManager beaconManager = BeaconManager.getInstance();
        List<Beacon> beacons = beaconManager.getAllBeacons();

        for(Beacon beacon : beacons) {
            File beaconContentFolder = beaconFileMap.get(beacon);

            if (beaconContentFolder == null) {
                Log.e(TAG, "No folder for beacon: " + beacon.friendlyName);
            } else {
                List<ExhibitContent> beaconContents =
                        loadBeaconContents(beacon);
                beaconContentMap.put(beacon, beaconContents);
            }
        }

        return beaconContentMap;
    }

    private List<ExhibitContent> loadBeaconContents(Beacon beacon) {
        List<ExhibitContent> beaconContents = new LinkedList<>();
        File beaconFolder = contentFolderForBeacon.get(beacon);

        try {
            JSONArray registeredContents = getBeaconMetadata(beacon).getJSONArray("contents");

            for (int i = 0; i < registeredContents.length(); i++) {
                String fileName = registeredContents.getString(i);
                File contentFile = new File(beaconFolder, fileName);

                if (!contentFile.exists())
                    throw new IllegalStateException("file referenced in JSON but doesn't exist: "
                            + fileName);

                beaconContents.add(ExhibitContent.fromFile(contentFile));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Trouble loading beacon contents from JSON file: " + e);
        }

        return beaconContents;
    }

    private static JSONObject loadMetadataFile(File metadataFile) {
        if (!metadataFile.exists() || !metadataFile.isFile())
            throw new IllegalArgumentException("metadata file isn't valid");

        JSONObject readMetadata = null;

        try {
            String metadataContents = readFile(metadataFile);
            readMetadata = new JSONObject(metadataContents);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't read metadata file: " + e);
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse JSON for metadata file: " + e);
        }

        return readMetadata;
    }

    private void saveMetadata() throws IOException {
        File target = new File(exhibitFolder, METADATA_FILE_NAME);
        writeToFile(target, this.metadata.toString());
    }

    public void configureForAdditionalBeacon(Beacon newBeacon) {
        long beaconId = newBeacon.id;

        File beaconContentFolder = new File(exhibitFolder, String.valueOf(beaconId));
        if (beaconContentFolder.exists())
            throw new IllegalArgumentException("Beacon content folder already setup");
        contentFolderForBeacon.put(newBeacon, beaconContentFolder);

        try {
            JSONObject beacon = new JSONObject();
            beacon.put("id", beaconId);
            JSONArray contents = new JSONArray();
            beacon.put("contents", contents);

            metadata.getJSONArray("beacons").put(beacon);
            saveMetadata();
        } catch (Exception e) {
            Log.e(TAG, "Error editing JSON: " + e);
        }

        beaconContentFolder.mkdir();
    }

    public void configureForRemovedBeacon(Beacon removedBeacon) {
        File beaconContentFolder = contentFolderForBeacon.get(removedBeacon);
        if (beaconContentFolder == null)
            throw new IllegalArgumentException("No such beacon to delete");

        try {
            JSONArray beacons = metadata.getJSONArray("beacons");
            int targetIndex = -1;

            for(int i = 0; i < beacons.length(); i++) {
                JSONObject currentBeacon = beacons.getJSONObject(i);

                // name is stored in first field
                if(currentBeacon.getLong("id") == removedBeacon.id) {
                    targetIndex = i;
                }
            }

            if(targetIndex != -1) {
                beacons.remove(targetIndex);
                saveMetadata();
            } else {
                throw new IllegalArgumentException("No such beacon found to delete");
            }
        } catch (Exception e) {
            Log.e(TAG, "Removing beacon from metadata failed: " + e);
        }

        deleteDir(beaconContentFolder);
    }

    /**
     * Returns an array of ExhibitContents given the name of a beacon
     * @param beacon
     * @return
     */
    public List<ExhibitContent> getContentForBeacon(Beacon beacon) {
        return contentsForBeacon.get(beacon);
    }

    private static String readFile(File file) throws IOException {
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

    private static void writeToFile(File file, String string) throws IOException {
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file));
        outputStreamWriter.write(string);
        outputStreamWriter.close();
    }

    public void insertContent(Uri uri, Beacon beacon, Context ctx) {
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

        File localCopy = writeToExhibitFolder(displayName, inputStream, beacon, ctx);
        appendContentToMetadata(displayName, beacon);
        contentsForBeacon.get(beacon).add(ExhibitContent.fromFile(localCopy));
    }

    private File writeToExhibitFolder(String filename, InputStream inputStream,
                                      Beacon beacon, Context ctx) {
        File beaconContentFolder = contentFolderForBeacon.get(beacon);
        if (beaconContentFolder == null)
            throw new IllegalArgumentException("No such beacon found");

        File localCopy = new File(beaconContentFolder, filename);

        try {
            FileOutputStream outputStream = new FileOutputStream(localCopy);

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
            return localCopy;
        } catch (IOException e) {
            Log.e(TAG, "Error copying file with name " + filename + ": " + e);
            return null;
        }
    }

    private void appendContentToMetadata(String filename, Beacon beacon) {
        try {
            JSONArray beacons = metadata.getJSONArray("beacons");
            int targetIndex = -1;
            for(int i = 0; i < beacons.length(); i++) {
                // index of beacon-name is 0
                if (beacons.getJSONObject(i).getLong("id") == beacon.id) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1)
                throw new IllegalArgumentException("No such beacon found in metadata");

            JSONArray contents = beacons.getJSONObject(targetIndex).getJSONArray("contents");
            contents.put(filename);
            saveMetadata();
        } catch (Exception e) {
            Log.e(TAG, "modifying metadata failed for content with filename: " + filename);
        }
    }

    public void removeContent(final ExhibitContent content, Beacon beacon) {
        removeContentMetadata(content, beacon);
        contentsForBeacon.get(beacon).remove(content);

        new Thread(new Runnable() {
            @Override
            public void run() {
                File contentFile = content.getContentFile();
                ContentSynchronizer.getInstance().deleteSyncedEquivalent(contentFile);
                contentFile.delete();
            }
        }).start();
    }

    private void removeContentMetadata(ExhibitContent content, Beacon beacon) {
        try {
            JSONArray beaconContents = getBeaconMetadata(beacon).getJSONArray("contents");
            for (int i = 0; i < beaconContents.length(); i++) {
                String contentName = beaconContents.getString(i);
                if (contentName.equals(content.getContentName())) {
                    beaconContents.remove(i);
                    saveMetadata();
                    return;
                }
            }
        } catch(JSONException e) {
            Log.e(TAG, "Trouble removing content from metadata");
        } catch(IOException e) {
            Log.e(TAG, "Error updating metadata file while removing content");
        }

        throw new IllegalArgumentException("No such contents found");
    }

    private JSONObject getBeaconMetadata(Beacon beacon) {
        try  {
            JSONArray beacons = metadata.getJSONArray("beacons");
            for(int i = 0; i < beacons.length(); i++) {
                JSONObject currentBeacon = beacons.getJSONObject(i);
                if(currentBeacon.getLong("id") == beacon.id) {
                    return currentBeacon;
                }
            }

            throw new IllegalArgumentException("No such beacon found");
        } catch (JSONException e) {
            Log.e(TAG, "error getting beacon metadata for beacon " + beacon.friendlyName);
            return null;
        }
    }
}
