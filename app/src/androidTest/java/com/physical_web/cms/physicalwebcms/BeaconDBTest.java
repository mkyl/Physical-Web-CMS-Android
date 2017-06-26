package com.physical_web.cms.physicalwebcms;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Instrumented tests for the DatabaseManager class
 */

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BeaconDBTest {
    private static final String TEST_BEACON_NAME = "testing beacon";
    private static final String TEST_BEACON_MAC = "00:11:22:33:44:55";

    private static final String TEST_BEACON_NAME_2 = "second test beacon";
    private static final String TEST_BEACON_MAC_2 = "00:DE:AD:BE:EF:00";

    private DatabaseManager DatabaseManager;

    @Before
    public void SetUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        DatabaseManager = new DatabaseManager(context);
        DatabaseManager.clearDB(context);
    }

    @After
    public void finish() {
        DatabaseManager.close();
    }

    @Test
    public void testPreConditions() {
        assertNotNull(DatabaseManager);
    }

    @Test
    public void addBeacon() {
        Beacon testBeacon = new Beacon(TEST_BEACON_MAC, TEST_BEACON_NAME);
        DatabaseManager.addBeacon(testBeacon);
    }

    /**
     * Add a single beacon and check that it is stored in the database
     */
    @Test
    public void checkBeacon() {
        Beacon testBeacon = new Beacon(TEST_BEACON_MAC, TEST_BEACON_NAME);
        DatabaseManager.addBeacon(testBeacon);
        Set<Beacon> beaconSet = DatabaseManager.getAllBeacons();

        // check number of beacons stored
        assertEquals(beaconSet.size(), 1);
        // check details of stored beacon
        Beacon storedBeacon = beaconSet.iterator().next();
        // this will only check that mac addresses are same
        assertEquals(testBeacon, storedBeacon);
        // other fields
        assertEquals(storedBeacon.getFriendlyName(), TEST_BEACON_NAME);
    }

    @Test
    public void checkMultipleBeacons() {
        Beacon firstBeacon = new Beacon(TEST_BEACON_MAC, TEST_BEACON_NAME);
        Beacon secondBeacon = new Beacon(TEST_BEACON_MAC_2, TEST_BEACON_NAME_2);

        DatabaseManager.addBeacon(firstBeacon);
        DatabaseManager.addBeacon(secondBeacon);
        Set<Beacon> beaconSet = DatabaseManager.getAllBeacons();

        assertEquals(beaconSet.size(), 2);

        assertTrue(beaconSet.contains(firstBeacon));
        assertTrue(beaconSet.contains(secondBeacon));
    }

    /**
     * Add a single beacon, attempt to retrieve it by ID, check that we get the
     * same beacon back
     */
    @Test
    public void checkAssignedID() {
        Beacon testBeacon = new Beacon(TEST_BEACON_MAC, TEST_BEACON_NAME);
        long storedBeaconID = DatabaseManager.addBeacon(testBeacon);

        // check details of stored beacon
        assertEquals(testBeacon, DatabaseManager.getBeaconByID(storedBeaconID));
    }

    @Test
    public void deleteOne() {
        Beacon testBeacon = new Beacon(TEST_BEACON_MAC, TEST_BEACON_NAME);
        DatabaseManager.addBeacon(testBeacon);
        DatabaseManager.deleteBeacon(testBeacon);
        assertEquals(0, DatabaseManager.getAllBeacons().size());
    }

    /**
     * Try to remove beacon from empty DB, observe an IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidDelete(){
        DatabaseManager.deleteBeacon(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addDuplicateBeacon() throws InterruptedException {
        Beacon testBeacon = new Beacon(TEST_BEACON_MAC, TEST_BEACON_NAME);
        DatabaseManager.addBeacon(testBeacon);

        Beacon testBeaconTwo = new Beacon(TEST_BEACON_MAC, TEST_BEACON_NAME);
        DatabaseManager.addBeacon(testBeaconTwo);
    }
}
