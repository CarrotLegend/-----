package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.BiologicalResearchItem;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class BiologicalResearchSampleDropEvents {
    private static final String DROPPED = "yixiangshouronglu:research_sample_drop";
    private static long nextWarning;

    private BiologicalResearchSampleDropEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || entity instanceof Player || !entity.isDeadOrDying()
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
                || entity.getPersistentData().getBoolean(DROPPED)) {
            return;
        }
        var id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) {
            return;
        }
        try {
            CompoundTag snapshot = entity.saveWithoutId(new CompoundTag());
            snapshot.putString("id", id.toString());
            var stack = BiologicalResearchItem.sample(id, snapshot);
            if (BiologicalResearchItem.entityId(stack, false) != null) {
                ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stack);
                drop.setDefaultPickUpDelay();
                event.getDrops().add(drop);
                entity.getPersistentData().putBoolean(DROPPED, true);
                return;
            }
        } catch (RuntimeException | StackOverflowError exception) {
            warn(id, exception);
            return;
        }
        warn(id, null);
    }

    private static void warn(Object id, Throwable cause) {
        long now = System.nanoTime();
        if (nextWarning == 0 || now - nextWarning >= 0) {
            nextWarning = now + 60_000_000_000L;
            LogUtils.getLogger().warn("Skipping invalid or oversized biological research snapshot for {}", id, cause);
        }
    }
}
