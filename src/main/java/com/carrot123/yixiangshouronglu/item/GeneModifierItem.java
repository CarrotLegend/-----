package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.gene.research.GeneResearchDefinition;
import com.carrot123.yixiangshouronglu.gene.research.GeneResearchService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "yixiangshouronglu", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneModifierItem extends Item {
    private final GeneResearchDefinition definition;

    public GeneModifierItem(Properties properties, GeneResearchDefinition definition) {
        super(properties);
        this.definition = definition;
    }

    public GeneResearchDefinition definition() {
        return definition;
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof ServerPlayer serverPlayer && GeneResearchService.use(serverPlayer, definition, stack)) {
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @SubscribeEvent public static void useInCreative(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isCreative()
                || !(event.getItemStack().getItem() instanceof GeneModifierItem item)) {
            return;
        }
        var result = item.use(event.getLevel(), player, event.getHand());
        if (result.getObject().isEmpty()) {
            player.setItemInHand(event.getHand(), ItemStack.EMPTY);
        }
        event.setCancellationResult(result.getResult());
        event.setCanceled(true);
        player.inventoryMenu.sendAllDataToRemote();
    }
}
