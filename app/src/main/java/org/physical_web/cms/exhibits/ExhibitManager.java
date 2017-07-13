package org.physical_web.cms.exhibits;

import android.content.Context;

import org.physical_web.cms.FileManager;

import java.util.List;

public class ExhibitManager {
    private List<Exhibit> exhibits;
    private FileManager exhibitFileManager;

    public ExhibitManager(Context context) {
        exhibitFileManager = new FileManager(context);
        refresh();
    }

    public Exhibit getExhibit(int position) {
        if(position > getExhibitCount())
            throw new ArrayIndexOutOfBoundsException();
        else
            return exhibits.get(position);
    }

    public void refresh() {
        exhibits = exhibitFileManager.loadExhibitsFromDisk();
    }

    public void insertExhibit(Exhibit exhibit) {
        exhibitFileManager.writeNewExhibit(exhibit);
    }

    public int getExhibitCount() {
        return exhibits.size();
    }

}
