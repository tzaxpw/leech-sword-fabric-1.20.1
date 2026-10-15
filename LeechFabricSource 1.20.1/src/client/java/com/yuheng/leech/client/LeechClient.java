package com.yuheng.leech.client;

import com.yuheng.leech.LeechMod;
import com.yuheng.leech.item.LeechSwordItem;
import com.yuheng.leech.net.LeechPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class LeechClient implements ClientModInitializer {
    private static KeyBinding summonSwordKey;
    private static KeyBinding clearLevitationKey;
    private static KeyBinding openStorageKey;

    @Override
    public void onInitializeClient() {
        summonSwordKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.leech.summon_sword",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.leech"
        ));

        clearLevitationKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.leech.clear_levitation",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.leech"
        ));

        openStorageKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.leech.open_storage",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.leech"
        ));

        // Resolve the blocking model per rendered entity/stack.
        // This also lets other players see the correct blocking model.
        FabricModelPredicateProviderRegistry.register(
                LeechMod.LEECH_SWORD,
                LeechMod.id("blocking"),
                (stack, world, entity, seed) ->
                        entity != null
                                && entity.isUsingItem()
                                && entity.getActiveItem() == stack
                                && stack.getItem() instanceof LeechSwordItem
                                ? 1.0F
                                : 0.0F
        );

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.currentScreen != null) {
            return;
        }

        while (summonSwordKey.wasPressed()) {
            ClientPlayNetworking.send(LeechPackets.SUMMON_SWORD, PacketByteBufs.empty());
        }

        while (clearLevitationKey.wasPressed()) {
            ClientPlayNetworking.send(LeechPackets.CLEAR_LEVITATION, PacketByteBufs.empty());
        }

        while (openStorageKey.wasPressed()) {
            ClientPlayNetworking.send(LeechPackets.OPEN_STORAGE, PacketByteBufs.empty());
        }

        // Reuse vanilla jump key for the air-jump mechanism.
        // Server-side per-player cooldown keeps it from firing every tick too fast.
        if (client.options.jumpKey.isPressed()
                && !client.player.isOnGround()
                && LeechSwordItem.isLeechSwordMainHand(client.player)) {
            ClientPlayNetworking.send(LeechPackets.AIR_JUMP, PacketByteBufs.empty());
        }
    }
}
