package util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Scanner;

public class MiscFile {
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
}
