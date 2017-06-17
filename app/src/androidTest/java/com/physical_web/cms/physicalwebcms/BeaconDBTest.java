package com.physical_web.cms.physicalwebcms;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * Instrumented tests for the BeaconDBManager class
 */

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BeaconDBTest {
    private static final String TEST_BEACON_NAME = "testing beacon";
    private static final String TEST_BEACON_MAC = "00:11:22:33:44:55";
    private BeaconDBManager beaconDBManager;

    @Before
    public void SetUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        beaconDBManager = new BeaconDBManager(context);
        beaconDBManager.clearDB(context);
    }

    @After
    public void finish() {
        beaconDBManager.close();
    }

    @Test
    public void testPreConditions() {
        assertNotNull(beaconDBManager);
    }

    @Test
    public void addBeacon() {
        Beacon testBeacon = new Beacon(TEST_BEACON_MAC, TEST_BEACON_NAME);
        beaconDBManager.addBeacon(testBeacon);
    }

    /**
     * Add a single beacon and check that it is stored in the database
     */
    @Test
    public void checkBeacon() {
        Beacon testBeacon = new Beacon(TEST_BEACON_MAC, TEST_BEACON_NAME);
        beaconDBManager.addBeacon(testBeacon);
        Set<Beacon> beaconSet = beaconDBManager.getAllBeacons();

        // check number of beacons stored
        assertEquals(beaconSet.size(), 1);
        // check details of stored beacon
        Beacon storedBeacon = beaconSet.iterator().next();
        // this will only check that mac addresses are same
        assertEquals(testBeacon, storedBeacon);
        // other fields
        assertEquals(storedBeacon.getFriendlyName(), TEST_BEACON_NAME);
    }

    /**
     * Add a single beacon, attempt to retrieve it by ID, check that we get the
     * same beacon back
     */
    @Test
    public void checkAssignedID() {
        Beacon testBeacon = new Beacon(TEST_BEACON_MAC, TEST_BEACON_NAME);
        long storedBeaconID = beaconDBManager.addBeacon(testBeacon);

        // check details of stored beacon
        assertEquals(testBeacon, beaconDBManager.getBeaconByID(storedBeaconID));
    }

    /**
     * Try to remove beacon from empty DB, observe an IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidDelete(){
        beaconDBManager.deleteBeacon(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addDuplicateBeacon() throws InterruptedException {
        Beacon testBeacon = new Beacon(TEST_BEACON_MAC, TEST_BEACON_NAME);
        beaconDBManager.addBeacon(testBeacon);

        Beacon testBeaconTwo = new Beacon(TEST_BEACON_MAC, TEST_BEACON_NAME);
        beaconDBManager.addBeacon(testBeaconTwo);
    }
}
