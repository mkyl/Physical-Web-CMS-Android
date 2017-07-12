package org.physical_web.cms;

import android.content.Context;

import java.io.File;

public class FileManager {
    private static final String BEACON_FOLDER = "beacons";
    private static final String EXHIBIT_FOLDER = "exhibits";

    private File internalStorage;

    public FileManager(Context context) {
        internalStorage = context.getFilesDir();
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
}
