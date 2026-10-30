
package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@Mod.EventBusSubscriber(
        modid = AnomalousContainmentChronicles.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RefinedDragonMealEvents {
    private static final int REQUIRED_AGE_TICKS = 100 * 24000;

    private static final Set<ResourceLocation> DRAGONS = Set.of(
            new ResourceLocation("iceandfire", "fire_dragon"),
            new ResourceLocation("iceandfire", "ice_dragon"),
            new ResourceLocation("iceandfire", "lightning_dragon")
    );

    private RefinedDragonMealEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        handle(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handle(event, event.getTarget());
    }

    private static void handle(PlayerInteractEvent event, Entity clicked) {
        if (event.isCanceled()) {
            return;
        }

        ItemStack stack = event.getItemStack();

        if (!stack.is(ModItems.REFINED_DRAGON_MEAL.get())) {
            return;
        }

        Entity target = resolveDragon(clicked);

        if (target == null || target.isRemoved()) {
            return;
        }

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());

        if (entityId == null || !DRAGONS.contains(entityId)) {
            return;
        }

        if (!(target instanceof TamableAnimal dragon)) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (event.getLevel().isClientSide) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!dragon.isTame() || !dragon.isOwnedBy(player)) {
            player.displayClientMessage(
                    Component.translatable("message.yixiangshouronglu.dragon_not_owned"),
                    true
            );
            return;
        }

        CompoundTag dragonData = dragon.saveWithoutId(new CompoundTag());

        if (!dragon.isAlive() || dragonData.getBoolean("ModelDead")) {
            player.displayClientMessage(
                    Component.translatable("message.yixiangshouronglu.dragon_not_alive"),
                    true
            );
            return;
        }

        if (dragonData.getInt("AgeTicks") < REQUIRED_AGE_TICKS) {
            player.displayClientMessage(
                    Component.translatable("message.yixiangshouronglu.dragon_not_mature"),
                    true
            );
            return;
        }

        ItemStack soul = SoulItem.create(ModItems.SOUL.get(), entityId);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        dragon.ejectPassengers();
        dragon.discard();

        if (!player.getInventory().add(soul)) {
            player.drop(soul, false);
        }

        player.getInventory().setChanged();

        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS,
                1.0F,
                1.25F
        );

        player.displayClientMessage(
                Component.translatable("message.yixiangshouronglu.dragon_soul_obtained"),
                true
        );
    }

    private static Entity resolveDragon(Entity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        if (id == null) {
            return entity;
        }

        if (!id.equals(new ResourceLocation("iceandfire", "dragon_multipart"))) {
            return entity;
        }

        try {
            Object parent = entity.getClass().getMethod("getParent").invoke(entity);

            if (parent instanceof Entity parentEntity) {
                return parentEntity;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return entity;
    }
}
