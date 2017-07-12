package org.physical_web.cms.exhibits;

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
}
