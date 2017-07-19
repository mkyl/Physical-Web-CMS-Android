package org.physical_web.cms.exhibits;

import android.webkit.MimeTypeMap;

import java.io.File;

/**
 * Abstract class representing the types of content that can be stored in an exhibit
 */
public abstract class ExhibitContent {
    private final static String MIME_TEXT_PREFIX = "text/";
    private final static String MIME_AUDIO_PREFIX = "audio/";
    private final static String MIME_IMAGE_PREFIX = "image/";
    private final static String MIME_VIDEO_PREFIX = "video/";

    private String contentName;

    public static ExhibitContent fromFile(File contentFile) {
        String mimeType = MimeTypeMap.getFileExtensionFromUrl(contentFile.toURI().toString());

        if (mimeType.startsWith(MIME_TEXT_PREFIX))
            return new TextContent(contentFile);
        else if (mimeType.startsWith(MIME_AUDIO_PREFIX))
            return new SoundContent(contentFile);
        else if (mimeType.startsWith(MIME_IMAGE_PREFIX))
            return new ImageContent(contentFile);
        else if (mimeType.startsWith(MIME_VIDEO_PREFIX))
            return new VideoContent(contentFile);
        else
            throw new IllegalArgumentException("Exhibit content has unrecognized file type");
    }

    public void setContentName(String contentName){
        this.contentName = contentName;
    }

    public abstract String toHTML();
}

class TextContent extends ExhibitContent {
    public TextContent(File contentFile) {}

    @Override
    public String toHTML() {
        return null;
    }
}

class SoundContent extends ExhibitContent {
    public SoundContent(File contentFile) {}

    @Override
    public String toHTML() {
        return null;
    }
}

class ImageContent extends ExhibitContent {
    public ImageContent(File contentFile) {}

    @Override
    public String toHTML() {
        return null;
    }
}

class VideoContent extends ExhibitContent {
    public VideoContent(File contentFile) {}

    @Override
    public String toHTML() {
        return null;
    }
}
