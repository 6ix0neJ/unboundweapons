package com.mega.weapons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.MaceItem;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class MegaWeaponsClient implements ClientModInitializer {
    private static KeyBinding slamKey;

    @Override
    public void onInitializeClient() {
        // This clears the errors from lines 14-21 of image_bd4f99.jpg
        slamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mega_weapons.slam",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.mega_weapons"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // This clears the 'player' and 'wasPressed' errors from lines 27-28
            if (client.player != null && slamKey.wasPressed()) {
                if (client.player.getMainHandStack().getItem() instanceof MaceItem) {
                    // Use Identifier.of for 1.21.1
                    public record SlamPayload() implements net.fabricmc.fabric.api.networking.v1.CustomPayload {
                        public static final Id<SlamPayload> ID = new Id<>(Identifier.of("mega_weapons", "slam_trigger"));
                        public static final net.minecraft.network.codec.PacketCodec<net.minecraft.network.PacketByteBuf, SlamPayload> CODEC = net.minecraft.network.codec.PacketCodec.unit(new SlamPayload());
                        @Override public Id<? extends net.fabricmc.fabric.api.networking.v1.CustomPayload> getId() { return ID; }
                    }

                    //ClientPlayNetworking.send(new SlamPayload());
                }
            }
        });
    }
}