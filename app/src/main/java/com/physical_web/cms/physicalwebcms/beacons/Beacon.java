package com.physical_web.cms.physicalwebcms.beacons;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * This class represents an Eddystone Beacon, with a physical address, internal ID
 * and user friendly name
 */
@Entity(tableName = "com/physical_web/cms/physicalwebcms/beacons")
public class Beacon {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String address;
    public String friendlyName;
    public Boolean unconfigured;
    // public Bitmap locationImage;

    public Beacon(String address, String friendlyName) {
        this.address = address;
        this.friendlyName = friendlyName;
        this.unconfigured = false;
    }

    @Override
    public boolean equals(Object other) {
        if(this == other)
            return true;
        if(other == null)
            return false;
        if(getClass() != other.getClass())
            return false;

        Beacon otherBeacon = (Beacon) other;
        return (this.address.equalsIgnoreCase(otherBeacon.address));
    }

    /**
     * Hashcode must be overwritten to be used in a set
     */
    @Override
    public int hashCode() {
        return address.hashCode();
    }

    public String toString() {
        String result = "";
        result += "Beacon '" + friendlyName + "' ";
        result += "with address " + address;
        return result;
    }
}

