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

    public void close() {
        dbHelper.close();
    }

    public long addBeacon(String bluetoothID, String friendlyName) {
        ContentValues values = new ContentValues();
        values.put(BeaconDBContract.BeaconEntry.COLUMN_NAME_BT_ID, bluetoothID);
        values.put(BeaconDBContract.BeaconEntry.COLUMN_NAME_FRIENDLY_NAME, friendlyName);

        return db.insert(BeaconDBContract.BeaconEntry.TABLE_NAME, null, values);
    }

    public void deleteBeacon(long id) throws IllegalArgumentException {
        String selection = BeaconDBContract.BeaconEntry.COLUMN_NAME_BT_ID + " LIKE ?";
        String[] selectionArgs = {Long.toString(id)};
        db.delete(BeaconDBContract.BeaconEntry.TABLE_NAME, selection, selectionArgs);
    }

    public Set<Beacon> getAllBeacons() {
        Cursor cursor = db.rawQuery("SELECT * FROM" + BeaconDBContract.BeaconEntry.TABLE_NAME,
                null);

        Set<Beacon> results = new HashSet<>();
        while(cursor.moveToNext()) {
            Beacon result = beaconFromCursor(cursor);
            try {
                results.add(result);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("App tried to add a duplicate beacon");
            }
        }

        cursor.close();

        return results;
    }

    public Beacon getBeaconByID(long searchId) {
        String[] projection = {
                BeaconDBContract.BeaconEntry._ID,
                BeaconDBContract.BeaconEntry.COLUMN_NAME_BT_ID,
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
        } finally{
            cursor.close();
        }
    }

    private Beacon beaconFromCursor(Cursor cursor) {
        long id = cursor.getLong(
                cursor.getColumnIndexOrThrow(BeaconDBContract.BeaconEntry._ID));
        String address = cursor.getString(
                cursor.getColumnIndexOrThrow(BeaconDBContract.BeaconEntry.COLUMN_NAME_BT_ID));
        String friendlyName = cursor.getString(
                cursor.getColumnIndexOrThrow(BeaconDBContract.BeaconEntry
                        .COLUMN_NAME_FRIENDLY_NAME));

        return new Beacon(id, address, friendlyName);
    }
}

class Beacon {
    private long id;
    private String address;
    private String friendlyName;

    public Beacon(long id, String address, String friendlyName) {
        this.id = id;
        this.address = address;
        this.friendlyName = friendlyName;
    }

    public long getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public boolean equals(Beacon other) {
        return (this.address.equalsIgnoreCase(other.getAddress()));
    }
}

class BeaconDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "beacons.db";

    public BeaconDBHelper(Context context) {
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

final class BeaconDBContract {
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + BeaconEntry.TABLE_NAME + " (" +
                    BeaconEntry._ID + " INTEGER PRIMARY KEY," +
                    BeaconEntry.COLUMN_NAME_FRIENDLY_NAME + " TEXT," +
                    BeaconEntry.COLUMN_NAME_BT_ID + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + BeaconEntry.TABLE_NAME;

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private BeaconDBContract() {}

    /* Inner class that defines the table contents */
    public static class BeaconEntry implements BaseColumns {
        public static final String TABLE_NAME = "beacons";
        public static final String COLUMN_NAME_FRIENDLY_NAME = "friendly-name";
        public static final String COLUMN_NAME_BT_ID = "bt-id";
    }
}
