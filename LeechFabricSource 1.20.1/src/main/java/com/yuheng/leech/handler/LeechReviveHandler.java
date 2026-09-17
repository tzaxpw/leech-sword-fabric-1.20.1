package com.yuheng.leech.handler;

import com.yuheng.leech.item.LeechSwordItem;
import com.yuheng.leech.storage.LeechStorageState;
import com.yuheng.leech.util.LeechUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class LeechReviveHandler {
    public static final int REVIVE_COOLDOWN_TICKS = 5 * 60 * 20;
    private static final float REVIVE_HEALTH = 40.0F;
    private static final Set<UUID> VISIBLE_ACTION_BARS = new HashSet<>();

    private LeechReviveHandler() {
    }

    public static boolean tryRevive(ServerPlayerEntity player) {
        if (!LeechUtil.hasLeechSword(player)) {
            return false;
        }

        LeechStorageState state = LeechStorageState.get(player.getServer());
        if (state.getReviveCooldownTicks(player.getUuid()) > 0) {
            return false;
        }

        // Ensure the inventory health modifier exists before setHealth clamps the value.
        LeechInventoryHealthHandler.updatePlayer(player);
        player.setHealth(REVIVE_HEALTH);
        state.setReviveCooldownTicks(player.getUuid(), REVIVE_COOLDOWN_TICKS);

        if (LeechSwordItem.isLeechSwordMainHand(player)) {
            showCooldown(player, REVIVE_COOLDOWN_TICKS);
        }
        return true;
    }

    public static void tick(MinecraftServer server) {
        LeechStorageState state = LeechStorageState.get(server);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerUuid = player.getUuid();
            int remainingTicks = state.tickReviveCooldown(playerUuid);
            boolean shouldShow = remainingTicks > 0 && LeechSwordItem.isLeechSwordMainHand(player);

            if (shouldShow) {
                if (!VISIBLE_ACTION_BARS.contains(playerUuid) || remainingTicks % 20 == 0) {
                    showCooldown(player, remainingTicks);
                }
            } else if (VISIBLE_ACTION_BARS.remove(playerUuid)) {
                player.sendMessage(Text.empty(), true);
            }
        }
    }

    private static void showCooldown(ServerPlayerEntity player, int remainingTicks) {
        int totalSeconds = (remainingTicks + 19) / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String message = String.format(Locale.ROOT, "Revive Cooldown: %02d:%02d", minutes, seconds);

        player.sendMessage(Text.literal(message), true);
        VISIBLE_ACTION_BARS.add(player.getUuid());
    }
}
