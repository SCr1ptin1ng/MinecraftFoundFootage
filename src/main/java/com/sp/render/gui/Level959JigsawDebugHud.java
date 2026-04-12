package com.sp.render.gui;

import com.sp.SPBRevamped;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Level959JigsawDebugHud implements HudRenderCallback {
    private static final Pattern LOCATION_PATTERN = Pattern.compile("\"location\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ERROR_PATTERN = Pattern.compile("No starting jigsaw .*|.*level959.*", Pattern.CASE_INSENSITIVE);
    private static final long REFRESH_INTERVAL_MS = 1000L;

    private long lastRefresh;
    private List<String> cachedLines = List.of("Waiting for level959 debug data...");

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!FabricLoader.getInstance().isDevelopmentEnvironment() || client.player == null || !shouldShow(client)) {
            return;
        }

        long now = Util.getMeasuringTimeMs();
        if (now - this.lastRefresh >= REFRESH_INTERVAL_MS) {
            this.lastRefresh = now;
            this.cachedLines = collectDebugLines();
        }

        int x = 8;
        int y = 8;
        int lineHeight = client.textRenderer.fontHeight + 2;
        int width = 0;
        for (String line : this.cachedLines) {
            width = Math.max(width, client.textRenderer.getWidth(line));
        }

        int height = this.cachedLines.size() * lineHeight + 6;
        drawContext.fill(x - 4, y - 4, x + width + 4, y + height, 0x90000000);

        for (int i = 0; i < this.cachedLines.size(); i++) {
            drawContext.drawText(client.textRenderer, Text.literal(this.cachedLines.get(i)), x, y + i * lineHeight, 0xFFFFFF, false);
        }
    }

    private boolean shouldShow(MinecraftClient client) {
        if (client.player == null) {
            return false;
        }

        if (client.player.getMainHandStack().isOf(Blocks.JIGSAW.asItem()) || client.player.getOffHandStack().isOf(Blocks.JIGSAW.asItem())) {
            return true;
        }

        if (client.crosshairTarget instanceof BlockHitResult blockHitResult && client.world != null) {
            return client.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.JIGSAW);
        }

        return client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK;
    }

    private List<String> collectDebugLines() {
        List<String> lines = new ArrayList<>();
        Path projectRoot = getProjectRoot();
        Path startPoolPath = projectRoot.resolve(Path.of("src", "main", "resources", "data", "spb-revamped", "worldgen", "template_pool", "level959", "start.json"));
        Path room1Path = projectRoot.resolve(Path.of("src", "main", "resources", "data", "spb-revamped", "structures", "level959", "959room1.nbt"));
        Path logPath = FabricLoader.getInstance().getGameDir().resolve(Path.of("logs", "latest.log"));

        lines.add("Level959 Jigsaw Debug");
        lines.add("Pool: spb-revamped:level959/start");
        lines.add("Target Name: spb-revamped:level959/start");
        lines.add("Start JSON: " + describeStartPool(startPoolPath));
        lines.add("Room1 NBT: " + (Files.exists(room1Path) ? "found" : "missing"));

        String latestError = findLatestLevel959Error(logPath);
        lines.add("Latest log: " + latestError);

        if (latestError.contains("No starting jigsaw")) {
            lines.add("Check the first jigsaw inside 959room1.");
            lines.add("Its NAME must be spb-revamped:level959/start.");
        }

        return lines;
    }

    private Path getProjectRoot() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path fileName = gameDir.getFileName();
        if (fileName != null && fileName.toString().equalsIgnoreCase("run") && gameDir.getParent() != null) {
            return gameDir.getParent();
        }
        return gameDir;
    }

    private String describeStartPool(Path startPoolPath) {
        if (!Files.exists(startPoolPath)) {
            return "missing";
        }

        try {
            String json = Files.readString(startPoolPath);
            Matcher matcher = LOCATION_PATTERN.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return "present, but no location found";
        } catch (IOException exception) {
            SPBRevamped.LOGGER.warn("Failed to read level959 start pool for debug HUD", exception);
            return "read error";
        }
    }

    private String findLatestLevel959Error(Path logPath) {
        if (!Files.exists(logPath)) {
            return "latest.log missing";
        }

        try {
            List<String> allLines = Files.readAllLines(logPath);
            for (int i = allLines.size() - 1; i >= 0; i--) {
                String line = allLines.get(i);
                if (ERROR_PATTERN.matcher(line).matches()) {
                    int marker = line.indexOf("] ");
                    return marker >= 0 ? line.substring(marker + 2).trim() : line.trim();
                }
            }
            return "no recent level959/jigsaw error";
        } catch (IOException exception) {
            SPBRevamped.LOGGER.warn("Failed to read latest.log for level959 debug HUD", exception);
            return "log read error";
        }
    }
}
