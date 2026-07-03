package api.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

final class SponsorOrderIdGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final byte[] PROCESS_RANDOM = new byte[5];
    private static final AtomicInteger COUNTER = new AtomicInteger(RANDOM.nextInt());

    static {
        RANDOM.nextBytes(PROCESS_RANDOM);
    }

    private SponsorOrderIdGenerator() {
    }

    static String nextOrderId() {
        byte[] bytes = new byte[12];
        int timestamp = (int) Instant.now().getEpochSecond();
        bytes[0] = (byte) (timestamp >>> 24);
        bytes[1] = (byte) (timestamp >>> 16);
        bytes[2] = (byte) (timestamp >>> 8);
        bytes[3] = (byte) timestamp;
        System.arraycopy(PROCESS_RANDOM, 0, bytes, 4, PROCESS_RANDOM.length);
        int counter = COUNTER.getAndIncrement() & 0x00ffffff;
        bytes[9] = (byte) (counter >>> 16);
        bytes[10] = (byte) (counter >>> 8);
        bytes[11] = (byte) counter;

        StringBuilder hex = new StringBuilder(24);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b & 0xff));
        }
        return hex.toString();
    }
}
