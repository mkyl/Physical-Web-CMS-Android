package com.physical_web.cms.physicalwebcms;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
