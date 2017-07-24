package org.physical_web.cms;

import android.content.Context;

import org.physical_web.cms.beacons.Beacon;
import org.physical_web.cms.beacons.BeaconDatabase;
import org.physical_web.cms.exhibits.Exhibit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static util.MiscFile.deleteDir;

public class FileManager {
    private static final String TAG = FileManager.class.getSimpleName();
    private static final String BEACON_FOLDER_NAME = "beacons";
    public static final String EXHIBIT_FOLDER_NAME = "exhibits";
    private final static int BUFFER_SIZE = 8 * 1024;

    private File internalStorage;
    private File exhibitFolder;
    private Context context;

    public FileManager(Context context) {
        this.context = context;
        internalStorage = context.getFilesDir();

        createFolderIfNotExists(EXHIBIT_FOLDER_NAME);
        exhibitFolder = new File(internalStorage, EXHIBIT_FOLDER_NAME);
    }

    private void createFolderIfNotExists(String name) {
        File folder = new File(internalStorage + File.separator +
                name + File.separator);
        if (!folder.exists())
            folder.mkdir();
    }

    public File getRootFolder() {
        return internalStorage;
    }

    public List<Exhibit> loadExhibitsFromDisk() {
        List<Exhibit> result = new ArrayList<>();

        for(File child : exhibitFolder.listFiles()) {
            if (!child.isFile()) {
                Exhibit foundExhibit = Exhibit.fromFolder(child);
                result.add(foundExhibit);
            }
        }

        return result;
    }

    public void writeNewExhibit(Exhibit exhibit) {
        String exhibitName = exhibit.getTitle();
        File newFolder = new File(exhibitFolder, exhibitName);
        newFolder.mkdir();
        createContentFolders(newFolder, exhibit);
    }

    public void createContentFolders(final File folder, final Exhibit exhibit) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Beacon> beacons = BeaconDatabase.getDatabase(context).beaconDao().getAllBeacons();
                for(Beacon beacon : beacons) {
                    File beaconFolder = new File(folder, beacon.friendlyName);
                    if (!beaconFolder.exists()) {
                        beaconFolder.mkdir();
                        exhibit.loadBeaconsIntoMetadata();
                    }
                    else
                        throw new IllegalStateException("Beacon folder already exists");
                }
            }
        }).start();
    }

    public void removeExhibit(Exhibit exhibit) {
        String exhibitName = exhibit.getTitle();
        File folderToRemove = new File(exhibitFolder, exhibitName);

        if(folderToRemove.exists())
            deleteDir(folderToRemove);
        else
            throw new IllegalArgumentException("Exhibit provided doesn't exist");
    }
}
