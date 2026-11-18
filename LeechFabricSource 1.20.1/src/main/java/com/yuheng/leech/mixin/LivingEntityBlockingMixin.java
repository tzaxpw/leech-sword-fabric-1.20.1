package com.yuheng.leech.mixin;

import com.yuheng.leech.item.LeechSwordItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBlockingMixin {
    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void leech$disableVanillaShieldBlocking(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof PlayerEntity player
                && player.isUsingItem()
                && LeechSwordItem.isLeechSwordActiveItem(player)) {
            // Keep UseAction.BLOCK for the correct arm pose, but do not let vanilla
            // shield logic fully cancel frontal damage. LeechBlockDamageHandler
            // handles the custom damage reduction.
            cir.setReturnValue(false);
        }
    }
}
