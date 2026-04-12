package com.sp.sounds;

import com.sp.SPBRevamped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class Level959SoundDebug {
    private static final Map<String, AtomicInteger> COUNTS = new ConcurrentHashMap<>();
    private static final AtomicInteger TOTAL = new AtomicInteger();

    private Level959SoundDebug() {
    }

    public static void reset() {
        COUNTS.clear();
        TOTAL.set(0);
    }

    public static void record(String soundKey) {
        COUNTS.computeIfAbsent(soundKey, key -> new AtomicInteger()).incrementAndGet();
        int total = TOTAL.incrementAndGet();

        int count = COUNTS.get(soundKey).get();
        SPBRevamped.LOGGER.info("Level959 sound '{}' played {} time(s). Total level959 sounds: {}", soundKey, count, total);

        if (total % 10 == 0) {
            logSummary("rolling");
        }
    }

    public static void logSummary(String label) {
        StringBuilder builder = new StringBuilder("Level959 sound summary [").append(label).append("]: total=").append(TOTAL.get());
        COUNTS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> builder.append(", ").append(entry.getKey()).append("=").append(entry.getValue().get()));
        SPBRevamped.LOGGER.info(builder.toString());
    }
}
