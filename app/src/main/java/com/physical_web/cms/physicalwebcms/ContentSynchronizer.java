package com.physical_web.cms.physicalwebcms;

import android.app.Activity;
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
import com.google.android.gms.common.api.PendingResult;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

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

    private static final String TAG = ContentSynchronizer.class.getSimpleName();
    private static final int BUFFER_SIZE = 8 * 1024;

    private Activity context;

    private BroadcastReceiver networkStateReceiver;

    private GoogleApiClient apiClient;
    private RecursiveFileObserver folderObserver;
    private Boolean syncRoot;
    private List<SyncStatusListener> syncStatusListeners;
    private File localStorageFolder;

    private Boolean currentlySyncing;

    public ContentSynchronizer(Activity ctx, File internalStorage) {
        context = ctx;
        localStorageFolder = internalStorage;
        currentlySyncing = false;

        setupNetworkHandling();
        setupDriveSync(internalStorage);
    }

    /**
     * Subscribe to notifications about changing sync status.
     * @param listener
     */
    public void registerSyncStatusListener(SyncStatusListener listener) {
        if (syncStatusListeners == null)
            syncStatusListeners = new ArrayList<>();

        syncStatusListeners.add(listener);
    }

    // watch network status to only sync when network is connected.
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
        // if there is no network at all, info will be null
        if (info != null && info.isConnected()) {
            syncNeeded();
            resumeSynchronization();
        } else {
            pauseSynchronization();
            notifyAllSyncListeners(NO_SYNC_NETWORK_DOWN);
        }
    }

    // setup drive API & file observer in preparation for sync
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

    /**
     * Only called by FileObserver to notify us of need to sync
     * @param event
     * @param file
     */
    @Override
    public void onFolderEvent(int event, File file) {
        if(event == FileObserver.MODIFY)
            syncNeeded();
    }

    /**
     * Force sync status update by simulating network change
     */
    public void kickStartSync() {
        ConnectivityManager manager = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = manager.getActiveNetworkInfo();
        handleNetworkChange(ni);
    }

    // start watching for file changes
    private void resumeSynchronization() {
        folderObserver.startWatching();
    }

    // stop watching for file changes
    private void pauseSynchronization() {
        folderObserver.stopWatching();
    }

    // begin sync process
    private void syncNeeded() {
        if(!currentlySyncing) {
            currentlySyncing = true;

            notifyAllSyncListeners(SYNC_IN_PROGRESS);
            apiClient.disconnect();
            apiClient.connect();
        }
    }

    /**
     * Callback for when Drive connection is successful
     * @param b
     */
    @Override
    public void onConnected(Bundle b) {
        final DriveFolder appFolder = Drive.DriveApi.getAppFolder(apiClient);
        appFolder.listChildren(apiClient).setResultCallback(childrenRetreievedCallback);

        Log.d(TAG, "Starting drive synchronization");
        syncRoot = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // this line is required to fix a Google Services bug where remote data isn't
                // visible when app is reinstalled, possibly due to bad cache
                Drive.DriveApi.requestSync(apiClient).await();
                syncFolders(localStorageFolder, appFolder);
            }
        }).start();
    }

    // TODO debugging function remove when done
    // called when listing appfolder is successful
    private ResultCallback<DriveApi.MetadataBufferResult> childrenRetreievedCallback =
            new ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(@NonNull DriveApi.MetadataBufferResult metadataBufferResult) {
                    MetadataBuffer result = metadataBufferResult.getMetadataBuffer();

                    int counter = 0;
                    for(Metadata md : result) {
                        counter++;
                        Log.d(TAG, counter + " - found in drive folder: " + md.getTitle());
                    }

                    result.release();
                }
            };

    @Override
    public void onConnectionSuspended(int status) {
    }

    /**
     * Recursively synchronize a local folder on the filesystem with a remote folder stored on
     * Google Drive. Any files and folders found only locally will be uploaded. Any files and
     * folders only found on remote will be downloaded. Files and folders found in both folders
     * will be over-written by the most recently modified version, unless both have the same
     * modification time, in which case neither will be changed.
     */
    private void syncFolders(File localFolder, DriveFolder remoteFolder) {
        Boolean isRootFolder = syncRoot;
        // root folder is only encountered once
        syncRoot = false;

        PendingResult<DriveApi.MetadataBufferResult> request = remoteFolder.listChildren(apiClient);
        DriveApi.MetadataBufferResult result = request.await();

        File[] localFiles = localFolder.listFiles();
        MetadataBuffer remoteFiles = result.getMetadataBuffer();

        try {
            for (File file : localFiles) {
                notifyAllSyncListeners(SYNC_IN_PROGRESS);
                if (file.isFile()) {
                    Metadata remoteCopy = driveFolderContainsFile(file, remoteFiles);
                    if (remoteCopy != null) {
                        // TODO conflict resolution
                        resolveConflict(file, remoteCopy);
                    } else {
                        // file in local, but not in remote
                        uploadFileToDriveFolder(file, remoteFolder);
                    }
                } else {
                    Metadata remoteCopy =
                            driveFolderContainsLocalFolder(file, remoteFiles);
                    if (remoteCopy != null) {
                        DriveFolder remoteFolder2 = remoteCopy.getDriveId()
                                .asDriveFolder();
                        syncFolders(file, remoteFolder2);
                    } else {
                        uploadFolderToDriveFolder(file, remoteFolder);
                    }
                }
            }

            for (Metadata remoteFile : remoteFiles) {
                notifyAllSyncListeners(SYNC_IN_PROGRESS);
                if (!remoteFile.isFolder()) {
                    File localCopy = localFolderContainsDriveFile(remoteFile, localFolder);
                    if (localCopy != null) {
                        // TODO conflict resolution
                        resolveConflict(localCopy, remoteFile);
                    } else {
                        downloadFileFromDriveFolder(remoteFile, localFolder);
                    }
                } else {
                    File localCopy = localFolderContainsDriveFolder(remoteFile,
                            localFolder);
                    if (localCopy != null) {
                        DriveFolder remoteFolder2 = remoteFile.getDriveId()
                                .asDriveFolder();
                        syncFolders(localCopy, remoteFolder2);
                    } else {
                        downloadFolderFromDriveFolder(remoteFile,
                                localFolder);
                    }
                }
            }

            if(isRootFolder) {
                notifyAllSyncListeners(SYNC_COMPLETE);
                Log.d(TAG, "Drive sync success");
            }
        } catch (Exception e) {
            notifyAllSyncListeners(NO_SYNC_DRIVE_ERROR);
            Log.e(TAG, e.toString());
        } finally {
            remoteFiles.release();
            if(isRootFolder) {
                currentlySyncing = false;
            }
        }
    }

    // resolve conflict when two versions of a file exist, both local and remote. One with most
    // recent modification date is prefered.
    private void resolveConflict(File localCopy, Metadata remoteCopy) {
        Log.d(TAG, "Starting conflict resolution");
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

    // download a drive folder into a local folder
    private void downloadFolderFromDriveFolder(Metadata remoteFolder, File localFolderBeingSynced)
            throws IOException{
        String folderName = remoteFolder.getTitle();
        DriveFolder driveFolder = remoteFolder.getDriveId().asDriveFolder();

        File folderBeingDownloaded = new File(localFolderBeingSynced, folderName);
        folderBeingDownloaded.mkdir();

        PendingResult<DriveApi.MetadataBufferResult> request = driveFolder.listChildren(apiClient);
        DriveApi.MetadataBufferResult result = request.await();
        MetadataBuffer metadataBuffer = result.getMetadataBuffer();

        for (Metadata metadata : metadataBuffer) {
            if (!metadata.isFolder()) {
                downloadFileFromDriveFolder(metadata, folderBeingDownloaded);
            } else {
                downloadFolderFromDriveFolder(metadata, folderBeingDownloaded);
            }
        }

        metadataBuffer.release();
    }

    // upload a local folder into a drive folder
    private void uploadFolderToDriveFolder(File folder, DriveFolder remoteFolderBeingSynced)
            throws IOException {
        String folderName = folder.getName();
        Log.d(TAG, "Uploading folder with name: " + folderName);

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(folderName).build();

        PendingResult<DriveFolder.DriveFolderResult> request =
                remoteFolderBeingSynced.createFolder(apiClient, changeSet);
        DriveFolder.DriveFolderResult result = request.await();

        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                uploadFileToDriveFolder(file, result.getDriveFolder());
            } else {
                uploadFolderToDriveFolder(file, result.getDriveFolder());
            }
        }
    }

    // download a file from Drive into a local folder
    private void downloadFileFromDriveFolder(Metadata remoteFile, File localFolderBeingSynced)
            throws IOException {
        // TODO SYNC METADATA or conflict resolution won't work
        DriveFile fileToDownload = remoteFile.getDriveId().asDriveFile();
        String fileToDownloadName = getDriveFileName(remoteFile);

        Log.d(TAG, "Downloading file from Drive: " + fileToDownloadName);

        File fileBeingDownloaded = new File (localFolderBeingSynced, fileToDownloadName);
        fileBeingDownloaded.createNewFile();

        PendingResult<DriveApi.DriveContentsResult> request
                = fileToDownload.open(apiClient, DriveFile.MODE_READ_ONLY, null);
        DriveApi.DriveContentsResult result = request.await();
        DriveContents contents = result.getDriveContents();

        InputStream reader = contents.getInputStream();
        FileOutputStream outputStream = new FileOutputStream(fileBeingDownloaded);

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = reader.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        reader.close();
        outputStream.close();

        Log.d(TAG, "Download complete.");
    }

    // upload local file to Drive folder
    private void uploadFileToDriveFolder(File localFile, DriveFolder remoteFolder)
            throws IOException{
        // TODO SYNC METADATA or conflict resolution won't work
        Log.d(TAG, "Uploading file to Drive: " + localFile.getName());

        PendingResult<DriveApi.DriveContentsResult> request =
                Drive.DriveApi.newDriveContents(apiClient);

        DriveApi.DriveContentsResult result = request.await();
        DriveContents driveContents = result.getDriveContents();

        InputStream inputStream = new FileInputStream(localFile);
        OutputStream outputStream = driveContents.getOutputStream();

        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }

        inputStream.close();
        outputStream.close();

        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                .setTitle(localFile.getName())
                .build();

        PendingResult <DriveFolder.DriveFileResult> creationRequest
                = remoteFolder.createFile(apiClient, metadataChangeSet, driveContents);
        DriveFolder.DriveFileResult creationResult = creationRequest.await();

        if (creationResult.getStatus().isSuccess()) {
            Log.d(TAG, "Upload success");
        } else {
            throw new IllegalStateException("Drive upload unsuccessful");
        }


    }

    // check if a drive directory immediately contains a file (non-recursive)
    private Metadata driveFolderContainsFile(File file, MetadataBuffer remoteFiles) {
        for(Metadata remoteFile : remoteFiles) {
            // TODO check below line
            if(file.getName().equals(getDriveFileName(remoteFile))) {
                return remoteFile;
            }
        }
        return null;
    }

    // check if a local folder contains a file with the same name as a drive file
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

    // check if a folder in Google Drive contains a folder with the same name as a local one
    private Metadata driveFolderContainsLocalFolder(File localFolder, MetadataBuffer driveFolders) {
        for(Metadata driveFolder : driveFolders) {
            if(driveFolder.getTitle().equals(localFolder.getName())) {
                return driveFolder;
            }
        }
        return null;
    }

    // check if local folder contains a folder with same name as a folder stored in Drive
    private File localFolderContainsDriveFolder(Metadata driveFolder, File localFolder) {
        for(File folder : localFolder.listFiles()) {
            if(!folder.isFile()) {
                if(folder.getName().equals(driveFolder.getTitle()))
                    return folder;
            }
        }
        return null;
    }

    // get drive name, appending extension if one exists
    private String getDriveFileName(Metadata file) {
        String result = file.getTitle();
        if(!file.getFileExtension().equals(""))
            result += "." + file.getFileExtension();
        return result;
    }

    // send notification about sync status to all listeners that have subscribed via
    // registerSyncStatusListener
    private void notifyAllSyncListeners(final int status) {
        if(syncStatusListeners != null) {
            for (final SyncStatusListener listener : syncStatusListeners) {
                // run on UI thread because lots of UI stuff is affected by status change
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.syncStatusChanged(status);
                    }
                });
            }
        }
    }
}
