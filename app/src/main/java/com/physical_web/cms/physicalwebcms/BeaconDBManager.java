package com.physical_web.cms.physicalwebcms;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Handles storing the details of the beacons enrolled in the application
 */

public class BeaconDBManager {
    private BeaconDBHelper dbHelper;

    public BeaconDBManager(Context context) {
        dbHelper = new BeaconDBHelper(context);
    }

    public long addBeacon(String bluetoothID, String friendlyName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BeaconDBContract.Beacon.COLUMN_NAME_BT_ID, bluetoothID);
        values.put(BeaconDBContract.Beacon.COLUMN_NAME_FRIENDLY_NAME, friendlyName);

        return db.insert(BeaconDBContract.Beacon.TABLE_NAME, null, values);
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
            "CREATE TABLE " + Beacon.TABLE_NAME + " (" +
                    Beacon._ID + " INTEGER PRIMARY KEY," +
                    Beacon.COLUMN_NAME_FRIENDLY_NAME + " TEXT," +
                    Beacon.COLUMN_NAME_BT_ID + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Beacon.TABLE_NAME;

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private BeaconDBContract() {}

    /* Inner class that defines the table contents */
    public static class Beacon implements BaseColumns {
        public static final String TABLE_NAME = "beacons";
        public static final String COLUMN_NAME_FRIENDLY_NAME = "friendly-name";
        public static final String COLUMN_NAME_BT_ID = "bt-id";
    }
}
