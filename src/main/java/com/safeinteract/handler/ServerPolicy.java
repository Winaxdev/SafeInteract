package com.safeinteract.handler;

import com.safeinteract.util.Feedback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class ServerPolicy {
    public static final String NO_HOLD = "§s§i§q§h";
    public static final String NO_PIERCE = "§s§i§q§p";

    private static boolean holdBlocked;
    private static boolean pierceBlocked;

    private ServerPolicy() {}

    public static boolean isHoldBlocked() { return holdBlocked; }
    public static boolean isPierceBlocked() { return pierceBlocked; }

    public static void reset() {
        holdBlocked = false;
        pierceBlocked = false;
    }

    public static void onJoin(MinecraftClient client) {
        reset();
        ServerInfo server = client.getCurrentServerEntry();
        if (server != null && server.label != null) apply(client, server.label.getString());
    }

    public static boolean onGameMessage(MinecraftClient client, Text message) {
        String text = message.getString();
        boolean hasToken = text.contains(NO_HOLD) || text.contains(NO_PIERCE);
        apply(client, text);
        return hasToken && text.replace(NO_HOLD, "").replace(NO_PIERCE, "").isBlank();
    }

    private static void apply(MinecraftClient client, String text) {
        boolean newHold = text.contains(NO_HOLD) && !holdBlocked;
        boolean newPierce = text.contains(NO_PIERCE) && !pierceBlocked;
        holdBlocked |= newHold;
        pierceBlocked |= newPierce;
        if (newHold || newPierce) {
            if (client.player != null) {
                MutableText list = Text.empty();
                if (holdBlocked) list.append(Text.translatable("feature.safeinteract.smart_hold"));
                if (pierceBlocked) {
                    if (holdBlocked) list.append(", ");
                    list.append(Text.translatable("feature.safeinteract.entity_pierce"));
                }
                client.player.sendMessage(Feedback.prefix().append(Text.translatable("message.safeinteract.server_policy", list)), false);
            }
            if (newHold) AutoHoldHandler.stopByServer(client);
        }
    }
}
