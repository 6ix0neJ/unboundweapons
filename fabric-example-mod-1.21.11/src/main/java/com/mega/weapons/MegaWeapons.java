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

    public static final net.minecraft.component.ComponentType<Integer> MEGA_LEVEL = net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents.register(
            net.minecraft.util.Identifier.of("mega_weapons", "level"),
            builder -> builder.codec(com.mojang.serialization.Codec.INT)
    );

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
        // 4. MEGA SWORD: Dash & Leveling System
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (world.isClient || !(stack.getItem() instanceof SwordItem)) {
                return TypedActionResult.pass(stack);
            }

            // --- UPGRADE LOGIC (Sneak + Right Click) ---
            if (player.isSneaking()) {
                int tokenSlot = player.getInventory().getSlotWithStack(new ItemStack(net.minecraft.item.Items.GOLD_NUGGET));

                if (tokenSlot != -1) {
                    player.getInventory().removeStack(tokenSlot, 1);

                    // 1.21.1 Way: Get and Set Components (Fixes image_91affc.jpg)
                    int currentLevel = stack.getOrDefault(MEGA_LEVEL, 0);
                    stack.set(MEGA_LEVEL, currentLevel + 1);

                    player.sendMessage(Text.literal("§6§lSWORD UPGRADED TO LEVEL " + (currentLevel + 1)), true);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.0f, 1.2f);
                } else {
                    player.sendMessage(Text.literal("§cYou need a Mega Token (Gold Nugget) to upgrade!"), true);
                }
                return TypedActionResult.success(stack);
            }

            // --- DASH LOGIC (Normal Right Click) ---
            else {
                int level = stack.getOrDefault(MEGA_LEVEL, 0);
                double power = 1.5 + (level * 0.2);

                net.minecraft.util.math.Vec3d dash = player.getRotationVector().multiply(power, 0.2, power);
                player.addVelocity(dash.x, dash.y, dash.z);
                player.velocityModified = true;

                // Visuals: Standard blue Sonic Boom particles
                ((ServerWorld)world).spawnParticles(
                        ParticleTypes.SONIC_BOOM,
                        player.getX(), player.getY(), player.getZ(),
                        1 + (level / 2), 0.2, 0.2, 0.2, 0.05
                );

                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.5f);
                player.getItemCooldownManager().set(stack.getItem(), 20);

                return TypedActionResult.success(stack);
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
