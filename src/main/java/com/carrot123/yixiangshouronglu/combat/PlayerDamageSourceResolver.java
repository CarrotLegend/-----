package com.carrot123.yixiangshouronglu.combat;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;

final class PlayerDamageSourceResolver {
    private static final int MAX_OWNER_DEPTH = 8;

    private PlayerDamageSourceResolver() {
    }

    static ServerPlayer resolve(DamageSource source) {
        if (source == null) {
            return null;
        }
        Set<Entity> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ServerPlayer player = resolveOwner(source.getEntity(), visited, 0);
        return player != null ? player : resolveOwner(source.getDirectEntity(), visited, 0);
    }

    private static ServerPlayer resolveOwner(Entity entity, Set<Entity> visited, int depth) {
        if (entity == null || depth > MAX_OWNER_DEPTH || !visited.add(entity)) {
            return null;
        }
        if (entity instanceof ServerPlayer player) {
            return player;
        }
        if (entity instanceof Projectile projectile) {
            ServerPlayer player = resolveOwner(projectile.getOwner(), visited, depth + 1);
            if (player != null) {
                return player;
            }
        }
        if (entity instanceof OwnableEntity ownable) {
            return resolveOwner(ownable.getOwner(), visited, depth + 1);
        }
        return null;
    }
}
