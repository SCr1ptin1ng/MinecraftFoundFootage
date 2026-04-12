package com.sp.world.generation;

import com.sp.SPBRevamped;
import com.sp.init.BackroomsLevels;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class Level959PlacementDebug {
    private static final Map<String, AtomicInteger> COUNTS = new ConcurrentHashMap<>();
    private static final AtomicInteger TOTAL = new AtomicInteger();

    private Level959PlacementDebug() {
    }

    public static void reset() {
        COUNTS.clear();
        TOTAL.set(0);
        SPBRevamped.LOGGER.info("Level959 placement counters reset.");
    }

    public static void record(StructureWorldAccess world, Identifier identifier, BlockPos pos) {
        if (world.toServerWorld().getRegistryKey() != BackroomsLevels.A_PLACE_YOU_DONT_WANT_TO_KNOW_WORLD_KEY) {
            return;
        }

        if (!SPBRevamped.MOD_ID.equals(identifier.getNamespace()) || !identifier.getPath().startsWith("level959/")) {
            return;
        }

        String key = identifier.toString();
        int pieceCount = COUNTS.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
        int total = TOTAL.incrementAndGet();

        if (pieceCount == 1) {
            SPBRevamped.LOGGER.info("Level959 placed first instance of {} at {}", key, pos);
        }

        if (total % 20 == 0) {
            logSummary("running");
        }
    }

    public static void logSummary(String label) {
        List<Map.Entry<String, AtomicInteger>> entries = new ArrayList<>(COUNTS.entrySet());
        entries.sort(Comparator.<Map.Entry<String, AtomicInteger>>comparingInt(entry -> entry.getValue().get()).reversed());

        StringBuilder builder = new StringBuilder();
        builder.append("Level959 placement summary (").append(label).append(") total=").append(TOTAL.get()).append(": ");

        if (entries.isEmpty()) {
            builder.append("no pieces recorded");
        } else {
            for (int i = 0; i < entries.size(); i++) {
                Map.Entry<String, AtomicInteger> entry = entries.get(i);
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(entry.getKey()).append("=").append(entry.getValue().get());
            }
        }

        SPBRevamped.LOGGER.info(builder.toString());
    }
}
