package com.physical_web.cms.physicalwebcms;

public interface SyncStatusListener {
    void syncStatusChanged(int status);
    void driveFolderIsEmpty(Boolean result);
}
