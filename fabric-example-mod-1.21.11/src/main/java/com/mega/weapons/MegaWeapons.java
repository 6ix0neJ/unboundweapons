/*
*  Note: All code is subject to change in the near future
*/
package com.mega.weapons;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import java.util.HashMap;
import java.util.UUID;

public class MegaWeapons implements ModInitializer {
    // This stores how many hits you've landed so the 4th hit is always a Black Flash
    private final HashMap<UUID, Integer> comboCounter = new HashMap<>();

    @Override
    public void onInitialize() {

        // 1. THE AXE CHARGE (Using the "Eating" animation trick)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof AxeItem) {
                if (!world.isClient) {
                    // Spawn white "wind" particles at the player's feet
                    ((ServerWorld)world).spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 3, 0.2, 0.1, 0.2, 0.02);
                }
                // This makes the player hold the axe up like they are eating/blocking
                return TypedActionResult.consume(stack);
            }
            return TypedActionResult.pass(stack);
        });

        // 2. THE MEGA IMPACTS (Sword & Axe combat)
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && entity instanceof LivingEntity target) {
                ItemStack stack = player.getStackInHand(hand);
                ServerWorld sWorld = (ServerWorld) world;

                // SWORD LOGIC: The 4-Hit Black Flash
                if (stack.getItem() instanceof SwordItem) {
                    int hits = comboCounter.getOrDefault(player.getUuid(), 0) + 1;

                    if (hits >= 4) {
                        triggerImpact(player, target, sWorld, "BLACK_FLASH");
                        comboCounter.put(player.getUuid(), 0); // Reset combo
                    } else {
                        comboCounter.put(player.getUuid(), hits);
                        player.sendMessage(Text.literal("§7Combo: §e" + hits), true);
                    }
                }

                // AXE LOGIC: Sneak + Attack = Armor Paralyze
                if (stack.getItem() instanceof AxeItem && player.isSneaking()) {
                    triggerImpact(player, target, sWorld, "PARALYZE");
                }
            }
            return ActionResult.PASS;
        });
        // 3. THE TOKEN ECONOMY
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents.AFTER_KILLED_ENEMY_ENTITY.register((world, entity, killer) -> {
            // This fixes the "Inconvertible types" error from image_ca09a4.png
            if (killer instanceof net.minecraft.server.network.ServerPlayerEntity player) {

                // Reward: Give the Gold Nugget
                player.getInventory().insertStack(new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLD_NUGGET));

                // Notification - Use false if 'true' still shows an error for sendMessage
                player.sendMessage(net.minecraft.text.Text.literal("§e§l+1 MEGA TOKEN"), true);

                player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        });
    }

    private void triggerImpact(net.minecraft.entity.player.PlayerEntity player, LivingEntity target, ServerWorld world, String type) {
        // Visual: The Blue Sonic Boom from the Warden
        world.spawnParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
        player.playSound(net.minecraft.sound.SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);

        if (type.equals("PARALYZE")) {
            // Apply the "Frozen" effect (Slowness 255 makes them unable to move)
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 255));
            target.sendMessage(Text.literal("§4§lARMOR PARALYZED!"));
        } else {
            // Black Flash: Huge knockback
            target.takeKnockback(2.0, player.getX() - target.getX(), player.getZ() - target.getZ());
            player.sendMessage(Text.literal("§0§l> §c§lBLACK FLASH §0§l<"), true);
        }
    }
}
