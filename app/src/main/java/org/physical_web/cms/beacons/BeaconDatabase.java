package org.physical_web.cms.beacons;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.Update;
import android.content.Context;

import java.util.List;

@Database(entities = {Beacon.class}, version = 2)
@TypeConverters({MacRoomConverters.class})
abstract class BeaconDatabase extends RoomDatabase {
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

@Dao
interface BeaconDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertBeacons(Beacon... beacons);

    @Update
    void updateBeacons(Beacon... beacons);

    @Query("select * from beacons")
    List<Beacon> getAllBeacons();

    @Query("select * from beacons where address = :address")
    Beacon getBeaconById(MacAddress address);

    @Delete
    void deleteBeacons(Beacon... beacons);
}
