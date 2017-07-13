package org.physical_web.cms.exhibits;

import java.io.File;

/**
 * Abstract class representing the types of content that can be stored in an exhibit
 */
public abstract class ExhibitContent {
    private String contentName;

    public static ExhibitContent fromRawFile(File contentFile) {
        // TODO figure out what type of file it is
        return new TextContent();
    }

    public void setContentName(String contentName){
        this.contentName = contentName;
    };

    public abstract String toHTML();

}

class TextContent extends ExhibitContent {
    @Override
    public String toHTML() {
        return null;
    }
}

class SoundContent extends ExhibitContent {
    @Override
    public String toHTML() {
        return null;
    }
}

class ImageContent extends ExhibitContent {
    @Override
    public String toHTML() {
        return null;
    }
}

class VideoContent extends ExhibitContent {
    @Override
    public String toHTML() {
        return null;
    }
}
