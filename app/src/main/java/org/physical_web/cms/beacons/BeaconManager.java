package org.physical_web.cms.beacons;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CountDownLatch;

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

    private BeaconManager() {}

    public static BeaconManager getInstance() {
        return INSTANCE;
    }

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

    public void insertBeacons(final Beacon... beacons) {
        waitOnLatch();

        latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                beaconDao.insertBeacons(beacons);
                refreshBeacons();
                latch.countDown();
            }
        }).start();

    }

    public void updateBeacons(final Beacon... beacons) {
        waitOnLatch();

        latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                beaconDao.updateBeacons(beacons);
                refreshBeacons();
                latch.countDown();
            }
        }).start();
    }

    public List<Beacon> getAllBeacons() {
        waitOnLatch();

        return beacons;
    }

    /**
     * Get beacon by index, from 0..N-1, where N is the number of beacons
     */
    public Beacon getBeaconByIndex(int index) {
        waitOnLatch();

        if(index >= 0 && index < beacons.size())
            return beacons.get(index);
        else
            throw new IllegalArgumentException("Bad beacon index");
    }

    /**
     * Get beacon by id, internal field stored in {@link Beacon#id}. NOT SAME AS INDEX.
     */
    public Beacon getBeaconById(long id) {
        waitOnLatch();

        for(Beacon beacon : beacons) {
            if (beacon.id == id)
                return beacon;
        }

        throw new IllegalArgumentException("no beacon with id " + id + " found");
    }

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

    public void closeAndSave() {
        waitOnLatch();
        db.close();
    }

    private void waitOnLatch() {
        try {
            if (latch != null)
                latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted: " + e);
        }
    }

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
}
