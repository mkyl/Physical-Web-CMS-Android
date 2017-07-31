package org.physical_web.cms.exhibits;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.util.Scanner;

/**
 * Abstract class representing the types of content that can be stored in an exhibit
 */
public abstract class ExhibitContent {
    private final static String TAG = ExhibitContent.class.getSimpleName();

    private final static String MIME_TEXT_PREFIX = "text/";
    private final static String MIME_AUDIO_PREFIX = "audio/";
    private final static String MIME_IMAGE_PREFIX = "image/";
    private final static String MIME_VIDEO_PREFIX = "video/";

    private String contentName;
    private File contentFile;

    public static ExhibitContent fromFile(File contentFile) {
        ExhibitContent result;
        // easy check: look at extension and determine file type
        String mimeType = mimeTypeFromExtension(contentFile);

        // more resource intensive, peek into content if no file extension
        if (mimeType == null)
            mimeType = mimeTypeFromContent(contentFile);

        if (mimeType.startsWith(MIME_TEXT_PREFIX))
            result = new TextContent(contentFile);
        else if (mimeType.startsWith(MIME_AUDIO_PREFIX))
            result = new SoundContent(contentFile);
        else if (mimeType.startsWith(MIME_IMAGE_PREFIX))
            result = new ImageContent(contentFile);
        else if (mimeType.startsWith(MIME_VIDEO_PREFIX))
            result = new VideoContent(contentFile);
        else
            throw new IllegalArgumentException("Exhibit content has unrecognized file type");

        result.setContentName(contentFile.getName());
        result.setContentFile(contentFile);
        return result;
    }

    public String getContentName() {
        return this.contentName;
    }

    public void setContentName(String contentName) {
        this.contentName = contentName;
    }

    public File getContentFile() {
        return this.contentFile;
    }

    private void setContentFile(File file) {
        this.contentFile = file;
    }

    public abstract String toHTML();

    private static String mimeTypeFromExtension(File contentFile) {
        String fileName = contentFile.toURI().toString();
        String extensionFromUrl = MimeTypeMap.getFileExtensionFromUrl(fileName);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extensionFromUrl);
    }

    private static String mimeTypeFromContent(File contentFile) {
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(contentFile));
            String mimeType = URLConnection.guessContentTypeFromStream(is);
            is.close();

            if (mimeType == null)
                throw new UnsupportedEncodingException();

            return mimeType;
        } catch (Exception e) {
            Log.e(TAG, "Failed to determine mime type from content: " + e);
            // "application/octet-stream" AKA it's binary data but I can't tell you much more
            return "application/octet-stream";
        }
    }
}

class TextContent extends ExhibitContent {
    String text;

    public TextContent(File contentFile) {
        try {
            text = readFile(contentFile);
        } catch (Exception e) {
            throw new RuntimeException("couldn't read text");
        }
    }

    public String getText() {
        return text;
    }

    private String readFile(File file) throws IOException {
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

    @Override
    public String toHTML() {
        return null;
    }
}

class SoundContent extends ExhibitContent {
    Uri soundURI;

    public SoundContent(File contentFile) {
        soundURI = Uri.fromFile(contentFile);
    }

    public Uri getURI() {
        return soundURI;
    }

    @Override
    public String toHTML() {
        return null;
    }
}

class ImageContent extends ExhibitContent {
    private Bitmap bitmap;

    public ImageContent(File contentFile) {
    }

    public Bitmap getSampledBitmap(int height, int width) {
        return decodeSampledBitmapFromFile(getContentFile(), width, height);
    }

    // method from https://developer.android.com/topic/performance/graphics/load-bitmap.html
    private static Bitmap decodeSampledBitmapFromFile(File file, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    // method from https://developer.android.com/topic/performance/graphics/load-bitmap.html
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    @Override
    public String toHTML() {
        return null;
    }
}

class VideoContent extends ExhibitContent {
    String videoPath;

    public VideoContent(File contentFile) {
        videoPath = contentFile.getAbsolutePath();
    }

    public String getVideoPath() {
        return videoPath;
    }

    @Override
    public String toHTML() {
        return null;
    }
}
