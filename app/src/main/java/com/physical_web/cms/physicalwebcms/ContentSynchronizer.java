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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
    private File localFolderBeingSynced;
    private DriveFolder remoteFolderBeingSynced;

    private File fileBeingUploaded;
    private File fileBeingDownloaded;
    private File folderBeingDownloaded;
    private File folderBeingUploaded;

    public ContentSynchronizer(Context ctx, File internalStorage) {
        context = ctx;
        localStorageFolder = internalStorage;

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
                    notifyAllListenersAppFolder(appFolderIsEmpty);
                    result.release();
                }
            };

    @Override
    public void onConnectionSuspended(int status) {
    }

    public void syncFolders(File localFolder, DriveFolder remoteFolder) {
        localFolderBeingSynced = localFolder;
        remoteFolderBeingSynced = remoteFolder;
        remoteFolder.listChildren(apiClient).setResultCallback(folderSyncCallback);
    }

    private ResultCallback<DriveApi.MetadataBufferResult> folderSyncCallback =
            new ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(@NonNull DriveApi.MetadataBufferResult metadataBufferResult) {
                    MetadataBuffer remoteFiles = metadataBufferResult.getMetadataBuffer();
                    File[] localFiles = localFolderBeingSynced.listFiles();

                    try {
                        for (File file : localFiles) {
                            if (file.isFile()) {
                                if (driveFolderContainsFile(file, remoteFiles)) {
                                    // TODO conflict resolution
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
                                    uploadFolderToDriveFolder(file, remoteFolderBeingSynced);
                                }
                            }
                        }

                        for (Metadata remoteFile : remoteFiles) {
                            if (!remoteFile.isFolder()) {
                                if (localFolderContainsDriveFile(remoteFile,
                                        localFolderBeingSynced)) {
                                    // TODO conflict resolution
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
                    }
                }
            };

    private void downloadFolderFromDriveFolder(Metadata remoteFolder, File localFolderBeingSynced) {
        String folderName = remoteFolder.getTitle();
        DriveFolder driveFolder = remoteFolder.getDriveId().asDriveFolder();

        folderBeingDownloaded = new File(localFolderBeingSynced, folderName);
        folderBeingDownloaded.mkdir();

        driveFolder.listChildren(apiClient)
                .setResultCallback(downloadFolderFromDriveFolderCallback);
    }

    private ResultCallback<DriveApi.MetadataBufferResult> downloadFolderFromDriveFolderCallback =
            new ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(@NonNull DriveApi.MetadataBufferResult metadataBufferResult) {
                    MetadataBuffer results = metadataBufferResult.getMetadataBuffer();
                    for(Metadata result : results) {
                        if (!result.isFolder()) {
                            downloadFileFromDriveFolder(result, folderBeingDownloaded);
                        } else {
                            downloadFolderFromDriveFolder(result, folderBeingDownloaded);
                        }
                    }
                    results.release();
                }
            };

    private void uploadFolderToDriveFolder(File folder, DriveFolder remoteFolderBeingSynced) {
        folderBeingUploaded = folder;
        String folderName = folder.getName();
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(folderName).build();
        remoteFolderBeingSynced.createFolder(apiClient, changeSet)
                .setResultCallback(folderCreatedCallback);
    }

    ResultCallback<DriveFolder.DriveFolderResult> folderCreatedCallback = new
            ResultCallback<DriveFolder.DriveFolderResult>() {
                @Override
                public void onResult(DriveFolder.DriveFolderResult result) {
                    for(File file : folderBeingUploaded.listFiles()) {
                        if(file.isFile()) {
                            uploadFileToDriveFolder(file, result.getDriveFolder());
                        } else {
                            uploadFolderToDriveFolder(file, result.getDriveFolder());
                        }
                    }
                }
            };

    private void downloadFileFromDriveFolder(Metadata remoteFile, File localFolderBeingSynced) {
        DriveFile fileToDownload = remoteFile.getDriveId().asDriveFile();
        String filetoDownloadName = getDriveFileName(remoteFile);

        fileBeingDownloaded = new File (localFolderBeingSynced, filetoDownloadName);
        try {
            fileBeingDownloaded.createNewFile();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        fileToDownload.open(apiClient, DriveFile.MODE_READ_ONLY, null).
                setResultCallback(downloadFileFromDriveFolderCallback);
    }

    ResultCallback<DriveApi.DriveContentsResult> downloadFileFromDriveFolderCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult driveContentsResult) {
                    DriveContents contents = driveContentsResult.getDriveContents();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(contents.getInputStream()));

                    try {
                        FileOutputStream outputStream = new FileOutputStream(fileBeingDownloaded);
                        int lf;
                        while((lf = reader.read()) != -1) {
                            outputStream.write(lf);
                        }

                        reader.close();
                        outputStream.close();
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                }
            };

    private void uploadFileToDriveFolder(File file, DriveFolder folder) {
        fileBeingUploaded = file;
        remoteFolderBeingSynced = folder;
        Drive.DriveApi.newDriveContents(apiClient).setResultCallback(uploadFolderToDriveFolder);
    }

    private ResultCallback<DriveApi.DriveContentsResult> uploadFolderToDriveFolder =
            new
            ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult driveContentsResult) {
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

                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                }
            };

    private boolean driveFolderContainsFile(File file, MetadataBuffer remoteFiles) {
        for(Metadata remoteFile : remoteFiles) {
            // TODO check below line
            if(file.getName().equals(getDriveFileName(remoteFile))) {
                return true;
            }
        }
        return false;
    }

    private boolean localFolderContainsDriveFile(Metadata driveFile, File localFolder) {
        for(File file : localFolder.listFiles()) {
            if(file.isFile()) {
                String driveFileName = getDriveFileName(driveFile);
                if (driveFileName.equals(file.getName()))
                    return true;
            }
        }

        return false;
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