package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.network.GeneSkillNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, value = Dist.CLIENT)
public final class GeneSkillKeys {
    private static final KeyMapping FALLEN_FORM = new KeyMapping(
            "key.yixiangshouronglu.fallen_form", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.yixiangshouronglu.gene_skills");

    private GeneSkillKeys() {}

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            requestIfBound(InputConstants.getKey(event.getKey(), event.getScanCode()));
        }
    }

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Post event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            requestIfBound(InputConstants.Type.MOUSE.getOrCreate(event.getButton()));
        }
    }

    private static void requestIfBound(InputConstants.Key key) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen == null
                && minecraft.getConnection() != null && minecraft.player != null
                && FALLEN_FORM.isActiveAndMatches(key)) {
            GeneSkillNetwork.requestFallenForm();
        }
    }

    @Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(FALLEN_FORM);
        }
    }
}
