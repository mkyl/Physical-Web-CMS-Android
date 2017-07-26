package org.physical_web.cms.exhibits;

import android.content.Context;
import android.util.Log;

import org.physical_web.cms.beacons.Beacon;
import org.physical_web.cms.sync.ContentSynchronizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    public synchronized void setContext(Context context) {
        if (exhibitsFolder == null) {
            exhibitsFolder = new File(context.getFilesDir(), EXHIBIT_FOLDER_NAME);

            if(!exhibitsFolder.exists())
                exhibitsFolder.mkdir();

            exhibits = loadExhibitsFromDisk();
        }
    }

    private List<Exhibit> loadExhibitsFromDisk() {
        List<Exhibit> result = new ArrayList<>();

        for(File child : exhibitsFolder.listFiles()) {
            if (!child.isFile()) {
                Exhibit foundExhibit = Exhibit.loadFromFolder(child);
                result.add(foundExhibit);
            }
        }

        return result;
    }

    public Exhibit getExhibit(int position) {
        if(position > getExhibitCount())
            throw new ArrayIndexOutOfBoundsException();
        else
            return exhibits.get(position);
    }

    public Exhibit getById(long id) {
        for(Exhibit searchSubject : exhibits) {
            if (searchSubject.getId() == id)
                return searchSubject;
        }
        throw new IllegalArgumentException("No such exhibit exists");
    }

    public void createNewExhibit(String exhibitName) {
        Exhibit createdExhibit = Exhibit.initializeIntoFolder(exhibitName, exhibitsFolder);
        exhibits.add(createdExhibit);
    }

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

    public void configureNewBeacon(Beacon beacon) {
        for(Exhibit exhibit : exhibits) {
            exhibit.configureForAdditionalBeacon(beacon);
        }
    }

    public void configureRemovedBeacon(Beacon beacon) {
        for(Exhibit exhibit : exhibits) {
            exhibit.configureForRemovedBeacon(beacon);
        }
    }

    public int getExhibitCount() {
        return exhibits.size();
    }
}
