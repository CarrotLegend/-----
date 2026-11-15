package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.BiologicalResearchItem;
import com.carrot123.yixiangshouronglu.item.ContainmentDeviceItem;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class ContainmentDeviceEvents {
    private static final Set<Entity> CAPTURING = Collections.newSetFromMap(new IdentityHashMap<>());

    private ContainmentDeviceEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) { capture(event, event.getTarget()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) { capture(event, event.getTarget()); }

    private static void capture(PlayerInteractEvent event, Entity clicked) {
        Entity target = clicked instanceof PartEntity<?> part ? part.getParent() : clicked;
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getEntity().isSpectator()
                || !event.getItemStack().is(ModItems.CONTAINMENT_DEVICE.get()) || !(target instanceof LivingEntity)
                || target instanceof Player || !target.isAlive() || target.isRemoved()) {
            return;
        }
        var id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (id == null || !id.equals(BiologicalResearchItem.entityId(ContainmentDeviceItem.stored(event.getItemStack()), true))) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel level) {
            if (!CAPTURING.add(target)) {
                return;
            }
            try {
                var soul = new ItemEntity(level, target.getX(), target.getY(), target.getZ(), SoulItem.create(ModItems.SOUL.get(), id));
                soul.setDefaultPickUpDelay();
                if (!level.addFreshEntity(soul)) {
                    return;
                }
                target.discard();
                level.playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.9F);
            } finally {
                CAPTURING.remove(target);
            }
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
