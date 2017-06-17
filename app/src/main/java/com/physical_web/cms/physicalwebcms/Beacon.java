package com.physical_web.cms.physicalwebcms;

/**
 * This class represents an Eddystone Beacon, with a physical address, internal ID
 * and user friendly name
 */
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
