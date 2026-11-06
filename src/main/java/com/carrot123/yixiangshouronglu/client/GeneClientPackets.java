package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.gene.GeneCapability;
import com.carrot123.yixiangshouronglu.gene.GeneSkills;
import com.carrot123.yixiangshouronglu.menu.GeneModificationMenu;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class GeneClientPackets {
    private GeneClientPackets() {}
    public static void skillResult(GeneNetwork.SkillResult result) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            String status = result.status();
            if (status.equals("ok")) {
                var skill = GeneSkills.available(GeneCapability.get(player)).stream().filter(value -> value.id().equals(result.skill())).findFirst();
                if (skill.isPresent()) {
                    player.displayClientMessage(Component.translatable("gene.yixiangshouronglu.skill.activated", Component.translatable(skill.get().nameKey())), true);
                    return;
                }
            }
            player.displayClientMessage(Component.translatable("gene.yixiangshouronglu.skill.result." + status), true);
        }
    }
    public static void apply(GeneNetwork.Snapshot snapshot) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            GeneCapability.get(player).load(snapshot.data());
            if (player.containerMenu instanceof GeneModificationMenu menu) {
                menu.status = snapshot.status();
                menu.capacityUpgrade = snapshot.capacityUpgrade();
            }
            if (snapshot.status().startsWith("capacity_upgrade_")) {
                player.displayClientMessage(Component.translatable("gene.yixiangshouronglu." + snapshot.status()), true);
            }
        }
    }
}
