package org.minitype.mcmodstest.client;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.item.Items;

import net.minecraft.util.math.Vec3d;

import org.lwjgl.glfw.GLFW;

public class MegaWeaponsClient implements ClientModInitializer {

    private static KeyBinding dashKey;

    @Override
    public void onInitializeClient() {

        dashKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.mega_weapons.dash",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        KeyBinding.Category.create(
                                Identifier.of("mega_weapons", "mega_weapons")
                        )
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (dashKey.wasPressed()) {

                MinecraftClient mc = MinecraftClient.getInstance();

                if (mc.player == null) return;

                // Must sprint
                if (!mc.player.isSprinting()) return;

                // Must hold sword
                if (
                        !mc.player.getMainHandStack().isOf(Items.WOODEN_SWORD) &&
                                !mc.player.getMainHandStack().isOf(Items.STONE_SWORD) &&
                                !mc.player.getMainHandStack().isOf(Items.IRON_SWORD) &&
                                !mc.player.getMainHandStack().isOf(Items.GOLDEN_SWORD) &&
                                !mc.player.getMainHandStack().isOf(Items.DIAMOND_SWORD) &&
                                !mc.player.getMainHandStack().isOf(Items.NETHERITE_SWORD)
                ) {
                    return;
                }

                Vec3d look = mc.player.getRotationVector();

                Vec3d dash = new Vec3d(
                        look.x,
                        0,
                        look.z
                ).normalize().multiply(1.5);

                mc.player.addVelocity(
                        dash.x,
                        0.15,
                        dash.z
                );
            }
        });
    }
}