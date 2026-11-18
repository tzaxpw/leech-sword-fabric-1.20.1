package com.yuheng.leech.handler;

import com.yuheng.leech.item.LeechSwordItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.player.PlayerEntity;

public final class LeechBlockDamageHandler {
    private static boolean reentryBypass = false;

    private LeechBlockDamageHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (reentryBypass) {
                return true;
            }

            if (!(entity instanceof PlayerEntity player)) {
                return true;
            }

            if (!player.isUsingItem() || !LeechSwordItem.isLeechSwordActiveItem(player)) {
                return true;
            }

            if (amount <= 0.0F) {
                return true;
            }

            try {
                reentryBypass = true;
                entity.damage(source, amount * 0.35F);
            } finally {
                reentryBypass = false;
            }

            // Cancel the original hit; the call above reapplies 35% of its damage.
            return false;
        });
    }
}
