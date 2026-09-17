package com.yuheng.leech.storage;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LeechStorageState extends PersistentState {
    public static final int STORAGE_SIZE = 54;
    private static final String SAVE_ID = "leech_storage";

    private final Map<UUID, DefaultedList<ItemStack>> inventories = new HashMap<>();
    private final Map<UUID, Integer> reviveCooldownTicks = new HashMap<>();

    public static LeechStorageState get(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(LeechStorageState::fromNbt, LeechStorageState::new, SAVE_ID);
    }

    public static LeechStorageState fromNbt(NbtCompound nbt) {
        LeechStorageState state = new LeechStorageState();
        NbtCompound players = nbt.getCompound("Players");

        for (String uuidString : players.getKeys()) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                DefaultedList<ItemStack> inventory = DefaultedList.ofSize(STORAGE_SIZE, ItemStack.EMPTY);
                NbtList items = players.getCompound(uuidString).getList("Items", 10);

                for (int i = 0; i < items.size(); i++) {
                    NbtCompound itemNbt = items.getCompound(i);
                    int slot = itemNbt.getByte("Slot") & 255;
                    if (slot >= 0 && slot < STORAGE_SIZE) {
                        inventory.set(slot, ItemStack.fromNbt(itemNbt));
                    }
                }

                state.inventories.put(uuid, inventory);
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupted or old entries instead of preventing the world from loading.
            }
        }

        NbtCompound cooldowns = nbt.getCompound("ReviveCooldowns");
        for (String uuidString : cooldowns.getKeys()) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                int remainingTicks = cooldowns.getInt(uuidString);
                if (remainingTicks > 0) {
                    state.reviveCooldownTicks.put(uuid, remainingTicks);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed UUID keys instead of preventing the world from loading.
            }
        }

        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound players = new NbtCompound();

        for (Map.Entry<UUID, DefaultedList<ItemStack>> entry : this.inventories.entrySet()) {
            NbtCompound playerNbt = new NbtCompound();
            NbtList items = new NbtList();
            DefaultedList<ItemStack> inventory = entry.getValue();

            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.get(slot);
                if (!stack.isEmpty()) {
                    NbtCompound itemNbt = new NbtCompound();
                    itemNbt.putByte("Slot", (byte) slot);
                    stack.writeNbt(itemNbt);
                    items.add(itemNbt);
                }
            }

            playerNbt.put("Items", items);
            players.put(entry.getKey().toString(), playerNbt);
        }

        nbt.put("Players", players);

        NbtCompound cooldowns = new NbtCompound();
        for (Map.Entry<UUID, Integer> entry : this.reviveCooldownTicks.entrySet()) {
            if (entry.getValue() > 0) {
                cooldowns.putInt(entry.getKey().toString(), entry.getValue());
            }
        }
        nbt.put("ReviveCooldowns", cooldowns);

        return nbt;
    }

    public DefaultedList<ItemStack> getInventory(UUID playerUuid) {
        return this.inventories.computeIfAbsent(playerUuid, uuid -> DefaultedList.ofSize(STORAGE_SIZE, ItemStack.EMPTY));
    }

    public int getReviveCooldownTicks(UUID playerUuid) {
        return this.reviveCooldownTicks.getOrDefault(playerUuid, 0);
    }

    public void setReviveCooldownTicks(UUID playerUuid, int ticks) {
        if (ticks > 0) {
            this.reviveCooldownTicks.put(playerUuid, ticks);
        } else {
            this.reviveCooldownTicks.remove(playerUuid);
        }
        this.markDirty();
    }

    public int tickReviveCooldown(UUID playerUuid) {
        int remainingTicks = this.getReviveCooldownTicks(playerUuid);
        if (remainingTicks <= 0) {
            return 0;
        }

        int updatedTicks = remainingTicks - 1;
        if (updatedTicks > 0) {
            this.reviveCooldownTicks.put(playerUuid, updatedTicks);
        } else {
            this.reviveCooldownTicks.remove(playerUuid);
        }
        this.markDirty();
        return updatedTicks;
    }
}
