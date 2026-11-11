package com.carrot123.yixiangshouronglu.worldgen;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.registry.ModBlocks;
import com.carrot123.yixiangshouronglu.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class EternalSpringFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceLocation TEMPLATE = new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "eternal_spring");
    private static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME,
            new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "has_feature/eternal_spring"));
    private static final Vec3i SIZE = new Vec3i(6, 4, 6);

    public EternalSpringFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos candidate = context.origin();
        int localX = candidate.getX() & 15;
        int localZ = candidate.getZ() & 15;

        if (!level.getLevel().dimension().equals(Level.OVERWORLD)
                || localX < 1 || localX > 9 || localZ < 1 || localZ > 9) {
            return false;
        }

        int centerX = candidate.getX() + 2;
        int centerZ = candidate.getZ() + 2;
        int groundY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, centerX, centerZ) - 1;
        BlockPos surface = new BlockPos(candidate.getX(), groundY, candidate.getZ());
        BlockPos origin = surface.below(2);

        if (level.isOutsideBuildHeight(origin)
                || level.isOutsideBuildHeight(origin.offset(5, 3, 5))
                || !level.ensureCanWrite(origin)
                || !level.ensureCanWrite(origin.offset(5, 3, 5))
                || !level.getBiome(new BlockPos(centerX, groundY, centerZ)).is(BIOMES)) {
            return false;
        }

        StructureTemplate template = level.getLevel().getStructureManager().get(TEMPLATE).orElse(null);
        if (template == null || !SIZE.equals(template.getSize())) {
            return false;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .setKeepLiquids(false)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        var sources = template.filterBlocks(origin, settings, ModBlocks.ETERNAL_SPRING_BLOCK.get());

        if (sources.size() != 16 || !fitsTerrain(level, surface)) {
            return false;
        }

        clearSurface(level, surface);

        if (!template.placeInWorld(level, origin, origin, settings, context.random(), Block.UPDATE_CLIENTS)) {
            return false;
        }

        for (var source : sources) {
            var fluid = level.getFluidState(source.pos());
            if (fluid.isSource() && fluid.is(ModFluids.ETERNAL_SPRING.get())) {
                level.scheduleTick(source.pos(), fluid.getType(), fluid.getType().getTickDelay(level));
            }
        }
        return true;
    }

    private static boolean fitsTerrain(WorldGenLevel level, BlockPos surface) {
        for (int dx = 0; dx < SIZE.getX(); dx++) {
            for (int dz = 0; dz < SIZE.getZ(); dz++) {
                int x = surface.getX() + dx;
                int z = surface.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;

                if (Math.abs(y - surface.getY()) > 1) {
                    return false;
                }

                BlockPos ground = new BlockPos(x, y, z);
                if (!level.getBlockState(ground).is(BlockTags.DIRT)) {
                    return false;
                }

                for (int depth = 0; depth < 3; depth++) {
                    BlockPos below = ground.below(depth);
                    BlockState state = level.getBlockState(below);
                    if (state.isAir() || !state.getFluidState().isEmpty()
                            || state.hasBlockEntity()
                            || !state.isFaceSturdy(level, below, Direction.UP)) {
                        return false;
                    }
                }

                for (int testY = y + 1; testY <= surface.getY() + 3; testY++) {
                    BlockPos above = new BlockPos(x, testY, z);
                    BlockState state = level.getBlockState(above);
                    if (!state.isAir() && (!state.canBeReplaced()
                            || !state.getFluidState().isEmpty() || state.hasBlockEntity())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void clearSurface(WorldGenLevel level, BlockPos surface) {
        for (int dx = 0; dx < SIZE.getX(); dx++) {
            for (int dz = 0; dz < SIZE.getZ(); dz++) {
                for (int y = surface.getY() + 1; y <= surface.getY() + 3; y++) {
                    BlockPos pos = new BlockPos(surface.getX() + dx, y, surface.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && (state.canBeReplaced()
                            || (y == surface.getY() + 1 && state.is(BlockTags.DIRT)))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }
}
