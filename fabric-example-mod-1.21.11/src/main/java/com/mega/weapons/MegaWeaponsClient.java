package com.mega.weapons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.MaceItem;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class MegaWeaponsClient implements ClientModInitializer {
    private static KeyBinding slamKey;

    @Override
    public void onInitializeClient() {
        // 1. Register the Keybind (Fixes 'KeyBinding' and 'KeyBindingHelper' from image_bdc73c.jpg)
        slamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mega_weapons.slam",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.mega_weapons"
        ));

        // 2. Client Tick Listener (Fixes 'ClientTickEvents' and 'player' from image_bdc73c.jpg)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && slamKey.wasPressed()) {
                if (client.player.getMainHandStack().getItem() instanceof MaceItem) {
                    // Send packet to server (Fixes 'ClientPlayNetworking' and 'Identifier' from image_bdc73c.jpg)
                    ClientPlayNetworking.send(Identifier.of("mega_weapons", "slam_trigger"), net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create());
                }
            }
        });
    }
}