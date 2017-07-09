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

    public void initializeFolders() {
        createFolderIfNotExists(BEACON_FOLDER);
        createFolderIfNotExists(EXHIBIT_FOLDER);
    }

    private void createFolderIfNotExists(String name) {
        File folder = new File(internalStorage + File.separator +
                name + File.separator);
        if (!folder.exists())
            folder.mkdir();
    }

    public void createDemoFile() {
        try {
            File demoFile = new File(internalStorage, "demo");
            if(!demoFile.exists()) {
                Log.d("FileManager", "Creating dummy file");
                demoFile.createNewFile();
                FileWriter writer = new FileWriter(demoFile);
                for (int length = 0; length <= 1e+7; length += 39) {
                    writer.write("abcdefghijkl");
                    writer.write("\n");
                    writer.write("abcdefghijkl");
                    writer.write("\n");
                    writer.write("abcdefghijkl");
                    writer.write("\n");
                }
                writer.flush();
                writer.close();
            }
        } catch (Exception e) {}
    }

    public void deleteDemoFile() {
        File demoFile = new File(internalStorage, "demo");
        if(demoFile.exists()) {
            demoFile.delete();
        }
    }

    public File getRootFolder() {
        return internalStorage;
    }
}
