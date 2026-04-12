package com.sp.world.generation.chunk_generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sp.SPBRevamped;
import com.sp.entity.custom.FacelingEntity;
import com.sp.init.ModBlocks;
import com.sp.init.ModEntities;
import com.sp.init.ModItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class Level324ChunkGenerator extends BackroomsChunkGenerator {
    public static final boolean FACELING_ENABLED = false;
    // Tweak this position after testing to place the Faceling exactly where you want inside the store.
    public static final BlockPos FACELING_SPAWN_POS = new BlockPos(37, 65, 5);
    public static final float FACELING_SPAWN_YAW = 0.0F;
    public static final BlockPos GAS_STATION_ORIGIN = new BlockPos(8, -2, 0);
    private static final Identifier GAS_STATION_TEMPLATE_ID = new Identifier(SPBRevamped.MOD_ID, "level324/gas_station_324");
    private static final Identifier GAS_STATION_TEMPLATE_FALLBACK_ID = new Identifier(SPBRevamped.MOD_ID, "level324/gas_station");
    private static final BlockPos[] BATTERY_OFFSETS = new BlockPos[] {
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1),
            new BlockPos(1, 0, 1),
            new BlockPos(-1, 0, -1)
    };

    public static final Codec<Level324ChunkGenerator> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
                            ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(generator -> generator.settings)
                    )
                    .apply(instance, instance.stable(Level324ChunkGenerator::new))
    );

    private final RegistryEntry<ChunkGeneratorSettings> settings;

    private final Random random;

    public Level324ChunkGenerator(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings) {
        super(biomeSource, 10);
        this.settings = settings;
        this.random = Random.create();
    }

    public static void ensureFacelingPresent(ServerWorld world) {
        if (!FACELING_ENABLED) {
            removeFacelings(world);
            return;
        }

        world.getChunk(FACELING_SPAWN_POS);

        boolean alreadySpawned = !world.getEntitiesByClass(FacelingEntity.class, new net.minecraft.util.math.Box(FACELING_SPAWN_POS).expand(8.0), entity -> true).isEmpty();
        if (alreadySpawned) {
            return;
        }

        FacelingEntity faceling = ModEntities.FACELING_ENTITY.create(world);
        if (faceling != null) {
            faceling.setPersistent();
            faceling.refreshPositionAndAngles(FACELING_SPAWN_POS.getX() + 0.5, FACELING_SPAWN_POS.getY(), FACELING_SPAWN_POS.getZ() + 0.5, FACELING_SPAWN_YAW, 0.0F);
            world.spawnEntity(faceling);
        }
    }

    public static void removeFacelings(ServerWorld world) {
        for (FacelingEntity faceling : world.getEntitiesByClass(FacelingEntity.class, new net.minecraft.util.math.Box(FACELING_SPAWN_POS).expand(256.0), entity -> true)) {
            faceling.discard();
        }
    }

    public static void spawnBatteryNearFaceling(ServerWorld world) {
        world.getChunk(FACELING_SPAWN_POS);

        BlockPos offset = BATTERY_OFFSETS[Math.floorMod((int) (world.getTime() + world.getSeed()), BATTERY_OFFSETS.length)];
        BlockPos spawnPos = FACELING_SPAWN_POS.add(offset);

        ItemEntity battery = new ItemEntity(
                world,
                spawnPos.getX() + 0.5,
                spawnPos.getY() + 0.35,
                spawnPos.getZ() + 0.5,
                new ItemStack(ModItems.BATTERY)
        );
        battery.setToDefaultPickupDelay();
        battery.setVelocity(0.0, 0.0, 0.0);
        world.spawnEntity(battery);
    }

    private Optional<StructureTemplate> getGasStationTemplate(StructureTemplateManager structureTemplateManager) {
        Optional<StructureTemplate> primary = structureTemplateManager.getTemplate(GAS_STATION_TEMPLATE_ID);
        if (primary.isPresent()) {
            return primary;
        }

        Optional<StructureTemplate> fallback = structureTemplateManager.getTemplate(GAS_STATION_TEMPLATE_FALLBACK_ID);
        if (fallback.isPresent()) {
            SPBRevamped.LOGGER.warn("Missing structure template '{}', falling back to '{}'.",
                    GAS_STATION_TEMPLATE_ID, GAS_STATION_TEMPLATE_FALLBACK_ID);
        } else {
            SPBRevamped.LOGGER.error("Missing both gas station templates '{}' and '{}'.",
                    GAS_STATION_TEMPLATE_ID, GAS_STATION_TEMPLATE_FALLBACK_ID);
        }

        return fallback;
    }

    @Override
    public void generate(StructureWorldAccess world, Chunk chunk) {
        int x = chunk.getPos().getStartX();
        int z = chunk.getPos().getStartZ();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        StructureTemplateManager structureTemplateManager = world.getServer().getStructureTemplateManager();

        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                if (!((z + j > -1 && z + j < 27) && (x + i > 7 && x + i < 56))) {
                    if (i == 8 && j == 7) {
                        BlockPos placementPos = mutable.set(x + i, 4, z + j);

                        StructurePlacementData structurePlacementData = new StructurePlacementData();
                        structurePlacementData.setMirror(BlockMirror.NONE).setRotation(BlockRotation.NONE).setIgnoreEntities(true);

                        Optional<StructureTemplate> optional = structureTemplateManager.getTemplate(new Identifier(SPBRevamped.MOD_ID, "level324/hanging_lamp" + (random.nextBetween(0, 5) == 0 ? "_on" : "_off")));

                        optional.ifPresent(structureTemplate -> structureTemplate.place(
                                world,
                                placementPos,
                                placementPos,
                                structurePlacementData, random, 2));
                    }

                    if ((chunk.getPos().getStartX() + i) % 1000 == 9) {
                        if ((chunk.getPos().getStartZ() + j) % 21 == 0) {
                            BlockPos placementPos = mutable.set(x + i, 65, z + j);
                            StructurePlacementData structurePlacementData = new StructurePlacementData();
                            structurePlacementData.setMirror(BlockMirror.NONE).setRotation(BlockRotation.NONE).setIgnoreEntities(true);

                            Optional<StructureTemplate> optional = structureTemplateManager.getTemplate(new Identifier(SPBRevamped.MOD_ID, "inf_grass/utility_pole"));

                            optional.ifPresent(structureTemplate -> structureTemplate.place(
                                    world,
                                    placementPos,
                                    placementPos,
                                    structurePlacementData, random, 2));

                        }
                    }
                }

                if ((chunk.getPos().getStartX() + i) == 8) {
                    if ((chunk.getPos().getStartZ() + j) == 0) {

                        BlockPos placementPos = mutable.set(x + i, -2, z + j);
                        StructurePlacementData structurePlacementData = new StructurePlacementData();
                        structurePlacementData.setMirror(BlockMirror.NONE).setRotation(BlockRotation.NONE).setIgnoreEntities(true);

                        Optional<StructureTemplate> optional = getGasStationTemplate(structureTemplateManager);

                        optional.ifPresent(structureTemplate -> structureTemplate.place(
                                world,
                                placementPos,
                                placementPos,
                                structurePlacementData, random, 2));

                        if (FACELING_SPAWN_POS.getX() >= x && FACELING_SPAWN_POS.getX() < x + 16 && FACELING_SPAWN_POS.getZ() >= z && FACELING_SPAWN_POS.getZ() < z + 16) {
                            ensureFacelingPresent(world.toServerWorld());
                        }
                    }
                }
            }
        }
    }

    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        int x = chunk.getPos().getStartX();
        int z = chunk.getPos().getStartZ();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                if (!((z + j > -1 && z + j < 27) && (x + i > 7 && x + i < 55))) {
                    chunk.setBlockState(mutable.set(i, 0, j), ModBlocks.CONCRETE_BLOCK_11.getDefaultState(), false);

                    chunk.setBlockState(mutable.set(i, 63, j), ModBlocks.CONCRETE_BLOCK_11.getDefaultState(), false);

                    if ((i + Math.abs(x * 16)) % 1000 < 8/* || (j + Math.abs(chunk.getPos().z * 16)) % 1000 < 8*/) {

                        chunk.setBlockState(mutable.set(i, 64, j), ModBlocks.ROAD.getDefaultState(), false);
                    } else {
                        chunk.setBlockState(mutable.set(i, 64, j), ModBlocks.RED_DIRT.getDefaultState(), false);
                    }
                }
            }
        }

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 1; k < 63; k++) {
                    chunk.setBlockState(mutable.set(i, k, j), ModBlocks.CONCRETE_BLOCK_11.getDefaultState(), false);
                }
            }

            for (int j = 14; j < 16; j++) {
                for (int k = 1; k < 63; k++) {
                    chunk.setBlockState(mutable.set(i, k, j), ModBlocks.CONCRETE_BLOCK_11.getDefaultState(), false);
                }
            }
        }

        for (int i = 14; i < 16; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 1; k < 63; k++) {
                    chunk.setBlockState(mutable.set(i, k, j), ModBlocks.CONCRETE_BLOCK_11.getDefaultState(), false);
                }
            }

            for (int j = 14; j < 16; j++) {
                for (int k = 1; k < 63; k++) {
                    chunk.setBlockState(mutable.set(i, k, j), ModBlocks.CONCRETE_BLOCK_11.getDefaultState(), false);
                }
            }
        }


        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }
}
