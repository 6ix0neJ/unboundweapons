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
        // 3. THE TOKEN ECONOMY (1.21.1 Compatible)
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof net.minecraft.entity.LivingEntity living && living.isDead()) {
                net.minecraft.entity.damage.DamageSource source = living.getRecentDamageSource();
                if (source != null && source.getAttacker() instanceof net.minecraft.server.network.ServerPlayerEntity player) {

                    // Give Gold Nugget
                    player.getInventory().insertStack(new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLD_NUGGET));

                    // Notification
                    player.sendMessage(net.minecraft.text.Text.literal("§e§l+1 MEGA TOKEN"));

                    // Sound
                    player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
            }
        });
        // MEGA SWORD: Right-Click to Dash
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, world, hand) -> {
            net.minecraft.item.ItemStack stack = player.getStackInHand(hand);

            // Check if it's a Sword and player is NOT sneaking (to distinguish from Mace launch)
            if (stack.getItem() instanceof net.minecraft.item.SwordItem && !player.isSneaking()) {
                if (!world.isClient) {
                    // 1. Calculate Dash Vector
                    net.minecraft.util.math.Vec3d dash = player.getRotationVector().multiply(2.0, 0.2, 2.0);
                    player.addVelocity(dash.x, dash.y, dash.z);
                    player.velocityModified = true;

                    // 2. Visuals: Standard blue particles (per your preference)
                    ((net.minecraft.server.world.ServerWorld)world).spawnParticles(
                            net.minecraft.particle.ParticleTypes.SONIC_BOOM,
                            player.getX(), player.getY(), player.getZ(),
                            1, 0, 0, 0, 0
                    );

                    // 3. Sound
                    player.playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
                }
                return net.minecraft.util.TypedActionResult.success(stack);
            }
            return net.minecraft.util.TypedActionResult.pass(stack);
            // 1. Check for Level Up (Sneak + Right Click)
            if (player.isSneaking() && stack.getItem() instanceof net.minecraft.item.SwordItem) {
                // Look for a Mega Token (Gold Nugget) in inventory
                if (player.getInventory().removeStack(player.getInventory().getSlotWithStack(new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLD_NUGGET)), 1) > 0) {

                    // Get current level from NBT (default to 0)
                    net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
                    int currentLevel = nbt.getInt("MegaLevel");
                    nbt.putInt("MegaLevel", currentLevel + 1);

                    // Success Feedback
                    player.sendMessage(net.minecraft.text.Text.literal("§6§lSWORD UPGRADED TO LEVEL " + (currentLevel + 1)), true);
                    player.playSound(net.minecraft.sound.SoundEvents.BLOCK_ANVIL_USE, 1.0f, 1.2f);
                } else {
                    player.sendMessage(net.minecraft.text.Text.literal("§cYou need a Mega Token to upgrade!"), true);
                }
                return net.minecraft.util.TypedActionResult.success(stack);
            }

            // 2. Modified Dash (Speed scales with Level)
            if (stack.getItem() instanceof net.minecraft.item.SwordItem && !player.isSneaking()) {
                int level = stack.getOrCreateNbt().getInt("MegaLevel");
                double speedMultiplier = 1.5 + (level * 0.2); // Starts at 1.5, grows by 0.2 per level

                net.minecraft.util.math.Vec3d dash = player.getRotationVector().multiply(speedMultiplier, 0.2, speedMultiplier);
                player.addVelocity(dash.x, dash.y, dash.z);
                player.velocityModified = true;

                // Standard blue particles (Sonic Boom) as requested
                ((net.minecraft.server.world.ServerWorld)player.getWorld()).spawnParticles(
                        net.minecraft.particle.ParticleTypes.SONIC_BOOM,
                        player.getX(), player.getY(), player.getZ(),
                        1 + level, 0.2, 0.2, 0.2, 0.05
                );

                return net.minecraft.util.TypedActionResult.success(stack);
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
