package com.safeinteract;

import com.mojang.blaze3d.platform.InputConstants;
import com.safeinteract.config.ModConfig;
import com.safeinteract.config.SafeInteractScreen;
import com.safeinteract.handler.AutoHoldHandler;
import com.safeinteract.handler.InteractionStateHandler;
import com.safeinteract.handler.ServerPolicy;
import com.safeinteract.util.Feedback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeInteractClient implements ClientModInitializer {
    public static final String MOD_ID = "safeinteract";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "general"));
    public static KeyMapping PIERCE_KEY;
    public static KeyMapping HOLD_USE_KEY;
    public static KeyMapping HOLD_ATTACK_KEY;
    public static KeyMapping BUILD_GUARD_KEY;
    public static KeyMapping OPEN_SETTINGS_KEY;

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        PIERCE_KEY = register("pierce", InputConstants.KEY_LALT);
        HOLD_USE_KEY = register("hold_use", InputConstants.KEY_R);
        HOLD_ATTACK_KEY = register("hold_attack", InputConstants.KEY_G);
        BUILD_GUARD_KEY = register("build_guard", InputConstants.UNKNOWN.getValue());
        OPEN_SETTINGS_KEY = register("open_settings", InputConstants.UNKNOWN.getValue());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ServerPolicy.onJoin(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                !ServerPolicy.onGameMessage(Minecraft.getInstance(), message));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) reset();
            else AutoHoldHandler.checkScreen(client);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("safeinteract").executes(ctx -> {
                    Minecraft client = ctx.getSource().getClient();
                    client.schedule(() -> client.gui.setScreen(new SafeInteractScreen(null)));
                    return 1;
                }).then(ClientCommands.literal("reload").executes(ctx -> {
                    ModConfig.load();
                    ctx.getSource().sendFeedback(Feedback.prefix().append(Component.translatable("message.safeinteract.reloaded")));
                    return 1;
                }))));
    }

    private static KeyMapping register(String name, int key) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.safeinteract." + name, InputConstants.Type.KEYBOARD, key, KEY_CATEGORY));
    }

    private static void reset() {
        AutoHoldHandler.reset();
        InteractionStateHandler.reset();
        ServerPolicy.reset();
    }

    public static void onPreInput(Minecraft client) {
        if (drain(OPEN_SETTINGS_KEY)) {
            client.gui.setScreen(new SafeInteractScreen(null));
            return;
        }
        ModConfig config = ModConfig.get();
        if (drain(PIERCE_KEY) && config.enabled && config.entityPierceEnabled && ServerPolicy.isPierceBlocked()) {
            Feedback.actionBar(Component.translatable("message.safeinteract.server_disabled", Component.translatable("feature.safeinteract.entity_pierce")));
        }
        if (drain(HOLD_USE_KEY)) {
            if (ModConfig.get().enabled) AutoHoldHandler.toggle(client, AutoHoldHandler.Mode.USE);
            else Feedback.disabled();
        }
        if (drain(HOLD_ATTACK_KEY)) {
            if (ModConfig.get().enabled) AutoHoldHandler.toggle(client, AutoHoldHandler.Mode.ATTACK);
            else Feedback.disabled();
        }
        if (drain(BUILD_GUARD_KEY)) {
            if (!config.enabled) Feedback.disabled();
            else {
                config.buildGuardEnabled = !config.buildGuardEnabled;
                ModConfig.save();
                Feedback.actionBar(Component.translatable("message.safeinteract.build_guard", Feedback.onOff(config.buildGuardEnabled)));
                Feedback.sound(SoundEvents.UI_BUTTON_CLICK, config.buildGuardEnabled ? 1.5f : 0.8f);
            }
        }
        AutoHoldHandler.tick(client);
        InteractionStateHandler.tick(client);
    }

    private static boolean drain(KeyMapping key) {
        boolean pressed = false;
        while (key != null && key.consumeClick()) pressed = true;
        return pressed;
    }
}
