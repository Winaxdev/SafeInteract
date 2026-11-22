package com.safeinteract.handler;

import com.safeinteract.config.ModConfig;
import com.safeinteract.util.Feedback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class AutoHoldHandler {
    public enum Mode { NONE, USE, ATTACK }

    private static Mode mode = Mode.NONE;
    private static ClientPlayerEntity player;
    private static ClientWorld world;
    private static int selectedSlot;
    private static Item item;
    private static Vec3d startPos;
    private static float lastHealth;
    private static BlockPos targetPos;
    private static int targetEntityId = -1;
    private static int airTicks;
    private static long lastAbortMs;
    private static Mode lastAbortMode = Mode.NONE;

    private AutoHoldHandler() {}

    public static void toggle(MinecraftClient client, Mode requested) {
        if (requested == Mode.NONE || client.player == null || client.world == null) return;
        if (mode == requested) {
            stop(client);
            announce(requested, false);
            return;
        }
        if (ServerPolicy.isHoldBlocked()) {
            Feedback.actionBar(Text.translatable("message.safeinteract.server_disabled", Text.translatable("feature.safeinteract.smart_hold")));
            Feedback.sound(SoundEvents.BLOCK_NOTE_BLOCK_HAT, 0.8f);
            return;
        }
        stop(client);
        mode = requested;
        player = client.player;
        world = client.world;
        selectedSlot = player.getInventory().selectedSlot;
        item = player.getMainHandStack().getItem();
        startPos = player.getPos();
        lastHealth = player.getHealth();
        targetPos = client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                ? hit.getBlockPos().toImmutable() : null;
        targetEntityId = client.crosshairTarget instanceof EntityHitResult hit ? hit.getEntity().getId() : -1;
        airTicks = 0;
        announce(requested, true);
    }

    private static void announce(Mode current, boolean enabled) {
        Feedback.actionBar(Text.translatable(current == Mode.USE
                ? "message.safeinteract.hold_use" : "message.safeinteract.hold_attack", Feedback.onOff(enabled)));
        Feedback.sound(SoundEvents.UI_BUTTON_CLICK, enabled ? 1.5f : 0.8f);
    }

    public static void tick(MinecraftClient client) {
        if (mode == Mode.NONE) return;
        ModConfig config = ModConfig.get();
        if (!config.enabled || client.player == null || client.world == null) {
            stop(client);
            return;
        }
        if (ServerPolicy.isHoldBlocked()) {
            abort(client, Text.translatable("reason.safeinteract.server"));
            return;
        }
        ClientPlayerEntity current = client.player;
        if (current != player || client.world != world) {
            abort(client, Text.translatable("reason.safeinteract.world"));
        } else if (!current.isAlive()) {
            abort(client, Text.translatable("reason.safeinteract.died"));
        } else if (config.durabilityGuardEnabled && isLowDurability(current.getMainHandStack(), config.durabilityThreshold)) {
            abortDurability(client, current.getMainHandStack());
        } else if (config.durabilityGuardEnabled && mode == Mode.USE
                && isLowDurability(current.getOffHandStack(), config.durabilityThreshold)) {
            abortDurability(client, current.getOffHandStack());
        } else if ((mode == Mode.USE ? config.holdFullInventoryGuardUse : config.holdFullInventoryGuardAttack)
                && current.getInventory().getEmptySlot() == -1) {
            abort(client, Text.translatable("reason.safeinteract.inventory_full"));
        } else if (config.holdDamageGuard && (current.hurtTime > 0 || current.getHealth() < lastHealth)) {
            abort(client, Text.translatable("reason.safeinteract.damage"));
        } else if (config.holdSlotGuard && current.getInventory().selectedSlot != selectedSlot) {
            abort(client, Text.translatable("reason.safeinteract.slot"));
        } else if (config.holdSlotGuard && current.getMainHandStack().getItem() != item) {
            abort(client, Text.translatable("reason.safeinteract.item"));
        } else if (config.holdMovementGuard
                && current.getPos().squaredDistanceTo(startPos) > config.holdMovementTolerance * config.holdMovementTolerance) {
            abort(client, Text.translatable("reason.safeinteract.moved"));
        } else {
            if (config.holdTargetLostGuard && targetPos != null) {
                airTicks = client.world.getBlockState(targetPos).isAir() ? airTicks + 1 : 0;
                if (airTicks > config.holdTargetLostTicks) {
                    abort(client, Text.translatable("reason.safeinteract.target_lost"));
                    return;
                }
            }
            key(client, mode).setPressed(true);
            lastHealth = current.getHealth();
        }
    }

    public static void checkScreen(MinecraftClient client) {
        if (mode != Mode.NONE && client.currentScreen != null && client.currentScreen.getClass() != ChatScreen.class) {
            abort(client, client.player != null && !client.player.isAlive()
                    ? Text.translatable("reason.safeinteract.died") : Text.translatable("reason.safeinteract.screen"));
        }
    }

    public static void stopByServer(MinecraftClient client) {
        if (mode != Mode.NONE) abort(client, Text.translatable("reason.safeinteract.server"));
    }

    private static void abortDurability(MinecraftClient client, ItemStack stack) {
        abort(client, Text.translatable("reason.safeinteract.durability", stack.getMaxDamage() - stack.getDamage()));
    }

    private static void abort(MinecraftClient client, Text reason) {
        lastAbortMs = Util.getMeasuringTimeMs();
        lastAbortMode = mode;
        stop(client);
        Feedback.actionBar(Text.translatable("message.safeinteract.hold_stopped", reason).formatted(Formatting.RED));
        Feedback.sound(SoundEvents.BLOCK_NOTE_BLOCK_HAT, 0.8f);
    }

    private static void stop(MinecraftClient client) {
        if (mode != Mode.NONE && client != null && client.options != null) {
            KeyBinding key = key(client, mode);
            key.setPressed(client.currentScreen == null && physicalState(client, key));
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

    private static KeyBinding key(MinecraftClient client, Mode current) {
        return current == Mode.USE ? client.options.useKey : client.options.attackKey;
    }

    private static boolean physicalState(MinecraftClient client, KeyBinding binding) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(binding);
        if (key.getCategory() == InputUtil.Type.KEYSYM) return InputUtil.isKeyPressed(client.getWindow().getHandle(), key.getCode());
        if (key.getCategory() == InputUtil.Type.MOUSE) return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), key.getCode()) == GLFW.GLFW_PRESS;
        return false;
    }

    public static boolean isLowDurability(ItemStack stack, int threshold) {
        return !stack.isEmpty() && stack.isDamageable() && stack.getMaxDamage() - stack.getDamage() <= threshold;
    }

    public static boolean isUseTarget(BlockPos pos) {
        return mode == Mode.USE && targetPos != null && targetPos.equals(pos);
    }

    public static boolean isUseTarget(Entity entity) {
        return mode == Mode.USE && targetEntityId != -1 && entity.getId() == targetEntityId;
    }

    public static void reset() {
        stop(MinecraftClient.getInstance());
        lastAbortMs = 0;
        lastAbortMode = Mode.NONE;
    }

    public static Mode getMode() { return mode; }
    public static long getLastAbortMs() { return lastAbortMs; }
    public static Mode getLastAbortMode() { return lastAbortMode; }
}
