package es.in2.wallet.api.crypto.service.impl;

import java.util.ArrayList;
import java.util.List;

public class UVarInt {
    private final long value;
    private final byte[] bytes;
    private final int length;

    public UVarInt(long value) {
        this.value = value;
        this.bytes = bytesFromUInt(value);
        this.length = bytes.length;
    }

    public long getValue() {
        return value;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getLength() {
        return length;
    }

    private byte[] bytesFromUInt(long num) {
        List<Byte> varInt = new ArrayList<>();
        long rest = num;
        while ((rest & MSBALL) != 0) {
            varInt.add((byte) ((rest & 0xFF) | MSB));
            rest = rest >>> 7;
        }
        varInt.add((byte) rest);
        byte[] result = new byte[varInt.size()];
        for (int i = 0; i < varInt.size(); i++) {
            result[i] = varInt.get(i);
        }
        return result;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(value);
    }

    public static final long MSB = 0x80L;
    public static final long LSB = 0x7FL;
    public static final long MSBALL = 0xFFFFFF80L;

    public static UVarInt fromBytes(byte[] bytes) {
        if (bytes.length == 0) {
            throw new IllegalArgumentException("Empty byte array");
        }

        int idx = 0;
        long value = (bytes[idx] & LSB);
        while (idx + 1 < bytes.length && (bytes[idx] & MSB) != 0) {
            idx++;
            value |= ((bytes[idx] & LSB) << (idx * 7));
        }
        return new UVarInt(value);
    }
}
