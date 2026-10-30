package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
public final class SaphyraGemEvents {
    private static final ResourceLocation HEALED_KINORA =
            new ResourceLocation("mokels_boss_mantyd", "mantydbosssiting");

    private static final ResourceLocation KINORA_SOUL =
            new ResourceLocation("mokels_boss_mantyd", "boss_mantyd");

    private SaphyraGemEvents() {
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
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        ItemStack held = event.getItemStack();

        if (!held.is(ModItems.SAPHYRA_GEM.get())) {
            return;
        }

        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());

        if (!HEALED_KINORA.equals(typeId) || target.isRemoved()) {
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

        ItemStack soul = SoulItem.create(ModItems.SOUL.get(), KINORA_SOUL);

        target.ejectPassengers();
        target.discard();

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

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
                1.0F
        );

        player.displayClientMessage(
                Component.translatable("message.yixiangshouronglu.kinora_soul_obtained"),
                true
        );
    }
}
