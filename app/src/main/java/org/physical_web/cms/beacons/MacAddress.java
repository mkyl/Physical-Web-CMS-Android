package org.physical_web.cms.beacons;

import android.arch.persistence.room.TypeConverter;

import java.util.Arrays;

/**
 * Represents a mac address
 */

public class MacAddress {
    private final static int MAC_ADDRESS_SIZE_BYTES = 6;
    private final static int FORMATTED_MAC_STRING_LENGTH = 17;
    private Byte[] address = new Byte[MAC_ADDRESS_SIZE_BYTES];

    public MacAddress(Byte[] inputAddress) {
        if (inputAddress == null)
            throw new IllegalArgumentException("Mac address can't be null");

        if (inputAddress.length != MAC_ADDRESS_SIZE_BYTES)
            throw new IllegalArgumentException("wrong MAC address length");

        this.address = Arrays.copyOf(inputAddress, inputAddress.length);
    }

    public static MacAddress fromString(String macAddress) {
        return fromString(macAddress, '-');
    }

    public static MacAddress fromString(String macAddress, char splitter) {
        if (macAddress.length() != FORMATTED_MAC_STRING_LENGTH)
            throw new IllegalArgumentException();

        String[] macAddressParts = macAddress.split(String.valueOf(splitter));
        Byte[] macAddressBytes = new Byte[MAC_ADDRESS_SIZE_BYTES];
        for(int i = 0; i < macAddressBytes.length; i++){
            Integer hex = Integer.parseInt(macAddressParts[i], 16);
            macAddressBytes[i] = hex.byteValue();
        }

        return new MacAddress(macAddressBytes);
    }

    @Override
    public String toString() {
        String hexCouple = String.format("%02x", address[0]);
        String macAddress = "" + hexCouple;
        for(int i = 1; i < address.length; i += 1) {
            hexCouple = "-" + String.format("%02x", address[i]);
            macAddress = macAddress.concat(hexCouple);
        }
        return macAddress;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null)
            return false;
        if (getClass() != other.getClass())
            return false;

        MacAddress otherMac = (MacAddress) other;
        return (Arrays.equals(this.address, otherMac.address));
    }
}

class MacRoomConverters {
    @TypeConverter
    public static MacAddress fromFormattedString(String value) {
        return value == null ? null : MacAddress.fromString(value);
    }

    @TypeConverter
    public static String macAddressToFormattedString(MacAddress value) {
        return value == null ? null : value.toString();
    }
}
