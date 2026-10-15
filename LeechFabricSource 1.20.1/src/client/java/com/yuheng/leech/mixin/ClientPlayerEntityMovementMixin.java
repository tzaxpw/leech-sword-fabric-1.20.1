package com.yuheng.leech.mixin;

import com.yuheng.leech.item.LeechSwordItem;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMovementMixin {
    @Redirect(
            method = {"tickMovement", "canStartSprinting"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z",
                    ordinal = 0
            ),
            require = 2,
            allow = 2
    )
    private boolean leech$ignoreSwordUseForMovement(ClientPlayerEntity player) {
        // Only these two movement checks ignore Leech Sword use. The actual
        // use state remains intact for the BLOCK pose and custom damage handler.
        // Vanilla hunger, collision, sneak, blindness, and other sprint checks remain.
        return player.isUsingItem() && !LeechSwordItem.isLeechSwordActiveItem(player);
    }
}
