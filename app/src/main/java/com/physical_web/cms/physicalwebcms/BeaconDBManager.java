package com.physical_web.cms.physicalwebcms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles storing the details of the beacons enrolled in the application. MAKE SURE TO CALL
 * CLOSE() WHEN DONE
 */

public class BeaconDBManager {
    private BeaconDBHelper dbHelper;
    private SQLiteDatabase db;

    public BeaconDBManager(Context context) {
        dbHelper = new BeaconDBHelper(context);
        // Writable database is used because there is no performance advantage over read-only one
        db = dbHelper.getWritableDatabase();
    }

    /**
     * Try to close the database instance. Must be done to ensure data is written to disk, but
     * expensive, so do once assured no more writes or writes to db required. Cannot call any
     * other methods in this class once this is called.
     *
     * @throws IllegalStateException if attempting to close a non-initialized or closed DB
     */
    public void close() {
        if(db != null && db.isOpen())
            dbHelper.close();
        else
            throw new IllegalStateException("Tried to close an non-existent or closed database");
    }

    /**
     * DELETES DATABASE
     */
    public void clearDB(Context context) {
        this.close();
        context.deleteDatabase(dbHelper.getDatabaseName());
        db = dbHelper.getWritableDatabase();
    }

    /**
     * Add a beacon to the database
     *
     * @param proposedBeacon beacon to be added to database
     * @return internal ID assigned to beacon
     * @throws IllegalStateException if attempting to add beacon to non-initialized or closed DB
     */
    public long addBeacon(Beacon proposedBeacon) {
        if (beaconIsInDB(proposedBeacon)) {
            throw new IllegalArgumentException("Attempted to add beacon that already in DB");
        }

        if (databaseIsOpen()) {
            ContentValues values = new ContentValues();
            values.put(BeaconDBContract.BeaconEntry.COLUMN_NAME_BT_ADDRESS,
                    proposedBeacon.getAddress());
            values.put(BeaconDBContract.BeaconEntry.COLUMN_NAME_FRIENDLY_NAME,
                    proposedBeacon.getFriendlyName());

            long id = db.insert(BeaconDBContract.BeaconEntry.TABLE_NAME, null, values);
            proposedBeacon.setId(id);
            return id;
        } else {
            throw new IllegalStateException("Attempted to add beacon to closed or null DB");
        }
    }

    /**
     * Remove a beacon from the database by its internal ID
     *
     * @param id internal ID assigned to beacon
     * @throws IllegalArgumentException if no such beacon exists
     * @throws IllegalStateException if attempting to remove beacon from non-initialized or closed
     *                               DB
     */
    public void deleteBeacon(long id) throws IllegalArgumentException {
        if (beaconIsInDB(id) && databaseIsOpen()) {
            String selection = BeaconDBContract.BeaconEntry._ID + " LIKE ?";
            String[] selectionArgs = {Long.toString(id)};
            db.delete(BeaconDBContract.BeaconEntry.TABLE_NAME, selection, selectionArgs);
        } else {
            throw new IllegalStateException("Attempted to delete beacon in closed or null DB");
        }
    }

    /**
     * Remove a beacon from the database
     *
     * @param beacon The beacon to be removed from Database
     * @throws IllegalArgumentException if no such beacon exists
     * @throws IllegalStateException if attempting to remove beacon from non-initialized or closed
     *                               DB
     */
    public void deleteBeacon(Beacon beacon) throws IllegalArgumentException {
        if (!beaconIsInDB(beacon)) {
            throw new IllegalArgumentException("Tried to remove beacon not in DB");
        } else {
            deleteBeacon(beacon.getId());
        }
    }

    /**
     * Get a Set containing all beacons stored in database
     *
     * @throws IllegalStateException if called when DB is closed or non-initialized
     * @return Set of beacons
     */
    public Set<Beacon> getAllBeacons() {
        if(databaseIsOpen()) {
            Cursor cursor = db.rawQuery("SELECT * FROM " + BeaconDBContract.BeaconEntry.TABLE_NAME,
                    null);

            Set<Beacon> results = new HashSet<>();
            while (cursor.moveToNext()) {
                Beacon result = beaconFromCursor(cursor);
                try {
                    results.add(result);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Database contains a duplicate beacon");
                }
            }
            cursor.close();

            return results;
        } else {
            throw new IllegalStateException("Attempted to list beacons from closed or null DB");
        }
    }

    /**
     * Get a Beacon by its internal ID
     *
     * @param searchId Internal ID of beacon
     * @throws IllegalStateException if called when DB is closed or non-initialized
     * @return beacon object
     */
    public Beacon getBeaconByID(long searchId) {
        if(databaseIsOpen()) {
            String[] projection = {
                    BeaconDBContract.BeaconEntry._ID,
                    BeaconDBContract.BeaconEntry.COLUMN_NAME_BT_ADDRESS,
                    BeaconDBContract.BeaconEntry.COLUMN_NAME_FRIENDLY_NAME
            };
            String selection = BeaconDBContract.BeaconEntry._ID + " = ?";
            String[] selectionArgs = {Long.toString(searchId)};

            Cursor cursor = db.query(
                    BeaconDBContract.BeaconEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );

            cursor.moveToFirst();

            try {
                return beaconFromCursor(cursor);
            } catch (Exception e) {
                throw new IllegalArgumentException("No such beacon exists in database");
            } finally {
                cursor.close();
            }
        } else {
            throw new IllegalStateException("Attempted to list beacon from closed or null DB");
        }
    }

    // returns beacon object from a cursor pointing at stored beacon in DB
    private Beacon beaconFromCursor(Cursor cursor) {
        long id = cursor.getLong(
                cursor.getColumnIndexOrThrow(BeaconDBContract.BeaconEntry._ID));
        String address = cursor.getString(
                cursor.getColumnIndexOrThrow(BeaconDBContract.BeaconEntry.COLUMN_NAME_BT_ADDRESS));
        String friendlyName = cursor.getString(
                cursor.getColumnIndexOrThrow(BeaconDBContract.BeaconEntry
                        .COLUMN_NAME_FRIENDLY_NAME));

        return new Beacon(id, address, friendlyName);
    }

    private Boolean databaseIsOpen() {
        return this.db != null && this.db.isOpen();
    }

    private Boolean beaconIsInDB(Beacon proposedBeacon) {
        Set<Beacon> existingBeacons = this.getAllBeacons();
        return existingBeacons.contains(proposedBeacon);
    }

    private Boolean beaconIsInDB(long id) {
        return getBeaconByID(id) != null;
    }

    private class BeaconDBHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;
        private static final String DATABASE_NAME = "beacons.db";

        BeaconDBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(BeaconDBContract.SQL_CREATE_ENTRIES);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO populate this as needed
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO populate this as needed
        }
    }

    private final class BeaconDBContract {
        static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + BeaconEntry.TABLE_NAME + " (" +
                        BeaconEntry._ID + " INTEGER PRIMARY KEY," +
                        BeaconEntry.COLUMN_NAME_FRIENDLY_NAME + " TEXT," +
                        BeaconEntry.COLUMN_NAME_BT_ADDRESS + " TEXT)";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + BeaconEntry.TABLE_NAME;

        // To prevent someone from accidentally instantiating the contract class,
        // make the constructor private.
        private BeaconDBContract() {}

        /* Inner class that defines the table contents */
        class BeaconEntry implements BaseColumns {
            static final String TABLE_NAME = "beacons";
            static final String COLUMN_NAME_FRIENDLY_NAME = "friendly_name";
            static final String COLUMN_NAME_BT_ADDRESS = "bt_address";
        }
    }
}


