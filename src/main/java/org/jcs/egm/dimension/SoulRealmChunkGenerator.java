package org.jcs.egm.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SoulRealmChunkGenerator extends ChunkGenerator {
    public static final Codec<SoulRealmChunkGenerator> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    BiomeSource.CODEC.fieldOf("biome_source")
                            .forGetter(ChunkGenerator::getBiomeSource)
            ).apply(inst, SoulRealmChunkGenerator::new)
    );

    public SoulRealmChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    public Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {}

    // WORLD GEN

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();

        // Step 1: Generate small, patchy water spots
        boolean[][] isWater = new boolean[16][16];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                long localSeed = (((pos.x << 4) + x) * 341873128712L + ((pos.z << 4) + z) * 132897987541L) ^ 987654321L;
                Random cellRand = new Random(localSeed);
                isWater[x][z] = cellRand.nextFloat() < 0.12f; // Lower = less water
            }
        }
        // Only 1 spread pass for modestly-sized blobs
        boolean[][] spread = new boolean[16][16];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (isWater[x][z]) {
                    spread[x][z] = true;
                    // Direct neighbors
                    if (x > 0) spread[x - 1][z] = true;
                    if (x < 15) spread[x + 1][z] = true;
                    if (z > 0) spread[x][z - 1] = true;
                    if (z < 15) spread[x][z + 1] = true;
                }
            }
        }
        isWater = spread;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = (pos.x << 4) + x;
                int wz = (pos.z << 4) + z;

                // y=1: bedrock
                chunk.setBlockState(new BlockPos(wx, 1, wz), Blocks.BEDROCK.defaultBlockState(), false);

                // y=2..51: red sand
                for (int y = 2; y <= 51; y++) {
                    chunk.setBlockState(new BlockPos(wx, y, wz), Blocks.RED_SAND.defaultBlockState(), false);
                }

                // y=52: puddle or sand
                BlockState surfaceState = isWater[x][z] ? Blocks.WATER.defaultBlockState() : Blocks.RED_SAND.defaultBlockState();
                chunk.setBlockState(new BlockPos(wx, 52, wz), surfaceState, false);
            }
        }
    }



// WORLD GEN

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {}

    @Override
    public int getGenDepth() {
        return 0;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,
                                                        net.minecraft.world.level.levelgen.blending.Blender blender,
                                                        RandomState randomState,
                                                        StructureManager structureManager,
                                                        ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("SoulRealm: Oasis world (flat + puddles + underwater)");
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor accessor, RandomState random) {
        // Approximate: 73 average surface
        return 73;
    }

    @Override
    public int getSeaLevel() {
        return 73;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor accessor, RandomState random) {
        int minY = accessor.getMinBuildHeight();
        int height = accessor.getHeight();
        BlockState[] states = new BlockState[height];
        for (int i = 0; i < height; i++) states[i] = Blocks.WATER.defaultBlockState();
        return new NoiseColumn(minY, states);
    }
}
