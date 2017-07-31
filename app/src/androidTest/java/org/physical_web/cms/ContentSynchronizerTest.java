package org.physical_web.cms;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.physical_web.cms.sync.ContentSynchronizer;
import org.physical_web.cms.sync.SyncStatusListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.physical_web.cms.sync.ContentSynchronizer.SYNC_COMPLETE;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ContentSynchronizerTest {
    private static final String TAG = ContentSynchronizerTest.class.getSimpleName();

    Context context;
    ContentSynchronizer contentSynchronizer;
    File testingDirectory;
    String randomFileName;
    String randomFileSHA1;
    CountDownLatch lock;
    Boolean uploadRun;

    @Before
    public void setupVariables() {
        context = InstrumentationRegistry.getTargetContext();
        randomFileName = UUID.randomUUID().toString().replaceAll("-", "");
        testingDirectory = context.getFilesDir();
        lock = new CountDownLatch(1);
        uploadRun = true;
    }

    @Before
    public void checkConditions() {
        ConnectivityManager manager = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = manager.getActiveNetworkInfo();

        if (ni == null || !ni.isConnected()) {
            fail("This test requires an internet connection");
        }

        if (Build.PRODUCT.contains("sdk")) {
            fail("This test must be run on a physical device");
        }
    }

    @Test
    public void uploadAndDownloadTest() {
        try {
            createRandomFile();
            Log.d(TAG, "random file created");

            SyncStatusListener mockUploadListener = mock(SyncStatusListener.class);
            contentSynchronizer = ContentSynchronizer.getInstance();
            contentSynchronizer.init(context, context.getFilesDir());
            contentSynchronizer.registerSyncStatusListener(mockUploadListener);

            contentSynchronizer.kickStartSync();

            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            verify(mockUploadListener, timeout(20000).atLeast(2)).syncStatusChanged(captor.capture());
            List<Integer> status = captor.getAllValues();

            if (status.contains(SYNC_COMPLETE)) {
                Log.d(TAG, "Upload complete. deleting local copy");
                (new File(testingDirectory, randomFileName)).delete();
                uploadRun = false;
            } else {
                fail("Upload got wrong status code: " + status);
            }

            SyncStatusListener mockDownloadListener = mock(SyncStatusListener.class);
            doNothing().when(mockUploadListener).syncStatusChanged(eq(1));
            contentSynchronizer.registerSyncStatusListener(mockDownloadListener);
            contentSynchronizer.kickStartSync();

            ArgumentCaptor<Integer> downloadCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(mockDownloadListener, timeout(20000).atLeast(2))
                    .syncStatusChanged(downloadCaptor.capture());
            List<Integer> status2 = downloadCaptor.getAllValues();

            if (status2.contains(SYNC_COMPLETE))
                Log.d(TAG, "Download complete. Checking integrity of fetched copy");
            else
                fail("Download got wrong status code: " + status);

            File downloadedFile = new File(testingDirectory, randomFileName);
            assertTrue(downloadedFile.exists());
            assertEquals(randomFileSHA1, sha1(downloadedFile));
        } catch (Exception e) {
            fail("Failed due to exception: " + e.toString());
        } finally {
            (new File(testingDirectory, randomFileName)).delete();
        }
    }

    private void createRandomFile() throws IOException, NoSuchAlgorithmException {
        File randomFile = new File(testingDirectory, randomFileName);
        randomFile.createNewFile();
        FileWriter writer = new FileWriter(randomFile);

        for (int i = 0; i < 12000; i++) {
            byte[] array = new byte[500]; // length is bounded by 7
            new Random().nextBytes(array);
            String generatedString = new String(array, Charset.forName("UTF-8"));
            writer.write(generatedString);
        }

        writer.flush();
        writer.close();

        randomFileSHA1 = sha1(randomFile);
    }

    /**
     * Read the file and calculate the SHA-1 checksum
     * source: http://www.javacreed.com/how-to-generate-sha1-hash-value-of-file/
     *
     * @param file the file to read
     * @return the hex representation of the SHA-1 using uppercase chars
     * @throws FileNotFoundException    if the file does not exist, is a directory rather than a
     *                                  regular file, or for some other reason cannot be opened for
     *                                  reading
     * @throws IOException              if an I/O error occurs
     * @throws NoSuchAlgorithmException should never happen
     */
    public static String sha1(final File file) throws NoSuchAlgorithmException, IOException {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

        InputStream is = new BufferedInputStream(new FileInputStream(file));
        final byte[] buffer = new byte[1024];
        for (int read = 0; (read = is.read(buffer)) != -1; ) {
            messageDigest.update(buffer, 0, read);
        }

        // Convert the byte to hex format
        Formatter formatter = new Formatter();
        for (final byte b : messageDigest.digest()) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    @After
    public void deleteDriveTestingFiles() {
        contentSynchronizer.deleteSyncedEquivalent((new File(testingDirectory, randomFileName)));
    }
}