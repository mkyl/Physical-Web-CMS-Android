package org.physical_web.cms.exhibits;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents an exhibition, or a set of content assigned to a number of beacons. These sets can
 * be composed, deployed and swapped from the app.
 */
public class Exhibit {
    private String title;
    private Boolean active;
    private List<ExhibitContent> exhibitContents;

    public Exhibit(String title) {
        this.title = title;
        this.active = false;
        this.exhibitContents = new LinkedList<>();
    }

    public static Exhibit fromFolder(File folder) {
        String name = folder.getName();
        Exhibit result = new Exhibit(name);
        result.active = false;
        result.loadExhibitContentsFromFolder(folder);
        return result;
    }

    public String getTitle() {
        return this.title;
    }

    public void addContent(ExhibitContent content) {
        this.exhibitContents.add(content);
    }

    public void removeContent(ExhibitContent content) {
        if (this.exhibitContents.contains(content))
            this.exhibitContents.remove(content);
        else
            throw new IllegalArgumentException("Content not found in exhibit");
    }

    public void makeActive() {
        this.active = true;
        // TODO complete method
    }

    private List<ExhibitContent> loadExhibitContentsFromFolder(File exhibitFolder) {
        if(exhibitFolder.isFile())
            throw new IllegalArgumentException("Passed file, not folder");

        List<ExhibitContent> result = new LinkedList<>();

        for(File child : exhibitFolder.listFiles()) {
            if (!child.isFile())
                result.add(ExhibitContent.fromRawFile(child));
        }

        return result;
    }
}
