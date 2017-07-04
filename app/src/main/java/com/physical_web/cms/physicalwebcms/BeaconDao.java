package com.physical_web.cms.physicalwebcms;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface BeaconDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    public void insertBeacons(Beacon... beacons);

    @Update
    public void updateBeacons(Beacon... beacons);

    @Query("select * from beacons")
    List<Beacon> getAllBeacons();

    @Query("select * from beacons where id = :id")
    Beacon getBeaconById(long id);

    @Delete
    public void deleteBeacons(Beacon... beacons);
}
