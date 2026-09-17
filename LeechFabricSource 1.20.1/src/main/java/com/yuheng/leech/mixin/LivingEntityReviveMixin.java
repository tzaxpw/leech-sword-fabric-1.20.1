package com.yuheng.leech.mixin;

import com.yuheng.leech.handler.LeechReviveHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityReviveMixin {
    @Inject(method = "tryUseTotem", at = @At("HEAD"), cancellable = true)
    private void leech$tryRevive(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity instanceof ServerPlayerEntity player && LeechReviveHandler.tryRevive(player)) {
            // Returning true prevents death without consuming an item or running
            // vanilla totem status effects. Injecting before the vanilla source
            // exclusions also permits /kill, void, and other bypass damage.
            cir.setReturnValue(true);
        }
    }
}
