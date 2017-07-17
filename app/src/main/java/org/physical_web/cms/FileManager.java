package org.physical_web.cms;

import android.content.Context;

import org.physical_web.cms.exhibits.Exhibit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private static final String BEACON_FOLDER_NAME = "beacons";
    private static final String EXHIBIT_FOLDER_NAME = "exhibits";

    private File internalStorage;
    private File exhibitFolder;

    public FileManager(Context context) {
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
    }

    public void removeExhibit(Exhibit exhibit) {
        String exhibitName = exhibit.getTitle();
        File folderToRemove = new File(exhibitFolder, exhibitName);

        if(folderToRemove.exists())
            deleteDir(folderToRemove);
        else
            throw new IllegalArgumentException("Exhibit provided doesn't exist");
    }

    // recursively delete folders
    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }

        Boolean deletionSuccess = file.delete();
        if(!deletionSuccess)
            throw new RuntimeException("Couldn't delete folder");
    }
}
