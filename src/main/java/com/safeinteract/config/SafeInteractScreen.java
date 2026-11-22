package com.safeinteract.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class SafeInteractScreen extends GameOptionsScreen {
    public SafeInteractScreen(Screen parent) {
        super(parent, MinecraftClient.getInstance().options, Text.translatable("config.safeinteract.title"));
    }

    @Override
    protected void addOptions() {
        ModConfig c = ModConfig.get();
        body.addAll(
                bool("enabled", c.enabled, v -> c.enabled = v),
                bool("actionBarEnabled", c.actionBarEnabled, v -> c.actionBarEnabled = v),
                bool("buildGuardEnabled", c.buildGuardEnabled, v -> c.buildGuardEnabled = v),
                bool("entityGuardEnabled", c.entityGuardEnabled, v -> c.entityGuardEnabled = v),
                bool("entityPierceEnabled", c.entityPierceEnabled, v -> c.entityPierceEnabled = v),
                bool("durabilityGuardEnabled", c.durabilityGuardEnabled, v -> c.durabilityGuardEnabled = v),
                integer("durabilityThreshold", c.durabilityThreshold, 1, 50, "", v -> c.durabilityThreshold = v),
                bool("durabilityGuardManualTools", c.durabilityGuardManualTools, v -> c.durabilityGuardManualTools = v),
                bool("durabilityGuardMining", c.durabilityGuardMining, v -> c.durabilityGuardMining = v),
                bool("holdFullInventoryGuardUse", c.holdFullInventoryGuardUse, v -> c.holdFullInventoryGuardUse = v),
                bool("holdDamageGuard", c.holdDamageGuard, v -> c.holdDamageGuard = v),
                bool("holdSlotGuard", c.holdSlotGuard, v -> c.holdSlotGuard = v),
                bool("holdMovementGuard", c.holdMovementGuard, v -> c.holdMovementGuard = v),
                bool("holdTargetLostGuard", c.holdTargetLostGuard, v -> c.holdTargetLostGuard = v),
                bool("hudEnabled", c.hudEnabled, v -> c.hudEnabled = v),
                integer("hudOpacity", c.hudOpacity, 10, 100, "%", v -> c.hudOpacity = v),
                bool("soundsEnabled", c.soundsEnabled, v -> c.soundsEnabled = v),
                integer("soundVolume", c.soundVolume, 0, 100, "%", v -> c.soundVolume = v)
        );
    }

    private static SimpleOption<Boolean> bool(String name, boolean value, Consumer<Boolean> save) {
        String key = "config.safeinteract.option." + name;
        return SimpleOption.ofBoolean(key, SimpleOption.constantTooltip(Text.translatable(key + ".tooltip")), value, save);
    }

    private static SimpleOption<Integer> integer(String name, int value, int min, int max, String suffix, Consumer<Integer> save) {
        String key = "config.safeinteract.option." + name;
        return new SimpleOption<>(key, SimpleOption.constantTooltip(Text.translatable(key + ".tooltip")),
                (text, v) -> GameOptions.getGenericValueText(text, Text.literal(v + suffix)),
                new SimpleOption.ValidatingIntSliderCallbacks(min, max), value, save);
    }

    @Override
    public void removed() {
        super.removed();
        ModConfig.save();
    }

    @Override
    protected void initFooter() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            super.initFooter();
            return;
        }
        DirectionalLayoutWidget row = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
        row.add(ButtonWidget.builder(Text.translatable("config.safeinteract.advanced"),
                b -> client.setScreen(ClothConfigScreen.create(this))).width(150).build());
        row.add(ButtonWidget.builder(ScreenTexts.DONE, b -> close()).width(150).build());
    }
}
