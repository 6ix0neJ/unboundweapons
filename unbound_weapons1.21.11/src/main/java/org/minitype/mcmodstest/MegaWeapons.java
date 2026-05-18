package org.minitype.mcmodstest;

import com.mojang.serialization.Codec;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.component.ComponentType;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import net.minecraft.particle.ParticleTypes;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import net.minecraft.text.Text;

import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MegaWeapons implements ModInitializer {
    public static final Identifier DASH_PACKET =
            Identifier.of("mega_weapons", "dash");
    // =========================================================
    // CUSTOM SWORD LEVEL COMPONENT
    // =========================================================

    public static final ComponentType<Integer> MEGA_LEVEL = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("mega_weapons", "level"),
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .build()
    );

    // =========================================================
    // COMBO TRACKING
    // =========================================================

    private final Map<UUID, Integer> comboCounter = new HashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();

    // Combo resets after 3 seconds
    private static final long COMBO_RESET_TIME = 3000;

    // =========================================================
    // IMPACT TYPES
    // =========================================================

    public enum ImpactType {
        BLACK_FLASH,
        PARALYZE
    }

    @Override
    public void onInitialize() {

        System.out.println("Mega Weapons Initialized!");


        // =====================================================
// USE ITEM CALLBACK
// =====================================================

        UseItemCallback.EVENT.register((player, world, hand) -> {

            ItemStack stack = player.getStackInHand(hand);

            if (world.isClient()) {
                return net.minecraft.util.TypedActionResult.pass(stack);
            }

            // =================================================
            // AXE LOGIC
            // =================================================

            if (stack.getItem() instanceof net.minecraft.item.AxeItem) {

                ((ServerWorld) world).spawnParticles(
                        ParticleTypes.CLOUD,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        3,
                        0.2,
                        0.1,
                        0.2,
                        0.02
                );

                return net.minecraft.util.TypedActionResult.consume(stack);
            }

            // =================================================
            // SWORD LOGIC
            // =================================================

            if (
                    stack.isOf(Items.WOODEN_SWORD) ||
                            stack.isOf(Items.STONE_SWORD) ||
                            stack.isOf(Items.IRON_SWORD) ||
                            stack.isOf(Items.GOLDEN_SWORD) ||
                            stack.isOf(Items.DIAMOND_SWORD) ||
                            stack.isOf(Items.NETHERITE_SWORD)
            ) {

                // =============================================
                // UPGRADE SYSTEM
                // Sneak + Right Click
                // =============================================

                if (player.isSneaking()) {

                    boolean upgraded = false;

                    for (int i = 0; i < player.getInventory().size(); i++) {

                        ItemStack invStack = player.getInventory().getStack(i);

                        if (invStack.isOf(Items.GOLD_NUGGET)) {

                            invStack.decrement(1);
                            upgraded = true;
                            break;
                        }
                    }

                    if (upgraded) {

                        int currentLevel = stack.getOrDefault(MEGA_LEVEL, 0);

                        stack.set(MEGA_LEVEL, currentLevel + 1);

                        player.sendMessage(
                                Text.literal("§6§lSWORD UPGRADED TO LEVEL " + (currentLevel + 1)),
                                true
                        );

                        world.playSound(
                                null,
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                SoundEvents.BLOCK_ANVIL_USE,
                                SoundCategory.PLAYERS,
                                1.0f,
                                1.2f
                        );

                    } else {

                        player.sendMessage(
                                Text.literal("§cYou need a Mega Token (Gold Nugget)!"),
                                true
                        );
                    }

                    return net.minecraft.util.TypedActionResult.success(stack);
                }

                return net.minecraft.util.TypedActionResult.pass(stack);
            }

            return net.minecraft.util.TypedActionResult.pass(stack);
        });

        // =====================================================
        // ATTACK CALLBACK
        // =====================================================

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {

            if (!world.isClient() && entity instanceof LivingEntity target) {

                ItemStack stack = player.getStackInHand(hand);
                ServerWorld sWorld = (ServerWorld) world;

                // =============================================
                // SWORD COMBO SYSTEM
                // =============================================

                if (
                        stack.isOf(Items.WOODEN_SWORD) ||
                                stack.isOf(Items.STONE_SWORD) ||
                                stack.isOf(Items.IRON_SWORD) ||
                                stack.isOf(Items.GOLDEN_SWORD) ||
                                stack.isOf(Items.DIAMOND_SWORD) ||
                                stack.isOf(Items.NETHERITE_SWORD)
                ) {

                    UUID uuid = player.getUuid();

                    long now = System.currentTimeMillis();

                    long lastHit = lastHitTime.getOrDefault(uuid, 0L);

                    int hits;

                    // Reset combo if too slow
                    if (now - lastHit > COMBO_RESET_TIME) {
                        hits = 1;
                    } else {
                        hits = comboCounter.getOrDefault(uuid, 0) + 1;
                    }

                    lastHitTime.put(uuid, now);

                    // BLACK FLASH
                    if (hits >= 4) {

                        triggerImpact(
                                player,
                                target,
                                sWorld,
                                ImpactType.BLACK_FLASH
                        );

                        comboCounter.put(uuid, 0);

                    } else {

                        comboCounter.put(uuid, hits);

                        player.sendMessage(
                                Text.literal("§7Combo: §e" + hits),
                                true
                        );
                    }
                }

                // =============================================
                // AXE PARALYZE
                // Sneak + Attack
                // =============================================

                if (stack.getItem() instanceof net.minecraft.item.AxeItem
                        && player.isSneaking()) {

                    triggerImpact(
                            player,
                            target,
                            sWorld,
                            ImpactType.PARALYZE
                    );
                }
            }

            return ActionResult.PASS;
        });

        // =====================================================
        // TOKEN ECONOMY
        // =====================================================

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {

            if (damageSource.getAttacker() instanceof ServerPlayerEntity player) {

                ItemStack nugget = new ItemStack(Items.GOLD_NUGGET);

                // Try inventory first
                if (!player.getInventory().insertStack(nugget)) {
                    player.dropItem(nugget, false);
                }

                player.sendMessage(
                        Text.literal("§e§l+1 MEGA TOKEN"),
                        false
                );

                player.playSound(
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        1.0f,
                        1.0f
                );
            }
        });

        // =====================================================
        // MEMORY CLEANUP
        // =====================================================

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {

            UUID uuid = handler.player.getUuid();

            comboCounter.remove(uuid);
            lastHitTime.remove(uuid);
        });
    }

    // =========================================================
    // IMPACT HANDLER
    // =========================================================

    private void triggerImpact(
            net.minecraft.entity.player.PlayerEntity player,
            LivingEntity target,
            ServerWorld world,
            ImpactType type
    ) {

        // Shared effects
        world.spawnParticles(
                ParticleTypes.SONIC_BOOM,
                target.getX(),
                target.getY() + 1,
                target.getZ(),
                1,
                0,
                0,
                0,
                0
        );

        player.playSound(
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                1.0f,
                1.0f
        );

        // =====================================================
        // PARALYZE
        // =====================================================

        if (type == ImpactType.PARALYZE) {

            target.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.SLOWNESS,
                            60,
                            255
                    )
            );

            player.sendMessage(
                    Text.literal("§4§lARMOR PARALYZED!"),
                    true
            );
        }

        // =====================================================
        // BLACK FLASH
        // =====================================================

        if (type == ImpactType.BLACK_FLASH) {

            target.takeKnockback(
                    2.0,
                    player.getX() - target.getX(),
                    player.getZ() - target.getZ()
            );

            player.sendMessage(
                    Text.literal("§0§l> §c§lBLACK FLASH §0§l<"),
                    true
            );
        }
    }
}