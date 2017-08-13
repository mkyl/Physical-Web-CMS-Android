package org.physical_web.cms.exhibits;

import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.physical_web.cms.beacons.Beacon;
import org.physical_web.cms.sync.ContentSynchronizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import util.MiscFile;

/**
 * Manages the {@link Exhibit}s stored by the app. Get a copy with a call to {@link #getInstance()}.
 * The first time this class is called, {@link #setContext(Context)} *must* be also called.
 */
public class ExhibitManager {
    private static final ExhibitManager INSTANCE = new ExhibitManager();

    private static final String TAG = ExhibitManager.class.getSimpleName();
    public static final String EXHIBIT_FOLDER_NAME = "exhibits";

    private ContentSynchronizer contentSynchronizer;
    private List<Exhibit> exhibits;

    private File exhibitsFolder = null;

    private ExhibitManager() {
        contentSynchronizer = ContentSynchronizer.getInstance();
    }

    public static ExhibitManager getInstance() {
        return INSTANCE;
    }

    /**
     * MUST BE CALLED AT LEAST ONCE before any other methods are called. Sets context this class
     * will operate in.
     *
     * @param context Context to work in
     */
    public synchronized void setContext(Context context) {
        if (exhibitsFolder == null) {
            exhibitsFolder = new File(context.getFilesDir(), EXHIBIT_FOLDER_NAME);

            if (!exhibitsFolder.exists())
                exhibitsFolder.mkdir();

            exhibits = loadExhibitsFromDisk();
        }
    }

    // returns list of exhibits, as they are loaded storage on disk
    private List<Exhibit> loadExhibitsFromDisk() {
        List<Exhibit> result = new ArrayList<>();

        for (File child : exhibitsFolder.listFiles()) {
            if (!child.isFile()) {
                Exhibit foundExhibit = Exhibit.loadFromFolder(child);
                result.add(foundExhibit);
            }
        }

        return result;
    }

    public Exhibit getActiveExhibit() {
        try {
            File metadataFile = new File(exhibitsFolder, "metadata.json");
            JSONObject metadata = new JSONObject(MiscFile.readFile(metadataFile));
            return this.getById(metadata.getLong("active-exhibit"));
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Couldn't get active exhibit: " + e);
            return null;
        }
    }

    public File getExhibitsFolder() {
        return exhibitsFolder;
    }

    public void setActiveExhibit(Exhibit activeExhibit) {
        try {
            File metadataFile = new File(exhibitsFolder, "metadata.json");
            if (!metadataFile.exists()) {
                metadataFile.createNewFile();
                MiscFile.writeToFile(metadataFile, "{}");
            }
            JSONObject metadata = new JSONObject(MiscFile.readFile(metadataFile));
            metadata.put("active-exhibit", activeExhibit.getId());
            MiscFile.writeToFile(metadataFile, metadata.toString());
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Couldn't set active exhibit: " + e);
        }
    }

    /**
     * Get an {@link Exhibit} by providing its index.
     *
     * @param position index of exhibit, range of 0..{@link #getExhibitCount()}-1
     * @return exhibit referred to
     */
    public Exhibit getExhibit(int position) {
        if (position > getExhibitCount() || position < 0)
            throw new ArrayIndexOutOfBoundsException();
        else
            return exhibits.get(position);
    }

    /**
     * get an {@link Exhibit} by referring to its unique id
     *
     * @param id unique id of the exhibit
     * @return exhibit with provided id
     */
    public Exhibit getById(long id) {
        for (Exhibit searchSubject : exhibits) {
            if (searchSubject.getId() == id)
                return searchSubject;
        }
        throw new IllegalArgumentException("No such exhibit exists");
    }

    /**
     * Create a new {@link Exhibit} with the given name, writing it to disk.
     *
     * @param exhibitName name of exhibit
     */
    public void createNewExhibit(String exhibitName) {
        Exhibit createdExhibit = Exhibit.initializeIntoFolder(exhibitName, exhibitsFolder);
        exhibits.add(createdExhibit);
    }

    /**
     * Remove an {@link Exhibit} permanently, deleting it completely.
     *
     * @param exhibit to be deleted
     */
    public void removeExhibit(Exhibit exhibit) {
        final File folderToDelete = exhibit.getExhibitFolder();
        new Thread(new Runnable() {
            @Override
            public void run() {
                contentSynchronizer.deleteSyncedEquivalent(folderToDelete);
            }
        }).start();

        exhibits.remove(exhibit);
        util.MiscFile.deleteDir(exhibit.getExhibitFolder());
    }


    /**
     * inform all exhibits that a new beacon has been added
     *
     * @param beacon new beacon that exhibits must now support
     */
    public void configureNewBeacon(Beacon beacon) {
        for (Exhibit exhibit : exhibits) {
            exhibit.configureForAdditionalBeacon(beacon);
        }
    }

    /**
     * inform all exhibits that a beacon has been removed
     *
     * @param beacon removed beacon that exhibits may no longer support
     */
    public void configureRemovedBeacon(Beacon beacon) {
        for (Exhibit exhibit : exhibits) {
            exhibit.configureForRemovedBeacon(beacon);
        }
    }

    /**
     * returns the number of exhibits loaded by this class
     *
     * @return number of exhibits
     */
    public int getExhibitCount() {
        return exhibits.size();
    }
}
