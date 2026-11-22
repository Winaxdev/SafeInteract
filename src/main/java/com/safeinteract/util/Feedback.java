package com.safeinteract.util;

import com.safeinteract.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;

public final class Feedback {
    private Feedback() {}

    public static MutableComponent prefix() {
        return Component.empty()
                .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("SafeInteract").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void actionBar(Component body) {
        ModConfig config = ModConfig.get();
        if (config.enabled && config.actionBarEnabled) show(body);
    }

    public static void disabled() {
        if (ModConfig.get().actionBarEnabled) show(Component.translatable("message.safeinteract.disabled"));
    }

    private static void show(Component body) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui != null) client.gui.hud.setOverlayMessage(prefix().append(Component.empty().withStyle(ChatFormatting.WHITE).append(body)), false);
    }

    public static void sound(Holder<SoundEvent> sound, float pitch) {
        ModConfig config = ModConfig.get();
        if (config.soundsEnabled && config.soundVolume > 0) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(sound.value(), pitch, config.soundVolume / 100f));
        }
    }

    public static Component onOff(boolean on) {
        return Component.translatable(on ? "message.safeinteract.on" : "message.safeinteract.off")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
