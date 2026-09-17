package com.yuheng.leech.handler;

import com.yuheng.leech.util.LeechUtil;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class LeechInventoryHealthHandler {
    private static final UUID HEALTH_UUID = UUID.fromString("8f6c8c4a-0b7d-4e6b-9f11-9a6d2c4e1f77");

    private LeechInventoryHealthHandler() {
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            updatePlayer(player);
        }
    }

    public static void updatePlayer(ServerPlayerEntity player) {
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        if (LeechUtil.hasLeechSword(player)) {
            if (maxHealth.getModifier(HEALTH_UUID) == null) {
                maxHealth.addPersistentModifier(new EntityAttributeModifier(
                        HEALTH_UUID,
                        "Leech inventory health",
                        20.0D,
                        EntityAttributeModifier.Operation.ADDITION
                ));
            }
        } else {
            maxHealth.removeModifier(HEALTH_UUID);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }
}
