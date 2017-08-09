package org.physical_web.cms.beacons;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * This class represents an Eddystone Beacon, with a physical address, internal ID
 * and user friendly name
 */
@Entity(tableName = "beacons")
public class Beacon {
    @PrimaryKey(autoGenerate = false)
    public MacAddress address;
    public String friendlyName;

    public Beacon(MacAddress address, String friendlyName) {
        this.address = address;
        this.friendlyName = friendlyName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null)
            return false;
        if (getClass() != other.getClass())
            return false;

        Beacon otherBeacon = (Beacon) other;
        return (this.address.equals(otherBeacon.address));
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

