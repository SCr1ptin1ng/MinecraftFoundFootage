package com.sp.world.generation.chunk_generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sp.SPBRevamped;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Level959ChunkGenerator extends BackroomsChunkGenerator {
    private static final int ANCHOR_GRID_SPACING_CHUNKS = 24;
    private static final int START_DEPTH = 40;
    private static final BlockPos START_POS = new BlockPos(0, 32, 0);
    private static final int START_CHUNK_X = START_POS.getX() >> 4;
    private static final int START_CHUNK_Z = START_POS.getZ() >> 4;
    private static final Identifier START_TEMPLATE = id("level959/959room1");
    private static final Identifier START_JIGSAW_NAME = id("level959/start");
    private static final RegistryKey<StructurePool> START_POOL_KEY = RegistryKey.of(RegistryKeys.TEMPLATE_POOL, START_JIGSAW_NAME);
    private static final Set<Long> GENERATED_ANCHORS = ConcurrentHashMap.newKeySet();
    public static final Codec<Level959ChunkGenerator> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource)
            ).apply(instance, instance.stable(Level959ChunkGenerator::new))
    );

    public Level959ChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource, 5);
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void generate(StructureWorldAccess world, Chunk chunk) {
        ServerWorld serverWorld = world.toServerWorld();
        ChunkPos chunkPos = chunk.getPos();
        if (!isAnchorChunk(chunkPos.x, chunkPos.z)) {
            return;
        }

        BlockPos anchorPos = new BlockPos(chunkPos.getStartX(), START_POS.getY(), chunkPos.getStartZ());
        ensureAnchorGenerated(serverWorld, anchorPos, false);
    }

    @Override
    public boolean shouldGenerateAtStatus(ChunkStatus status) {
        return status == ChunkStatus.FEATURES;
    }

    public static boolean bootstrap(ServerWorld world) {
        MinecraftServer server = world.getServer();
        if (server == null) {
            return false;
        }

        Registry<StructurePool> templatePools = world.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL);
        RegistryEntry.Reference<StructurePool> startPool = templatePools.getEntry(START_POOL_KEY).orElse(null);
        if (startPool == null) {
            SPBRevamped.LOGGER.warn("Missing level959 start pool {}", START_POOL_KEY.getValue());
            return placeStartTemplate(world, server, START_POS);
        }

        GENERATED_ANCHORS.clear();
        boolean generated = ensureAnchorGenerated(world, START_POS, true);
        SPBRevamped.LOGGER.info("Level959 region bootstrap primed center anchor at {}", START_POS);
        return generated;
    }

    private static Identifier id(String path) {
        return new Identifier(SPBRevamped.MOD_ID, path);
    }

    private static boolean isAnchorChunk(int chunkX, int chunkZ) {
        return Math.floorMod(chunkX - START_CHUNK_X, ANCHOR_GRID_SPACING_CHUNKS) == 0
                && Math.floorMod(chunkZ - START_CHUNK_Z, ANCHOR_GRID_SPACING_CHUNKS) == 0;
    }

    private static boolean ensureAnchorGenerated(ServerWorld world, BlockPos anchorPos, boolean forceCenterFallback) {
        long key = ChunkPos.toLong(anchorPos.getX() >> 4, anchorPos.getZ() >> 4);
        if (!GENERATED_ANCHORS.add(key)) {
            return true;
        }

        MinecraftServer server = world.getServer();
        if (server == null) {
            GENERATED_ANCHORS.remove(key);
            return false;
        }

        Registry<StructurePool> templatePools = world.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL);
        RegistryEntry.Reference<StructurePool> startPool = templatePools.getEntry(START_POOL_KEY).orElse(null);
        if (startPool == null) {
            GENERATED_ANCHORS.remove(key);
            return forceCenterFallback && anchorPos.equals(START_POS) && placeStartTemplate(world, server, anchorPos);
        }

        try {
            boolean generated = StructurePoolBasedGenerator.generate(world, startPool, START_JIGSAW_NAME, START_DEPTH, anchorPos, false);
            if (generated) {
                SPBRevamped.LOGGER.info("Level959 generated anchor from start pool {} at {}", START_POOL_KEY.getValue(), anchorPos);
                return true;
            }

            if (forceCenterFallback && anchorPos.equals(START_POS)) {
                SPBRevamped.LOGGER.warn("Level959 center anchor jigsaw returned false, falling back to direct room1 placement");
                return placeStartTemplate(world, server, anchorPos);
            }

            SPBRevamped.LOGGER.warn("Level959 anchor jigsaw returned false at {}", anchorPos);
            GENERATED_ANCHORS.remove(key);
            return false;
        } catch (Exception exception) {
            if (forceCenterFallback && anchorPos.equals(START_POS)) {
                SPBRevamped.LOGGER.error("Level959 center anchor jigsaw failed at {}", anchorPos, exception);
                return placeStartTemplate(world, server, anchorPos);
            }

            SPBRevamped.LOGGER.error("Level959 anchor jigsaw failed at {}", anchorPos, exception);
            GENERATED_ANCHORS.remove(key);
            return false;
        }
    }

    private static boolean placeStartTemplate(ServerWorld world, MinecraftServer server, BlockPos anchorPos) {
        StructureTemplateManager templateManager = server.getStructureTemplateManager();
        Optional<StructureTemplate> optional = templateManager.getTemplate(START_TEMPLATE);
        if (optional.isEmpty()) {
            SPBRevamped.LOGGER.warn("Missing level959 start template {}", START_TEMPLATE);
            return false;
        }

        StructurePlacementData placement = new StructurePlacementData()
                .setMirror(BlockMirror.NONE)
                .setRotation(BlockRotation.NONE)
                .setIgnoreEntities(true);

        try {
            optional.get().place(world, anchorPos, anchorPos, placement, Random.create(world.getSeed() ^ 0x959L ^ anchorPos.asLong()), 2);
            SPBRevamped.LOGGER.info("Placed fallback level959 start template {} at {}", START_TEMPLATE, anchorPos);
            return true;
        } catch (Exception exception) {
            SPBRevamped.LOGGER.error("Failed placing fallback level959 start template {} at {}", START_TEMPLATE, anchorPos, exception);
            return false;
        }
    }
}
