package util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.physical_web.cms.beacons.Beacon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Scanner;

public class MiscFile {
    private static final String TAG = MiscFile.class.getSimpleName();
    private static final int BUFFER_SIZE = 8 * 1024;

    /**
     * Overwrite file with string
     * @param file
     * @param string
     * @throws IOException
     */
    public static void writeToFile(File file, String string) throws IOException {
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file));
        outputStreamWriter.write(string);
        outputStreamWriter.close();
    }

    /**
     * Read file into string
     * @param file
     * @return
     * @throws IOException
     */
    public static String readFile(File file) throws IOException {
        StringBuilder fileContents = new StringBuilder((int) file.length());
        Scanner scanner = new Scanner(file);
        String lineSeparator = System.getProperty("line.separator");

        try {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine());
                // avoid extra line separator at end of file
                if (scanner.hasNextLine())
                    fileContents.append(lineSeparator);
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }

    /**
     * recursively delete folders
     */
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

    // copy from input stream to beacon folder
    public static File copyURIContentsToFolder(Uri uri, String filename,
                                      File folder, Context ctx) {
        try {
            InputStream inputStream = ctx.getContentResolver().openInputStream(uri);
            if (inputStream == null)
                throw new IllegalArgumentException("No copyable content at URI provided");

            File localCopy = new File(folder, filename);
            if (!localCopy.exists()) {
                Boolean createdFile = localCopy.createNewFile();
                if (!createdFile)
                    throw new IOException("Couldn't create file");
            }

            FileOutputStream outputStream = new FileOutputStream(localCopy);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
            return localCopy;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File at URI provided not found: " + e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Error copying file with name " + filename + ": " + e);
            return null;
        }
    }
}
