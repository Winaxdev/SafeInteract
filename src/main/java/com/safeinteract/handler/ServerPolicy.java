package com.safeinteract.handler;

import com.safeinteract.util.Feedback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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

    public static void onJoin(Minecraft client) {
        reset();
        ServerData server = client.getCurrentServer();
        if (server != null && server.motd != null) apply(client, server.motd.getString());
    }

    public static boolean onGameMessage(Minecraft client, Component message) {
        String text = message.getString();
        boolean hasToken = text.contains(NO_HOLD) || text.contains(NO_PIERCE);
        apply(client, text);
        return hasToken && text.replace(NO_HOLD, "").replace(NO_PIERCE, "").isBlank();
    }

    private static void apply(Minecraft client, String text) {
        boolean newHold = text.contains(NO_HOLD) && !holdBlocked;
        boolean newPierce = text.contains(NO_PIERCE) && !pierceBlocked;
        holdBlocked |= newHold;
        pierceBlocked |= newPierce;
        if (newHold || newPierce) {
            if (client.player != null) {
                MutableComponent list = Component.empty();
                if (holdBlocked) list.append(Component.translatable("feature.safeinteract.smart_hold"));
                if (pierceBlocked) {
                    if (holdBlocked) list.append(", ");
                    list.append(Component.translatable("feature.safeinteract.entity_pierce"));
                }
                client.showDebugChat(Feedback.prefix().append(Component.translatable("message.safeinteract.server_policy", list)));
            }
            if (newHold) AutoHoldHandler.stopByServer(client);
        }
    }
}
