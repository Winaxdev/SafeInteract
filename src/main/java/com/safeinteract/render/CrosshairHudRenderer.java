package com.safeinteract.render;

import com.safeinteract.config.ModConfig;
import com.safeinteract.handler.AutoHoldHandler;
import com.safeinteract.handler.InteractionStateHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Util;

public final class CrosshairHudRenderer {
    private CrosshairHudRenderer() {}

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        ModConfig c = ModConfig.get();
        if (!c.enabled || !c.hudEnabled || MinecraftClient.getInstance().player == null) return;
        int x = context.getScaledWindowWidth() / 2 + c.hudOffsetX;
        int y = context.getScaledWindowHeight() / 2 + c.hudOffsetY;
        long now = Util.getMeasuringTimeMs();
        float opacity = c.hudOpacity / 100f;
        boolean shieldDrawn = false;
        if (c.hudShowPierce && InteractionStateHandler.isPierceActive()) {
            shield(context, x, y, 0x26E6FF, 0xB2F5FF, 0.9f * opacity);
            shieldDrawn = true;
        }
        long lastGuard = InteractionStateHandler.getLastGuardBlockMs();
        long guardAge = now - lastGuard;
        if (c.hudShowGuardPulse && lastGuard != 0 && guardAge >= 0 && guardAge < 400) {
            shield(context, x, y, 0xFFB300, 0xFFE082, (1f - guardAge / 400f) * opacity);
            shieldDrawn = true;
        }
        if (c.hudShowHold) {
            int hx = x + (shieldDrawn ? 11 : 0);
            int hy = y + 2;
            if (AutoHoldHandler.getMode() != AutoHoldHandler.Mode.NONE) {
                float pulse = (float) (0.45 + 0.55 * (0.5 + 0.5 * Math.sin(2 * Math.PI * now / 1000.0)));
                ring(context, hx, hy, 0x2BE38A, pulse * opacity, opacity);
            } else {
                long lastAbort = AutoHoldHandler.getLastAbortMs();
                long abortAge = now - lastAbort;
                if (lastAbort != 0 && abortAge >= 0 && abortAge < 1200) {
                    float alpha = (1f - abortAge / 1200f) * opacity;
                    ring(context, hx, hy, 0xFF4D4D, alpha, alpha);
                }
            }
        }
    }

    private static void shield(DrawContext context, int x, int y, int body, int highlight, float alpha) {
        if (alpha < 0.02f) return;
        for (int row = 0; row <= 10; row++) {
            int start = row == 0 || row == 7 ? 1 : row == 8 ? 2 : row == 9 ? 3 : row == 10 ? 4 : 0;
            rect(context, x + start, y + row, x + 9 - start, y + row + 1, 0, alpha * 0.7f);
        }
        for (int row = 1; row <= 9; row++) {
            int start = row <= 6 ? 1 : row == 7 ? 2 : row == 8 ? 3 : 4;
            rect(context, x + start, y + row, x + 9 - start, y + row + 1, body, alpha);
        }
        rect(context, x + 2, y + 2, x + 3, y + 6, highlight, alpha);
        rect(context, x + 3, y + 3, x + 6, y + 5, 0, alpha * 0.7f);
        rect(context, x + 4, y + 5, x + 5, y + 7, 0, alpha * 0.7f);
    }

    private static void ring(DrawContext context, int x, int y, int color, float ringAlpha, float dotAlpha) {
        rect(context, x + 1, y, x + 6, y + 1, color, ringAlpha);
        rect(context, x + 1, y + 6, x + 6, y + 7, color, ringAlpha);
        rect(context, x, y + 1, x + 1, y + 6, color, ringAlpha);
        rect(context, x + 6, y + 1, x + 7, y + 6, color, ringAlpha);
        rect(context, x + 2, y + 2, x + 5, y + 5, color, dotAlpha);
    }

    private static void rect(DrawContext context, int x1, int y1, int x2, int y2, int rgb, float alpha) {
        if (alpha < 0.02f) return;
        int a = Math.min(255, Math.round(alpha * 255));
        context.fill(x1, y1, x2, y2, (a << 24) | rgb);
    }
}
