package com.physical_web.cms.physicalwebcms;

/**
 * This class represents an Eddystone Beacon, with a physical address, internal ID
 * and user friendly name
 */
class Beacon {
    private long id;
    private String address;
    private String friendlyName;
    private Boolean fullyInitialized;

    public Beacon(long id, String address, String friendlyName) {
        this.id = id;
        this.address = address;
        this.friendlyName = friendlyName;
        fullyInitialized = false;
    }

    // beacon whose ID we don't know yet
    public Beacon(String address, String friendlyName) {
        this(-1 , address, friendlyName);
        fullyInitialized = false;
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

    // can be only called on Beacon created without specifying ID
    public void setId(long newID) {
        if(this.fullyInitialized) {
            throw new IllegalStateException("Cannot change internal ID once its been set");
        } else {
            this.id = newID;
            fullyInitialized = true;
        }
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
        return (this.address.equalsIgnoreCase(otherBeacon.getAddress()));
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
