package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public final class LanternActivation {
    private LanternActivation() {
    }

    public static void play() {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        minecraft.particleEngine.createTrackingEmitter(player, ParticleTypes.TOTEM_OF_UNDYING, 30);
        minecraft.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE,
                player.getSoundSource(), 1.0F, 1.0F, false);
        minecraft.gameRenderer.displayItemActivation(new ItemStack(ModItems.SOUL_GUIDING_LANTERN.get()));
    }
}
