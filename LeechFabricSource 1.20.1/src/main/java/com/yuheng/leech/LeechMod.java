package com.yuheng.leech;

import com.yuheng.leech.handler.LeechBlockDamageHandler;
import com.yuheng.leech.handler.LeechInventoryHealthHandler;
import com.yuheng.leech.handler.LeechReviveHandler;
import com.yuheng.leech.item.LeechSwordItem;
import com.yuheng.leech.net.LeechPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class LeechMod implements ModInitializer {
    public static final String MOD_ID = "leech";

    public static final Item LEECH_SWORD = new LeechSwordItem(
            new Item.Settings().fireproof().maxCount(1)
    );

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, id("leech"), LEECH_SWORD);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(LeechSwordItem.createDefaultStack()));

        LeechPackets.registerServer();
        ServerTickEvents.END_SERVER_TICK.register(LeechInventoryHealthHandler::tick);
        ServerTickEvents.END_SERVER_TICK.register(LeechReviveHandler::tick);
        LeechBlockDamageHandler.register();

        System.out.println("Leech Sword Loaded!");
    }
}
