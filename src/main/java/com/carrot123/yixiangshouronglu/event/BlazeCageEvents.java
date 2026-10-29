package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlazeCageEvents {
    private static final ResourceLocation RODLING =
            new ResourceLocation("mutantmore", "rodling");

    private static final ResourceLocation MUTANT_BLAZE =
            new ResourceLocation("mutantmore", "mutant_blaze");

    private BlazeCageEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        ItemStack held = event.getEntity().getMainHandItem();

        if (!held.is(ModItems.BLAZE_CAGE.get())) {
            return;
        }

        Entity target = event.getTarget();
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());

        if (!RODLING.equals(entityId) || !isTamed(target)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (!ForgeRegistries.ENTITY_TYPES.containsKey(MUTANT_BLAZE)) {
            return;
        }

        ItemStack soul = SoulItem.create(ModItems.SOUL.get(), MUTANT_BLAZE);

        ItemEntity droppedSoul = new ItemEntity(
                level,
                target.getX(),
                target.getY() + 0.25D,
                target.getZ(),
                soul
        );

        if (!event.getEntity().getAbilities().instabuild) {
            held.shrink(1);
        }

        target.discard();
        level.addFreshEntity(droppedSoul);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static boolean isTamed(Entity entity) {
        if (entity instanceof TamableAnimal tamable) {
            return tamable.isTame();
        }

        if (entity instanceof OwnableEntity ownable) {
            return ownable.getOwnerUUID() != null;
        }

        return false;
    }
}
