package com.example.autolunge;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class AutoLungeClient implements ClientModInitializer {
    /** Ticks between lunges (20 ticks = 1 second). */
    private static final int INTERVAL_TICKS = 8;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("autolunge", "main"));

    private static KeyMapping holdKey;
    private int cooldown = 0;
    private int restoreSlot = -1;

    @Override
    public void onInitializeClient() {
        holdKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.autolunge.hold",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        if (client.player == null || !client.mouseHandler.isMouseGrabbed()) {
            cooldown = 0;
            restoreSlot = -1;
            return;
        }

        Inventory inv = client.player.getInventory();

        // The lunge went through during this tick, so now swap back
        if (restoreSlot >= 0) {
            inv.setSelectedSlot(restoreSlot);
            restoreSlot = -1;
        }

        if (holdKey.isDown()) {
            if (cooldown <= 0) {
                int spear = findSpearSlot(inv);
                if (spear >= 0) {
                    int current = inv.getSelectedSlot();
                    if (spear != current) {
                        inv.setSelectedSlot(spear);
                        restoreSlot = current;
                    }
                    KeyMapping.click(KeyMappingHelper.getBoundKeyOf(client.options.keyAttack));
                    cooldown = INTERVAL_TICKS;
                }
            }
            cooldown--;
        } else {
            cooldown = 0; // next press fires instantly
        }
    }

    private static int findSpearSlot(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()
                    && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_spear")) {
                return i;
            }
        }
        return -1;
    }
}
