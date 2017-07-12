package org.physical_web.cms.exhibits;

/**
 * Abstract class representing the types of content that can be stored in an exhibit
 */
public abstract class ExhibitContent {
    private String contentName;

    public void setContentName(String contentName){
        this.contentName = contentName;
    };
}
