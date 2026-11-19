package com.yuheng.leech.mixin;

import com.yuheng.leech.client.LeechAirMovement;
import com.yuheng.leech.item.LeechSwordItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityAirMovementMixin {
    @Inject(method = "getOffGroundSpeed", at = @At("HEAD"), cancellable = true, require = 1)
    private void leech$useSwordSpeedForAirJump(CallbackInfoReturnable<Float> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!player.getWorld().isClient) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (player != client.player || client.currentScreen != null
                || player.isOnGround() || player.hasVehicle()
                || player.getAbilities().flying || player.isFallFlying()
                || player.isTouchingWater() || player.isInLava()
                || !client.options.jumpKey.isPressed()
                || !LeechSwordItem.isLeechSwordMainHand(player)) {
            return;
        }

        cir.setReturnValue(LeechAirMovement.accelerationFor(player.getMovementSpeed()));
    }
}
