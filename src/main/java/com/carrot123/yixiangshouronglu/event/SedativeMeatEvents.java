package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(
        modid = AnomalousContainmentChronicles.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SedativeMeatEvents {
    private static final ResourceLocation SCP_682_SOUL =
            new ResourceLocation("hardtodestroyreptile", "the_hard_to_destroy_reptile");

    private SedativeMeatEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        handle(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handle(event, event.getTarget());
    }

    private static void handle(PlayerInteractEvent event, Entity target) {
        if (event.isCanceled()) {
            return;
        }

        ItemStack held = event.getItemStack();

        if (!held.is(ModItems.SEDATIVE_MEAT.get()) || held.isEmpty()) {
            return;
        }

        if (!(target instanceof Mob) || target.isRemoved() || !target.isAlive()) {
            return;
        }

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());

        if (!isScp682(entityId)) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ItemStack soul = SoulItem.create(ModItems.SOUL.get(), SCP_682_SOUL);

        ItemEntity droppedSoul = new ItemEntity(
                level,
                target.getX(),
                target.getY() + 0.3D,
                target.getZ(),
                soul
        );

        if (!level.addFreshEntity(droppedSoul)) {
            return;
        }

        target.ejectPassengers();
        target.discard();

        if (!event.getEntity().getAbilities().instabuild) {
            held.shrink(1);
        }

        level.playSound(
                null,
                droppedSoul.blockPosition(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS,
                1.0F,
                0.8F
        );
    }

    private static boolean isScp682(ResourceLocation id) {
        if (id == null || !id.getNamespace().equals("hardtodestroyreptile")) {
            return false;
        }

        String path = id.getPath();

        return path.contains("reptile")
                || path.contains("scp682")
                || path.contains("scp_682");
    }
}
