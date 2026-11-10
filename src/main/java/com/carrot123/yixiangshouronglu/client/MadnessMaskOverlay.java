package com.carrot123.yixiangshouronglu.client;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MadnessMaskOverlay {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?#@$%&*+-/\\";
    private static final RandomSource RANDOM = RandomSource.create();
    private static String text = "";
    private static long lastTick = Long.MIN_VALUE;

    private MadnessMaskOverlay() {
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("madness_mask_text", (gui, graphics, partialTick, width, height) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
                return;
            }

            boolean wearing = CuriosApi.getCuriosInventory(minecraft.player)
                    .resolve()
                    .flatMap(inventory -> inventory.getStacksHandler("head"))
                    .map(handler -> {
                        for (int slot = 0; slot < handler.getStacks().getSlots(); slot++) {
                            if (handler.getStacks().getStackInSlot(slot).is(ModItems.MADNESS_MASK.get())) {
                                return true;
                            }
                        }
                        return false;
                    }).orElse(false);

            if (!wearing) {
                text = "";
                lastTick = Long.MIN_VALUE;
                return;
            }

            long tick = minecraft.level.getGameTime();
            if (tick != lastTick && (lastTick == Long.MIN_VALUE || tick < lastTick || tick - lastTick >= 2L)) {
                lastTick = tick;
                int length = 4 + RANDOM.nextInt(28);
                StringBuilder builder = new StringBuilder(length);
                for (int i = 0; i < length; i++) {
                    builder.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
                }
                text = builder.toString();
            }

            graphics.drawCenteredString(minecraft.font, text, width / 2, height - 77, 0xFFCA64);
        });
    }
}
