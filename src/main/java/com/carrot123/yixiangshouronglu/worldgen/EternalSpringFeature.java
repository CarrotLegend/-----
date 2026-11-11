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

import java.util.HashSet;
import java.util.Set;

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
        BlockPos surface = context.origin().below();
        int localX = surface.getX() & 15;
        int localZ = surface.getZ() & 15;
        if (!level.getLevel().dimension().equals(Level.OVERWORLD) || localX < 1 || localX > 9 || localZ < 1 || localZ > 9
                || level.isOutsideBuildHeight(surface.below(3)) || level.isOutsideBuildHeight(surface.above(3))) {
            return false;
        }

        BlockPos origin = surface.below(2);
        if (!level.ensureCanWrite(origin) || !level.ensureCanWrite(origin.offset(5, 3, 5))) {
            return false;
        }
        StructureTemplate template = level.getLevel().getStructureManager().get(TEMPLATE).orElse(null);
        if (template == null || !SIZE.equals(template.getSize())) {
            return false;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true).setKeepLiquids(false)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        var sources = template.filterBlocks(origin, settings, ModBlocks.ETERNAL_SPRING_BLOCK.get());
        Set<BlockPos> pool = new HashSet<>();
        for (var source : sources) {
            if (source.pos().getY() == surface.getY()) {
                pool.add(source.pos());
            }
        }
        if (sources.size() != 16 || !fitsTerrain(level, surface, pool)) {
            return false;
        }
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

    private static boolean fitsTerrain(WorldGenLevel level, BlockPos surface, Set<BlockPos> pool) {
        for (BlockPos ground : BlockPos.betweenClosed(surface, surface.offset(5, 0, 5))) {
            if (!level.getBiome(ground).is(BIOMES)
                    || level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, ground.getX(), ground.getZ()) != ground.getY() + 1) {
                return false;
            }
            for (int depth = 0; depth <= 3; depth++) {
                BlockPos pos = ground.below(depth);
                BlockState state = level.getBlockState(pos);
                if ((!state.is(BlockTags.DIRT) && (depth == 0 || !state.is(BlockTags.BASE_STONE_OVERWORLD)))
                        || !state.getFluidState().isEmpty() || state.hasBlockEntity()
                        || !state.isFaceSturdy(level, pos, Direction.UP)) {
                    return false;
                }
            }
            BlockState above = level.getBlockState(ground.above());
            if ((!above.isAir() && (pool.contains(ground) || !(above.is(Blocks.GRASS) || above.is(Blocks.FERN))))
                    || !level.isEmptyBlock(ground.above(2)) || !level.isEmptyBlock(ground.above(3))) {
                return false;
            }
        }
        return true;
    }
}
