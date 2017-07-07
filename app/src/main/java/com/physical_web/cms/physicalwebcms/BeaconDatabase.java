package com.physical_web.cms.physicalwebcms;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import java.io.File;

@Database(entities = {Beacon.class}, version = 1)
public abstract class BeaconDatabase extends RoomDatabase {
    public final static String DATABASE_NAME = "beacon_db";

    private static BeaconDatabase instance;

    public static BeaconDatabase getDatabase(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    BeaconDatabase.class, DATABASE_NAME).build();
        }
        return instance;
    }

    public abstract BeaconDao beaconDao();
}
