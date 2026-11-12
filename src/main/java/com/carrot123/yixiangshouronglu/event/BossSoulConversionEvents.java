package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossSoulConversionEvents {
    private static final Map<ResourceLocation, ResourceLocation> CONVERSIONS = Map.of(
            new ResourceLocation("cataclysm", "the_baby_leviathan"), new ResourceLocation("cataclysm", "the_leviathan"),
            new ResourceLocation("cataclysm", "modern_remnant"), new ResourceLocation("cataclysm", "ancient_remnant"),
            new ResourceLocation("cataclysm", "netherite_ministrosity"), new ResourceLocation("cataclysm", "netherite_monstrosity")
    );

    private BossSoulConversionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        convert(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        convert(event, event.getTarget());
    }

    private static void convert(PlayerInteractEvent event, Entity target) {
        ItemStack held = event.getItemStack();
        if (event.isCanceled() || event.getEntity().isSpectator() || !held.is(Items.NETHER_STAR)
                || !target.isAlive() || target.isRemoved()) {
            return;
        }

        ResourceLocation targetId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        ResourceLocation bossId = targetId == null ? null : CONVERSIONS.get(targetId);
        if (bossId == null || !ForgeRegistries.ENTITY_TYPES.containsKey(bossId)) {
            return;
        }

        boolean clientSide = event.getLevel().isClientSide;
        event.setCancellationResult(InteractionResult.sidedSuccess(clientSide));
        event.setCanceled(true);
        if (clientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack soul = SoulItem.create(ModItems.SOUL.get(), bossId);
        target.discard();
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        var inventory = player.getInventory();
        if (inventory.getSlotWithRemainingSpace(soul) >= 0 || inventory.getFreeSlot() >= 0) {
            inventory.add(soul);
        }
        if (!soul.isEmpty()) {
            player.drop(soul, false);
        }
        inventory.setChanged();
    }
}
