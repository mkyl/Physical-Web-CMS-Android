package org.physical_web.cms;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.physical_web.cms.bluetooth.BluetoothManager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class BluetoothManagerTest {
    private final static String TAG = BluetoothManagerTest.class.getSimpleName();
    BluetoothManager testSubject;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        testSubject = new BluetoothManager(context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonHTTPS() {
        String nonHTTPSUri = "http://example.com";
        testSubject.shortenIfNeeded(nonHTTPSUri);
    }

    @Test
    public void noNeedToShortenTest() {
        String alreadyShortUri = "https://192.168.1.1";
        String processedUri = testSubject.shortenIfNeeded(alreadyShortUri);
        assertEquals(alreadyShortUri.substring("https://".length()), processedUri);
    }

    @Test
    public void shortenTest() {
        String longUri = "https://example.com/?solonglonglonguri";
        String longUriWithoutPrefix = longUri.substring("https://".length());
        String processedUri = testSubject.shortenIfNeeded(longUri);
        assertTrue(processedUri.length() < longUriWithoutPrefix.length());
        assertTrue(processedUri.length() < 16); // max length of URI per eddystone spec
        assertTrue(processedUri.contains("goo.gl"));
        Log.i(TAG, "shortned URI correctly: " + processedUri);
    }
}
