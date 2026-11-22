package com.safeinteract.util;

import com.safeinteract.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class Feedback {
    private Feedback() {}

    public static MutableText prefix() {
        return Text.empty()
                .append(Text.literal("[").formatted(Formatting.DARK_GRAY))
                .append(Text.literal("SafeInteract").formatted(Formatting.AQUA))
                .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
    }

    public static void actionBar(Text body) {
        ModConfig config = ModConfig.get();
        if (config.enabled && config.actionBarEnabled) show(body);
    }

    public static void disabled() {
        if (ModConfig.get().actionBarEnabled) show(Text.translatable("message.safeinteract.disabled"));
    }

    private static void show(Text body) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null) client.inGameHud.setOverlayMessage(prefix().append(Text.empty().formatted(Formatting.WHITE).append(body)), false);
    }

    public static void sound(RegistryEntry<SoundEvent> sound, float pitch) {
        ModConfig config = ModConfig.get();
        if (config.soundsEnabled && config.soundVolume > 0) {
            MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(sound.value(), pitch, config.soundVolume / 100f));
        }
    }

    public static Text onOff(boolean on) {
        return Text.translatable(on ? "message.safeinteract.on" : "message.safeinteract.off")
                .formatted(on ? Formatting.GREEN : Formatting.RED);
    }
}
