package com.carrot123.yixiangshouronglu.entity.appearance;

import com.carrot123.yixiangshouronglu.entity.ShadowDemonEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public final class ShadowDemonAppearance {
    private ShadowDemonAppearance() {
    }

    public static void resolve(ServerLevel level, ShadowDemonEntity demon) {
        MinecraftServer server = level.getServer();
        Map<UUID, GameProfile> profiles = new HashMap<>();
        Path playerData = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        if (Files.isDirectory(playerData)) {
            try (Stream<Path> files = Files.list(playerData)) {
                files.filter(path -> path.getFileName().toString().endsWith(".dat")).forEach(path -> {
                    String fileName = path.getFileName().toString();
                    try {
                        UUID id = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                        GameProfile cached = server.getProfileCache().get(id).orElse(null);
                        profiles.put(id, cached != null ? cached : new GameProfile(id, null));
                    } catch (IllegalArgumentException ignored) {
                    }
                });
            } catch (IOException ignored) {
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            profiles.put(player.getUUID(), player.getGameProfile());
        }

        List<GameProfile> candidates = new ArrayList<>(profiles.values());
        UUID demonId = demon.getUUID();
        CompletableFuture.supplyAsync(() -> select(server, candidates))
                .exceptionally(error -> new Appearance(null, "", "", "", false)).thenAccept(appearance ->
                server.execute(() -> {
                    if (level.getEntity(demonId) instanceof ShadowDemonEntity current && current.isAlive()) {
                        current.setAppearance(appearance.uuid(), appearance.name(), appearance.value(),
                                appearance.signature(), appearance.slim());
                    }
                }));
    }

    private static Appearance select(MinecraftServer server, List<GameProfile> candidates) {
        List<Appearance> resolved = new ArrayList<>();
        for (GameProfile candidate : candidates) {
            try {
                GameProfile filled = server.getSessionService().fillProfileProperties(candidate, true);
                Property texture = filled.getProperties().get("textures").stream().findFirst().orElse(null);
                MinecraftProfileTexture skin = server.getSessionService().getTextures(filled, false)
                        .get(MinecraftProfileTexture.Type.SKIN);
                if (texture != null && skin != null) {
                    resolved.add(new Appearance(filled.getId(), safe(filled.getName()), texture.getValue(),
                            safe(texture.getSignature()), "slim".equals(skin.getMetadata("model"))));
                }
            } catch (Exception ignored) {
            }
        }
        if (!resolved.isEmpty()) {
            return resolved.get(ThreadLocalRandom.current().nextInt(resolved.size()));
        }
        if (!candidates.isEmpty()) {
            GameProfile fallback = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            return new Appearance(fallback.getId(), safe(fallback.getName()), "", "", false);
        }
        return new Appearance(null, "", "", "", false);
    }

    private record Appearance(UUID uuid, String name, String value, String signature, boolean slim) {
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
