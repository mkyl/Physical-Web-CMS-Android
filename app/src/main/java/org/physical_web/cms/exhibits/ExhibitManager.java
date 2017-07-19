package org.physical_web.cms.exhibits;

import android.content.Context;

import org.physical_web.cms.FileManager;
import org.physical_web.cms.sync.ContentSynchronizer;

import java.util.List;

public class ExhibitManager {
    private static final ExhibitManager INSTANCE = new ExhibitManager();
    private ContentSynchronizer contentSynchronizer;

    private List<Exhibit> exhibits;
    private FileManager exhibitFileManager = null;

    private ExhibitManager() {
        contentSynchronizer = ContentSynchronizer.getInstance();
    }

    public static ExhibitManager getInstance() {
        return INSTANCE;
    }

    public synchronized void setContext(Context context) {
        if (exhibitFileManager == null) {
                exhibitFileManager = new FileManager(context);
                refresh();
        }
    }

    public Exhibit getExhibit(int position) {
        if(position > getExhibitCount())
            throw new ArrayIndexOutOfBoundsException();
        else
            return exhibits.get(position);
    }

    public Exhibit getByName(String name) {
        for(Exhibit searchSubject : exhibits) {
            if (searchSubject.getTitle().equals(name))
                return searchSubject;
        }
        throw new IllegalArgumentException("No such exhibit exists");
    }

    public void refresh() {
        exhibits = exhibitFileManager.loadExhibitsFromDisk();
    }

    public void insertExhibit(Exhibit exhibit) {
        exhibitFileManager.writeNewExhibit(exhibit);
        this.refresh();
    }

    public void removeExhibit(final Exhibit exhibit) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                contentSynchronizer.removeExhibit(exhibit.getTitle());
            }
        }).start();
        exhibitFileManager.removeExhibit(exhibit);
        this.refresh();
    }

    public int getExhibitCount() {
        return exhibits.size();
    }
}
