package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.gene.research.GeneResearchCapability;
import com.carrot123.yixiangshouronglu.item.GeneModifierItem;
import com.carrot123.yixiangshouronglu.menu.GeneResearchTableMenu;
import com.carrot123.yixiangshouronglu.network.GeneResearchNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "yixiangshouronglu", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneResearchClient {
    private GeneResearchClient() {}

    public static void apply(GeneResearchNetwork.PlayerState packet) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            var data = GeneResearchCapability.get(player);
            if (data != null) {
                data.load(packet.data());
            }
        }
    }

    public static void apply(GeneResearchNetwork.MenuState packet) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof GeneResearchTableMenu menu) {
            menu.apply(packet);
        }
    }

    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof GeneModifierItem item) {
            var player = Minecraft.getInstance().player;
            var data = player == null ? null : GeneResearchCapability.get(player);
            var definition = item.definition();
            event.getToolTip().add(Component.translatable("tooltip.yixiangshouronglu.gene_modifier.load",
                    data == null ? 0 : data.uses(definition), definition.limit()).withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable(definition.effectKey()).withStyle(ChatFormatting.GOLD));
        }
    }
}
