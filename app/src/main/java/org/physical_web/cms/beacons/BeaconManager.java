package org.physical_web.cms.beacons;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.physical_web.cms.exhibits.ExhibitManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.crypto.Mac;

import util.MiscFile;

/**
 * Singleton class to manage beacons. {@link #setContext(Context)} must be called
 * before calling any methods other than {@link #getInstance()}. Call {@link #closeAndSave()} when
 * done with class to save changes.
 */

public class BeaconManager {
    private static final BeaconManager INSTANCE = new BeaconManager();
    private static final String TAG = BeaconManager.class.getSimpleName();

    private Context context = null;
    private BeaconDatabase db;
    private BeaconDao beaconDao;
    private List<Beacon> beacons = null;
    private CountDownLatch latch = null;

    private BeaconManager() {
    }

    public static BeaconManager getInstance() {
        return INSTANCE;
    }

    /**
     * Set context that this class will operate in. MUST BE CALLED BEFORE ANY OTHER METHODS.
     *
     * @param context to work in
     */
    public void setContext(final Context context) {
        this.context = context;
        latch = new CountDownLatch(1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                db = BeaconDatabase.getDatabase(context);
                beaconDao = db.beaconDao();
                refreshBeacons();
                latch.countDown();
            }
        }).run();
    }

    /**
     * Store new beacons persistently
     *
     * @param beacons new beacons
     */
    public void insertBeacons(final Beacon... beacons) {
        waitOnLatch();

        latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                beaconDao.insertBeacons(beacons);
                updateBeaconMetadata(beacons);
                refreshBeacons();
                latch.countDown();
            }
        }).start();

    }

    /**
     * Provide a list of modified beacons that will be persisted to disk
     *
     * @param beacons changed beacons
     */
    public void updateBeacons(final Beacon... beacons) {
        waitOnLatch();

        latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                beaconDao.updateBeacons(beacons);
                updateBeaconMetadata(beacons);
                refreshBeacons();
                latch.countDown();
            }
        }).start();
    }

    /**
     * Get a list of all stored beacons
     *
     * @return all stored beacons
     */
    public List<Beacon> getAllBeacons() {
        waitOnLatch();

        return beacons;
    }

    /**
     * Get beacon by index, from 0..N-1, where N is the number of beacons
     */
    public Beacon getBeaconByIndex(int index) {
        waitOnLatch();

        if (index >= 0 && index < beacons.size())
            return beacons.get(index);
        else
            throw new IllegalArgumentException("Bad beacon index");
    }

    /**
     * Get beacon by its mac address, internal field stored in {@link Beacon#address}
     */
    public Beacon getBeaconByAddress(MacAddress macAddress) {
        waitOnLatch();

        for (Beacon beacon : beacons) {
            if (beacon.address.equals(macAddress))
                return beacon;
        }

        throw new IllegalArgumentException("no beacon with address "
                + macAddress.toString() + " found");
    }

    /**
     * Remove beacons from persistent storage
     *
     * @param beacons to be removed
     */
    public void deleteBeacons(final Beacon... beacons) {
        waitOnLatch();

        latch = new CountDownLatch(1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                beaconDao.deleteBeacons(beacons);
                refreshBeacons();
                latch.countDown();
            }
        }).start();
    }

    /**
     * Call when done making changes to beacons. No other methods may be called after this one.
     */
    public void closeAndSave() {
        waitOnLatch();
        db.close();
    }

    // wait while other database operations complete
    private void waitOnLatch() {
        try {
            if (latch != null)
                latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted: " + e);
        }
    }

    // reload beacons from database
    private void refreshBeacons() {
        final CountDownLatch refreshLatch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                beacons = beaconDao.getAllBeacons();
                refreshLatch.countDown();
            }
        }).start();

        try {
            refreshLatch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Refresh interrupted: " + e);
        }
    }

    private void updateBeaconMetadata(Beacon... beacons) {
        try {
            File metadataFile = new File(ExhibitManager.getInstance().getExhibitsFolder(),
                    "metadata.json");
            if (!metadataFile.exists()) {
                metadataFile.createNewFile();
                JSONObject md = new JSONObject();
                JSONArray beaconNames = new JSONArray();
                md.put("beacon-names", beaconNames);
                MiscFile.writeToFile(metadataFile, md.toString());
            }

            JSONObject metadata = new JSONObject(MiscFile.readFile(metadataFile));
            JSONArray beaconName = metadata.getJSONArray("beacon-names");
            for (Beacon beacon : beacons) {
                JSONObject beaconMd = new JSONObject();
                beaconMd.put("address", beacon.address);
                beaconMd.put("friendly-name", beacon.friendlyName);
                beaconName.put(beaconMd);
            }
            MiscFile.writeToFile(metadataFile, metadata.toString());
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Couldn't update beacon metadata: " + e);
        }
    }
}
