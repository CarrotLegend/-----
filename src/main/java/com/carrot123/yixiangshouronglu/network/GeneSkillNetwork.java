package com.carrot123.yixiangshouronglu.network;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.event.BiologicalGeneModuleEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class GeneSkillNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "gene_skill"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private GeneSkillNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, ActivateFallenForm.class, (message, buffer) -> {}, buffer -> new ActivateFallenForm(),
                (message, context) -> {
                    var packet = context.get();
                    var player = packet.getSender();
                    if (player != null) {
                        packet.enqueueWork(() -> BiologicalGeneModuleEffects.activateFallenForm(player));
                    }
                    packet.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void requestFallenForm() {
        CHANNEL.sendToServer(new ActivateFallenForm());
    }

    private record ActivateFallenForm() {}
}
