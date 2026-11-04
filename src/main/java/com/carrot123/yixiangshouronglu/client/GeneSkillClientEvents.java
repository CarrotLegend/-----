package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.gene.GeneCapability;
import com.carrot123.yixiangshouronglu.gene.GeneSkills;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, value = Dist.CLIENT)
public final class GeneSkillClientEvents {
    private static final KeyMapping ACTIVATE = new KeyMapping("key.yixiangshouronglu.gene_skill", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.yixiangshouronglu.genes");
    private static final GeneSkillKeyLatch LATCH = new GeneSkillKeyLatch();
    private GeneSkillClientEvents() {}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) { return; }
        Minecraft client = Minecraft.getInstance();
        boolean clicked = false;
        while (ACTIVATE.consumeClick()) { clicked = true; }
        if (!LATCH.press(clicked, ACTIVATE.isDown(), client.player != null && client.screen == null && client.isWindowActive())) { return; }
        var skills = GeneSkills.available(GeneCapability.get(client.player));
        if (skills.size() == 1) { GeneNetwork.CHANNEL.sendToServer(new GeneNetwork.SkillRequest(skills.get(0).id())); }
        else if (skills.size() > 1) { client.setScreen(new GeneSkillSelectionScreen(skills)); }
    }
    @Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(ACTIVATE); }
    }
}
