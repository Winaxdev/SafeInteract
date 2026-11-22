package com.safeinteract;

import com.safeinteract.config.ModConfig;
import com.safeinteract.config.SafeInteractScreen;
import com.safeinteract.handler.AutoHoldHandler;
import com.safeinteract.handler.InteractionStateHandler;
import com.safeinteract.handler.ServerPolicy;
import com.safeinteract.util.Feedback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeInteractClient implements ClientModInitializer {
    public static final String MOD_ID = "safeinteract";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "general"));
    public static KeyBinding PIERCE_KEY;
    public static KeyBinding HOLD_USE_KEY;
    public static KeyBinding HOLD_ATTACK_KEY;
    public static KeyBinding BUILD_GUARD_KEY;
    public static KeyBinding OPEN_SETTINGS_KEY;

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        PIERCE_KEY = register("pierce", GLFW.GLFW_KEY_LEFT_ALT);
        HOLD_USE_KEY = register("hold_use", GLFW.GLFW_KEY_R);
        HOLD_ATTACK_KEY = register("hold_attack", GLFW.GLFW_KEY_G);
        BUILD_GUARD_KEY = register("build_guard", GLFW.GLFW_KEY_UNKNOWN);
        OPEN_SETTINGS_KEY = register("open_settings", GLFW.GLFW_KEY_UNKNOWN);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ServerPolicy.onJoin(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                !ServerPolicy.onGameMessage(MinecraftClient.getInstance(), message));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) reset();
            else AutoHoldHandler.checkScreen(client);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("safeinteract").executes(ctx -> {
                    MinecraftClient client = ctx.getSource().getClient();
                    client.send(() -> client.setScreen(new SafeInteractScreen(null)));
                    return 1;
                }).then(ClientCommandManager.literal("reload").executes(ctx -> {
                    ModConfig.load();
                    ctx.getSource().sendFeedback(Feedback.prefix().append(Text.translatable("message.safeinteract.reloaded")));
                    return 1;
                }))));
    }

    private static KeyBinding register(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.safeinteract." + name, InputUtil.Type.KEYSYM, key, KEY_CATEGORY));
    }

    private static void reset() {
        AutoHoldHandler.reset();
        InteractionStateHandler.reset();
        ServerPolicy.reset();
    }

    public static void onPreInput(MinecraftClient client) {
        if (drain(OPEN_SETTINGS_KEY)) {
            client.setScreen(new SafeInteractScreen(null));
            return;
        }
        ModConfig config = ModConfig.get();
        if (drain(PIERCE_KEY) && config.enabled && config.entityPierceEnabled && ServerPolicy.isPierceBlocked()) {
            Feedback.actionBar(Text.translatable("message.safeinteract.server_disabled", Text.translatable("feature.safeinteract.entity_pierce")));
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
                Feedback.actionBar(Text.translatable("message.safeinteract.build_guard", Feedback.onOff(config.buildGuardEnabled)));
                Feedback.sound(SoundEvents.UI_BUTTON_CLICK, config.buildGuardEnabled ? 1.5f : 0.8f);
            }
        }
        AutoHoldHandler.tick(client);
        InteractionStateHandler.tick(client);
    }

    private static boolean drain(KeyBinding key) {
        boolean pressed = false;
        while (key != null && key.wasPressed()) pressed = true;
        return pressed;
    }
}
