package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.gene.GeneCapability;
import com.carrot123.yixiangshouronglu.menu.GeneModificationMenu;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class GeneClientPackets {
    private GeneClientPackets() {}
    public static void skillResult(GeneNetwork.SkillResult result) {
        var player = Minecraft.getInstance().player;
        if (player != null) { player.displayClientMessage(Component.translatable("gene.yixiangshouronglu.skill.result." + result.status()), true); }
    }
    public static void apply(GeneNetwork.Snapshot snapshot) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            GeneCapability.get(player).load(snapshot.data());
            if (player.containerMenu instanceof GeneModificationMenu menu) { menu.status = snapshot.status(); }
        }
    }
}
