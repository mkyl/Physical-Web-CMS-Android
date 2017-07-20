package util;

import java.io.File;

public class MiscFile {
    // recursively delete folders
    public static void deleteDir(File file) {
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
