package org.physical_web.cms.exhibits;

/**
 * Abstract class representing the types of content that can be stored in an exhibit
 */
public abstract class ExhibitContent {
    private String contentName;

    public void setContentName(String contentName){
        this.contentName = contentName;
    };

    public abstract String toHTML();
}

class TextContent extends ExhibitContent {
}

class SoundContent extends ExhibitContent {
}

class ImageContent extends ExhibitContent {
}

class VideoContent extends ExhibitContent {
}
