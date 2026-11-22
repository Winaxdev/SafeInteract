package com.safeinteract.handler;

import com.mojang.blaze3d.platform.InputConstants;
import com.safeinteract.config.ModConfig;
import com.safeinteract.util.Feedback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.sdl.SDLMouse;

public final class AutoHoldHandler {
    public enum Mode { NONE, USE, ATTACK }

    private static Mode mode = Mode.NONE;
    private static LocalPlayer player;
    private static ClientLevel world;
    private static int selectedSlot;
    private static Item item;
    private static Vec3 startPos;
    private static float lastHealth;
    private static BlockPos targetPos;
    private static int targetEntityId = -1;
    private static int airTicks;
    private static long lastAbortMs;
    private static Mode lastAbortMode = Mode.NONE;

    private AutoHoldHandler() {}

    public static void toggle(Minecraft client, Mode requested) {
        if (requested == Mode.NONE || client.player == null || client.level == null) return;
        if (mode == requested) {
            stop(client);
            announce(requested, false);
            return;
        }
        if (ServerPolicy.isHoldBlocked()) {
            Feedback.actionBar(Component.translatable("message.safeinteract.server_disabled", Component.translatable("feature.safeinteract.smart_hold")));
            Feedback.sound(SoundEvents.NOTE_BLOCK_HAT, 0.8f);
            return;
        }
        stop(client);
        mode = requested;
        player = client.player;
        world = client.level;
        selectedSlot = player.getInventory().getSelectedSlot();
        item = player.getMainHandItem().getItem();
        startPos = player.position();
        lastHealth = player.getHealth();
        targetPos = client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                ? hit.getBlockPos().immutable() : null;
        targetEntityId = client.hitResult instanceof EntityHitResult hit ? hit.getEntity().getId() : -1;
        airTicks = 0;
        announce(requested, true);
    }

    private static void announce(Mode current, boolean enabled) {
        Feedback.actionBar(Component.translatable(current == Mode.USE
                ? "message.safeinteract.hold_use" : "message.safeinteract.hold_attack", Feedback.onOff(enabled)));
        Feedback.sound(SoundEvents.UI_BUTTON_CLICK, enabled ? 1.5f : 0.8f);
    }

    public static void tick(Minecraft client) {
        if (mode == Mode.NONE) return;
        ModConfig config = ModConfig.get();
        if (!config.enabled || client.player == null || client.level == null) {
            stop(client);
            return;
        }
        if (ServerPolicy.isHoldBlocked()) {
            abort(client, Component.translatable("reason.safeinteract.server"));
            return;
        }
        LocalPlayer current = client.player;
        if (current != player || client.level != world) {
            abort(client, Component.translatable("reason.safeinteract.world"));
        } else if (!current.isAlive()) {
            abort(client, Component.translatable("reason.safeinteract.died"));
        } else if (config.durabilityGuardEnabled && isLowDurability(current.getMainHandItem(), config.durabilityThreshold)) {
            abortDurability(client, current.getMainHandItem());
        } else if (config.durabilityGuardEnabled && mode == Mode.USE
                && isLowDurability(current.getOffhandItem(), config.durabilityThreshold)) {
            abortDurability(client, current.getOffhandItem());
        } else if ((mode == Mode.USE ? config.holdFullInventoryGuardUse : config.holdFullInventoryGuardAttack)
                && current.getInventory().getFreeSlot() == -1) {
            abort(client, Component.translatable("reason.safeinteract.inventory_full"));
        } else if (config.holdDamageGuard && (current.hurtTime > 0 || current.getHealth() < lastHealth)) {
            abort(client, Component.translatable("reason.safeinteract.damage"));
        } else if (config.holdSlotGuard && current.getInventory().getSelectedSlot() != selectedSlot) {
            abort(client, Component.translatable("reason.safeinteract.slot"));
        } else if (config.holdSlotGuard && current.getMainHandItem().getItem() != item) {
            abort(client, Component.translatable("reason.safeinteract.item"));
        } else if (config.holdMovementGuard
                && current.position().distanceToSqr(startPos) > config.holdMovementTolerance * config.holdMovementTolerance) {
            abort(client, Component.translatable("reason.safeinteract.moved"));
        } else {
            if (config.holdTargetLostGuard && targetPos != null) {
                airTicks = client.level.getBlockState(targetPos).isAir() ? airTicks + 1 : 0;
                if (airTicks > config.holdTargetLostTicks) {
                    abort(client, Component.translatable("reason.safeinteract.target_lost"));
                    return;
                }
            }
            key(client, mode).setDown(true);
            lastHealth = current.getHealth();
        }
    }

    public static void checkScreen(Minecraft client) {
        if (mode != Mode.NONE && client.gui.screen() != null && client.gui.screen().getClass() != ChatScreen.class) {
            abort(client, client.player != null && !client.player.isAlive()
                    ? Component.translatable("reason.safeinteract.died") : Component.translatable("reason.safeinteract.screen"));
        }
    }

    public static void stopByServer(Minecraft client) {
        if (mode != Mode.NONE) abort(client, Component.translatable("reason.safeinteract.server"));
    }

    private static void abortDurability(Minecraft client, ItemStack stack) {
        abort(client, Component.translatable("reason.safeinteract.durability", stack.getMaxDamage() - stack.getDamageValue()));
    }

    private static void abort(Minecraft client, Component reason) {
        lastAbortMs = Util.getMillis();
        lastAbortMode = mode;
        stop(client);
        Feedback.actionBar(Component.translatable("message.safeinteract.hold_stopped", reason).withStyle(ChatFormatting.RED));
        Feedback.sound(SoundEvents.NOTE_BLOCK_HAT, 0.8f);
    }

    private static void stop(Minecraft client) {
        if (mode != Mode.NONE && client != null && client.options != null) {
            KeyMapping key = key(client, mode);
            key.setDown(client.gui.screen() == null && physicalState(client, key));
        }
        mode = Mode.NONE;
        player = null;
        world = null;
        item = null;
        startPos = null;
        targetPos = null;
        targetEntityId = -1;
        airTicks = 0;
    }

    private static KeyMapping key(Minecraft client, Mode current) {
        return current == Mode.USE ? client.options.keyUse : client.options.keyAttack;
    }

    private static boolean physicalState(Minecraft client, KeyMapping binding) {
        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(binding);
        if (key.getType() == InputConstants.Type.KEYBOARD) return InputConstants.isKeyDown(key.getValue());
        if (key.getType() == InputConstants.Type.MOUSE && key.getValue() > 0 && key.getValue() <= 32)
            return (SDLMouse.SDL_GetMouseState(null, null) & (1 << (key.getValue() - 1))) != 0;
        return false;
    }

    public static boolean isLowDurability(ItemStack stack, int threshold) {
        return !stack.isEmpty() && stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= threshold;
    }

    public static boolean isUseTarget(BlockPos pos) {
        return mode == Mode.USE && targetPos != null && targetPos.equals(pos);
    }

    public static boolean isUseTarget(Entity entity) {
        return mode == Mode.USE && targetEntityId != -1 && entity.getId() == targetEntityId;
    }

    public static void reset() {
        stop(Minecraft.getInstance());
        lastAbortMs = 0;
        lastAbortMode = Mode.NONE;
    }

    public static Mode getMode() { return mode; }
    public static long getLastAbortMs() { return lastAbortMs; }
    public static Mode getLastAbortMode() { return lastAbortMode; }
}
