package com.carrot123.yixiangshouronglu.production;

import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.List;

public final class ChamberProduction {
    public static final int INTERVAL = 1200;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LootContextParamSet PARAMETERS = new LootContextParamSet.Builder()
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.DAMAGE_SOURCE)
            .build();

    private ChamberProduction() {
    }

    public static List<ItemStack> roll(ServerLevel level, BlockPos pos, String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id == null || !ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
            return List.of();
        }
        var type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        ResourceLocation tableId = type.getDefaultLootTable();
        if (BuiltInLootTables.EMPTY.equals(tableId)) {
            return List.of();
        }
        LootTable table = level.getServer().getLootData().getLootTable(tableId);
        if (table == LootTable.EMPTY) {
            return List.of();
        }

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                .create(PARAMETERS);
        try {
            return table.getRandomItems(params).stream()
                    .filter(stack -> !stack.isEmpty() && !stack.is(ModItems.SOUL.get())
                            && !stack.is(ModItems.BIOLOGICAL_RESEARCH_SAMPLE.get())
                            && !stack.is(ModItems.BIOLOGICAL_RESEARCH_RESULT.get()))
                    .map(ItemStack::copy)
                    .toList();
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not roll containment loot table {} for {} without an entity", tableId, id, exception);
            return List.of();
        }
    }
}
