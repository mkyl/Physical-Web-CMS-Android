package com.physical_web.cms.physicalwebcms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.FileObserver;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.RecursiveFileObserver;

/**
 * Handles syncing data about the beacons and exhibits with the user's
 * Google Drive.
 *
 * Call {@link ContentSynchronizer#connectReceiver} in the parent Activity onResume() and
 * {@link ContentSynchronizer#disconnectReceiver()} in its onPause()
 */
public class ContentSynchronizer implements GoogleApiClient.ConnectionCallbacks,
        RecursiveFileObserver.FolderListener {
    public static final int SYNC_COMPLETE = 0;
    public static final int SYNC_IN_PROGRESS = 1;
    public static final int NO_SYNC_NETWORK_DOWN = 2;
    public static final int NO_SYNC_DRIVE_ERROR = 3;

    public static final String TAG = ContentSynchronizer.class.getSimpleName();

    private Context context;

    private BroadcastReceiver networkStateReceiver;

    private GoogleApiClient apiClient;
    private RecursiveFileObserver folderObserver;
    private Boolean syncInProgress;
    private List<SyncStatusListener> syncStatusListeners;
    private File localStorageFolder;

    private Map<ResultCallback, File> callbackTracker;
    private Map<ResultCallback, DriveFolder> remoteCallbackTracker;

    public ContentSynchronizer(Context ctx, File internalStorage) {
        context = ctx;
        localStorageFolder = internalStorage;
        callbackTracker = new HashMap<>();
        remoteCallbackTracker = new HashMap<>();

        setupNetworkHandling();
        setupDriveSync(internalStorage);
        kickStartSync();
    }

    public void registerSyncStatusListener(SyncStatusListener listener) {
        if (syncStatusListeners == null)
            syncStatusListeners = new ArrayList<>();

        syncStatusListeners.add(listener);
    }

    private void setupNetworkHandling() {
        networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityManager manager = (ConnectivityManager) context.
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = manager.getActiveNetworkInfo();
                handleNetworkChange(ni);
            }
        };
    }

    /**
     * Registers a receiver for network change activities
     */
    public void connectReceiver() {
        context.registerReceiver(networkStateReceiver,
                new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    /**
     * Disconnects the receiver for network changes
     */
    public void disconnectReceiver() {
        context.unregisterReceiver(networkStateReceiver);
    }

    // called when network status changes
    private void handleNetworkChange(NetworkInfo info) {
        int updatedStatus;

        if (info.isConnected()) {
            resumeSynchronization();
            updatedStatus = SYNC_IN_PROGRESS;
        } else {
            pauseSynchronization();
            updatedStatus = NO_SYNC_NETWORK_DOWN;
        }

        notifyAllSyncListeners(updatedStatus);
    }

    private void setupDriveSync(File internalStorage) {
        apiClient = new GoogleApiClient.Builder(context)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addConnectionCallbacks(this)
                .build();

        folderObserver = new RecursiveFileObserver(internalStorage.getAbsolutePath(),
                FileObserver.MODIFY, this);
        resumeSynchronization();
    }

    @Override
    public void onFolderEvent(int event, File file) {
        if(event == FileObserver.MODIFY)
            syncNeeded();
    }

    /**
     * Force sync status update by simulating network change
     */
    private void kickStartSync() {
        ConnectivityManager manager = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = manager.getActiveNetworkInfo();
        handleNetworkChange(ni);
    }

    private void resumeSynchronization() {
        folderObserver.startWatching();
        // check if drive contents match
        syncNeeded();
    }

    private void pauseSynchronization() {
        folderObserver.stopWatching();
    }

    private void syncNeeded() {
        apiClient.connect();
    }

    @Override
    public void onConnected(Bundle b) {
        notifyAllSyncListeners(SYNC_COMPLETE);

        DriveFolder appFolder = Drive.DriveApi.getAppFolder(apiClient);
        appFolder.listChildren(apiClient).setResultCallback(childrenRetreievedCallback);

        syncFolders(localStorageFolder, appFolder);
    }

    private ResultCallback<DriveApi.MetadataBufferResult> childrenRetreievedCallback =
            new ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(@NonNull DriveApi.MetadataBufferResult metadataBufferResult) {
                    MetadataBuffer result = metadataBufferResult.getMetadataBuffer();
                    Boolean appFolderIsEmpty = !result.iterator().hasNext();
                    Log.d(TAG, "Drive folder is empty: " + appFolderIsEmpty);
                    for(Metadata md : result) {
                        Log.d(TAG, "Found in drive folder: " + md.getTitle());
                        md.getDriveId().asDriveFolder()
                                .listChildren(apiClient).setResultCallback(childrenRetreievedCallback);
                    }
                    notifyAllListenersAppFolder(appFolderIsEmpty);
                    result.release();
                }
            };

    @Override
    public void onConnectionSuspended(int status) {
    }

    public void syncFolders(File localFolder, DriveFolder remoteFolder) {
        ResultCallback<DriveApi.MetadataBufferResult> callback = folderSyncCallback();
        callbackTracker.put(callback, localFolder);
        remoteCallbackTracker.put(callback, remoteFolder);
        remoteFolder.listChildren(apiClient).setResultCallback(callback);
    }

    private ResultCallback<DriveApi.MetadataBufferResult> folderSyncCallback() {
        return new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(@NonNull DriveApi.MetadataBufferResult metadataBufferResult) {
                File localFolderBeingSynced = callbackTracker.get(this);
                DriveFolder remoteFolderBeingSynced = remoteCallbackTracker.get(this);

                MetadataBuffer remoteFiles = metadataBufferResult.getMetadataBuffer();
                File[] localFiles = localFolderBeingSynced.listFiles();

                try {
                    for (File file : localFiles) {
                        if (file.isFile()) {
                            Metadata remoteCopy = driveFolderContainsFile(file, remoteFiles);
                            if (remoteCopy != null) {
                                // TODO conflict resolution
                                resolveConflict(file, remoteCopy);
                            } else {
                                // file in local, but not in remote
                                uploadFileToDriveFolder(file, remoteFolderBeingSynced);
                            }
                        } else {
                            Metadata remoteCopy =
                                    driveFolderContainsLocalFolder(file, remoteFiles);
                            if (remoteCopy != null) {
                                DriveFolder remoteFolder = remoteCopy.getDriveId()
                                        .asDriveFolder();
                                syncFolders(file, remoteFolder);
                            } else {
                                uploadFolderToDriveFolderCallback(file, remoteFolderBeingSynced);
                            }
                        }
                    }

                    for (Metadata remoteFile : remoteFiles) {
                        if (!remoteFile.isFolder()) {
                            File localCopy = localFolderContainsDriveFile(remoteFile,
                                    localFolderBeingSynced);
                            if (localCopy != null) {
                                // TODO conflict resolution
                                resolveConflict(localCopy, remoteFile);
                            } else {
                                downloadFileFromDriveFolder(remoteFile, localFolderBeingSynced);
                            }
                        } else {
                            File localCopy = localFolderContainsDriveFolder(remoteFile,
                                    localFolderBeingSynced);
                            if (localCopy != null) {
                                DriveFolder remoteFolder = remoteFile.getDriveId()
                                        .asDriveFolder();
                                syncFolders(localCopy, remoteFolder);
                            } else {
                                downloadFolderFromDriveFolder(remoteFile,
                                        localFolderBeingSynced);
                            }
                        }
                    }

                    notifyAllSyncListeners(SYNC_COMPLETE);
                } catch (Exception e) {
                    notifyAllSyncListeners(NO_SYNC_DRIVE_ERROR);
                } finally {
                    remoteFiles.release();
                    callbackTracker.remove(this);
                    remoteCallbackTracker.remove(this);
                }
            }
        };
    }

    private void resolveConflict(File localCopy, Metadata remoteCopy) {
        Date localModified = new Date(localCopy.lastModified());
        Date remoteModified = remoteCopy.getModifiedDate();

        if(localModified.after(remoteModified)) {
            // overwrite remote with local
        } else if (remoteModified.after(localModified)){
            // overwrite local with remote
        } else {
            // already synced do nothing
        }
    }

    private void downloadFolderFromDriveFolder(Metadata remoteFolder, File localFolderBeingSynced) {
        String folderName = remoteFolder.getTitle();
        DriveFolder driveFolder = remoteFolder.getDriveId().asDriveFolder();

        File folderBeingDownloaded = new File(localFolderBeingSynced, folderName);
        folderBeingDownloaded.mkdir();

        ResultCallback<DriveApi.MetadataBufferResult> callback =
                downloadFolderFromDriveFolderCallback();

        callbackTracker.put(callback, folderBeingDownloaded);

        driveFolder.listChildren(apiClient)
                .setResultCallback(callback);
    }

    private ResultCallback<DriveApi.MetadataBufferResult> downloadFolderFromDriveFolderCallback() {
        return new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(@NonNull DriveApi.MetadataBufferResult metadataBufferResult) {
                File folderBeingDownloaded = callbackTracker.get(this);
                MetadataBuffer results = metadataBufferResult.getMetadataBuffer();
                for (Metadata result : results) {
                    if (!result.isFolder()) {
                        downloadFileFromDriveFolder(result, folderBeingDownloaded);
                    } else {
                        downloadFolderFromDriveFolder(result, folderBeingDownloaded);
                    }
                }
                results.release();
                callbackTracker.remove(this);
            }
        };
    }

    private void uploadFolderToDriveFolderCallback(File folder, DriveFolder remoteFolderBeingSynced) {
        ResultCallback<DriveFolder.DriveFolderResult> callback = folderCreatedCallback();
        callbackTracker.put(callback, folder);
        String folderName = folder.getName();
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(folderName).build();
        Log.d(TAG, "Uploading folder with name: " + folderName);
        remoteFolderBeingSynced.createFolder(apiClient, changeSet)
                .setResultCallback(callback);
    }

    private ResultCallback<DriveFolder.DriveFolderResult> folderCreatedCallback() {
        return new ResultCallback<DriveFolder.DriveFolderResult>() {
                    @Override
                    public void onResult(DriveFolder.DriveFolderResult result) {
                        File folderBeingUploaded = callbackTracker.get(this);
                        for (File file : folderBeingUploaded.listFiles()) {
                            if (file.isFile()) {
                                uploadFileToDriveFolder(file, result.getDriveFolder());
                            } else {
                                uploadFolderToDriveFolderCallback(file, result.getDriveFolder());
                            }
                        }

                        callbackTracker.remove(this);
                    }
                };
    }

    private void downloadFileFromDriveFolder(Metadata remoteFile, File localFolderBeingSynced) {
        DriveFile fileToDownload = remoteFile.getDriveId().asDriveFile();
        String filetoDownloadName = getDriveFileName(remoteFile);

        ResultCallback<DriveApi.DriveContentsResult> callback =
                downloadFileFromDriveFolderCallback();

        File fileBeingDownloaded = new File (localFolderBeingSynced, filetoDownloadName);
        try {
            fileBeingDownloaded.createNewFile();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        callbackTracker.put(callback, fileBeingDownloaded);
        fileToDownload.open(apiClient, DriveFile.MODE_READ_ONLY, null).
                setResultCallback(callback);
    }

    private ResultCallback<DriveApi.DriveContentsResult> downloadFileFromDriveFolderCallback() {
        return new ResultCallback<DriveApi.DriveContentsResult>() {
            @Override
            public void onResult(@NonNull DriveApi.DriveContentsResult driveContentsResult) {
                File fileBeingDownloaded = callbackTracker.get(this);
                DriveContents contents = driveContentsResult.getDriveContents();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(contents.getInputStream()));

                try {
                    FileOutputStream outputStream = new FileOutputStream(fileBeingDownloaded);
                    int lf;
                    while ((lf = reader.read()) != -1) {
                        outputStream.write(lf);
                    }

                    reader.close();
                    outputStream.close();
                    callbackTracker.remove(this);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        };
    }

    private void uploadFileToDriveFolder(File file, DriveFolder folder) {
        ResultCallback<DriveApi.DriveContentsResult> callback = uploadFolderToDriveFolderCallback();
        callbackTracker.put(callback, file);
        remoteCallbackTracker.put(callback, folder);
        Drive.DriveApi.newDriveContents(apiClient).setResultCallback(callback);
    }

    private ResultCallback<DriveApi.DriveContentsResult> uploadFolderToDriveFolderCallback() {
        return new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.DriveContentsResult driveContentsResult) {
                        File fileBeingUploaded = callbackTracker.get(this);
                        DriveFolder remoteFolderBeingSynced = remoteCallbackTracker.get(this);
                        DriveContents driveContents = driveContentsResult.getDriveContents();

                        try {
                            InputStream inputStream = new FileInputStream(fileBeingUploaded);
                            OutputStream outputStream = driveContents.getOutputStream();

                            byte[] buffer = new byte[1024 * 1024];
                            int len;
                            while ((len = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, len);
                            }

                            MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                    .setTitle(fileBeingUploaded.getName())
                                    .build();

                            remoteFolderBeingSynced.createFile(apiClient, metadataChangeSet,
                                    driveContents);

                            inputStream.close();
                            outputStream.close();

                            callbackTracker.remove(this);
                            remoteCallbackTracker.remove(this);

                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                };
    }

    private Metadata driveFolderContainsFile(File file, MetadataBuffer remoteFiles) {
        for(Metadata remoteFile : remoteFiles) {
            // TODO check below line
            if(file.getName().equals(getDriveFileName(remoteFile))) {
                return remoteFile;
            }
        }
        return null;
    }

    private File localFolderContainsDriveFile(Metadata driveFile, File localFolder) {
        for(File file : localFolder.listFiles()) {
            if(file.isFile()) {
                String driveFileName = getDriveFileName(driveFile);
                if (driveFileName.equals(file.getName()))
                    return file;
            }
        }

        return null;
    }

    private Metadata driveFolderContainsLocalFolder(File localFolder, MetadataBuffer driveFolders) {
        for(Metadata driveFolder : driveFolders) {
            if(driveFolder.getTitle().equals(localFolder.getName())) {
                return driveFolder;
            }
        }
        return null;
    }

    private File localFolderContainsDriveFolder(Metadata driveFolder, File localFolder) {
        for(File folder : localFolder.listFiles()) {
            if(!folder.isFile()) {
                if(folder.getName().equals(driveFolder.getTitle()))
                    return folder;
            }
        }
        return null;
    }

    private String getDriveFileName(Metadata file) {
        String result = file.getTitle();
        if(!file.getFileExtension().equals(""))
            result += "." + file.getFileExtension();
        return result;
    }

    private void notifyAllSyncListeners(int status) {
        if(syncStatusListeners != null) {
            for (SyncStatusListener listener : syncStatusListeners) {
                listener.syncStatusChanged(status);
            }
        }
    }

    private void notifyAllListenersAppFolder(Boolean result) {
        if(syncStatusListeners != null) {
            for (SyncStatusListener listener : syncStatusListeners) {
                listener.driveFolderIsEmpty(result);
            }
        }
    }
}

interface SyncStatusListener {
    void syncStatusChanged(int status);
    void driveFolderIsEmpty(Boolean result);
}