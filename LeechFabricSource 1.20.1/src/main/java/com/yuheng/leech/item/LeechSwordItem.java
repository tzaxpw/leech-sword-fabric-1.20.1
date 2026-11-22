package com.yuheng.leech.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.yuheng.leech.LeechMod;
import com.yuheng.leech.mixin.LivingEntityAccessor;
import com.yuheng.leech.util.LeechDamageSources;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class LeechSwordItem extends PickaxeItem {
    // Same UUIDs as your /give command's UUID int arrays:
    // [179,14,3214,4323], [52,4341,3375,2053], [7435,23,234,2412], [8842,91,5567,3001]
    // Movement speed uses ADDITION 0.06: fixed absolute movement-speed bonus, not a multiplier.
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("000000b3-0000-000e-0000-0c8e000010e3");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("00000034-0000-10f5-0000-0d2f00000805");
    private static final UUID MOVEMENT_SPEED_UUID = UUID.fromString("00001d0b-0000-0017-0000-00ea0000096c");
    private static final UUID KNOCKBACK_RESISTANCE_UUID = UUID.fromString("0000228a-0000-005b-0000-15bf00000bb9");

    private final Multimap<EntityAttribute, EntityAttributeModifier> mainHandAttributeModifiers;

    public LeechSwordItem(Settings settings) {
        // This item intentionally extends PickaxeItem, not SwordItem.
        // That preserves the old mod behavior: pickaxe-like tool logic and no vanilla sword sweep attack.
        super(LeechToolMaterial.INSTANCE, 0, 0.0F, settings);

        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(
                ATTACK_DAMAGE_UUID,
                "generic.attack_damage",
                10.0D,
                EntityAttributeModifier.Operation.ADDITION
        ));
        builder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(
                ATTACK_SPEED_UUID,
                "generic.attack_speed",
                99.0D,
                EntityAttributeModifier.Operation.ADDITION
        ));
        builder.put(EntityAttributes.GENERIC_MOVEMENT_SPEED, new EntityAttributeModifier(
                MOVEMENT_SPEED_UUID,
                "generic.movement_speed",
                0.06D,
                EntityAttributeModifier.Operation.ADDITION
        ));
        builder.put(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, new EntityAttributeModifier(
                KNOCKBACK_RESISTANCE_UUID,
                "generic.knockback_resistance",
                1.0D,
                EntityAttributeModifier.Operation.ADDITION
        ));
        this.mainHandAttributeModifiers = builder.build();
    }

    public static ItemStack createDefaultStack() {
        ItemStack stack = new ItemStack(LeechMod.LEECH_SWORD);
        ensureNativeNbt(stack);
        return stack;
    }

    public static void ensureNativeNbt(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean("Unbreakable", true);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return false;
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.mainHandAttributeModifiers;
        }
        return super.getAttributeModifiers(slot);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Keep right-click held so the custom blocking damage handler can detect it.
        user.setCurrentHand(hand);

        if (!world.isClient) {
            ensureNativeNbt(stack);

            user.heal(1.0F);
            user.fallDistance = 0.0F;

            // Right-click cleanse list. Levitation is intentionally NOT here anymore;
            // it is removed by the separate keybind packet.
            user.removeStatusEffect(StatusEffects.SLOWNESS);
            user.removeStatusEffect(StatusEffects.NAUSEA);
            user.removeStatusEffect(StatusEffects.DARKNESS);
            user.removeStatusEffect(StatusEffects.BLINDNESS);
            user.removeStatusEffect(StatusEffects.SLOW_FALLING);
            user.removeStatusEffect(StatusEffects.MINING_FATIGUE);
            user.removeStatusEffect(StatusEffects.UNLUCK);
        }

        return TypedActionResult.consume(stack);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        // Use vanilla BLOCK arm pose so the third-person right arm naturally
        // crosses the chest instead of rotating only the item model.
        return UseAction.BLOCK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient && attacker instanceof PlayerEntity player) {
            ensureNativeNbt(stack);

            // Slowness II, 120 ticks. Amplifier 1 means level II.
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1));

            if (target.isAlive()) {
                LivingEntityAccessor accessor = (LivingEntityAccessor) target;
                int normalHitIFrames = target.timeUntilRegen;
                float normalHitLastDamage = accessor.leech$getLastDamageTaken();

                // Temporarily clear the just-applied normal hit i-frames so the extra 3 true damage is not eaten.
                target.timeUntilRegen = 0;
                target.hurtTime = 0;

                target.damage(LeechDamageSources.trueDamage(player), 3.0F);

                // Restore an invulnerability window after the extra hit.
                // Without restoring lastDamageTaken, fast clicks can leak damage through vanilla's
                // "larger damage during i-frames" rule, making click speed equal DPS.
                target.timeUntilRegen = Math.max(target.timeUntilRegen, Math.max(normalHitIFrames, 20));
                accessor.leech$setLastDamageTaken(
                        Math.max(accessor.leech$getLastDamageTaken(), normalHitLastDamage + 3.0F)
                );
            }
        }

        // Do not call super.postHit(), because the item is intended to behave like Unbreakable:1b.
        return true;
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient) {
            ensureNativeNbt(stack);
        }
        // Do not call super.postMine(), because vanilla tools would consume durability on blocks.
        return true;
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        // Old mod behavior: fixed high mining speed on every block.
        // 6.0F matches iron pickaxe / iron shovel speed.
        return state.isAir() ? 1.0F : LeechToolMaterial.INSTANCE.getMiningSpeedMultiplier();
    }

    @Override
    public boolean isSuitableFor(BlockState state) {
        // Old mod behavior: can harvest every block. The material mining level is netherite-level.
        return true;
    }

    public static boolean isLeechSwordMainHand(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof LeechSwordItem;
    }

    public static boolean isLeechSwordActiveItem(PlayerEntity player) {
        return player.getActiveItem().getItem() instanceof LeechSwordItem;
    }
}
