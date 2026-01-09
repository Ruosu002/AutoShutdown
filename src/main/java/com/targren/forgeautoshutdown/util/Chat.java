package com.targren.forgeautoshutdown.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Static utility class for chat functions (syntactic sugar)
 */
public class Chat
{
    /**
     * Broadcasts an auto translated message to all players
     * @param server Server instance to broadcast to
     * @param msg String or language key to broadcast
     * @param parts Optional objects to add to formattable message
     */
    public static void toAll(MinecraftServer server, String msg, Object... parts)
    {
        toAll(server, Component.translatable(msg, parts));
    }

    public static void toAll(MinecraftServer server, Component message)
    {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    /**
     * Sends an automatically translated, formatted & encapsulated message to a command source
     * @param source Target to send message to
     * @param msg String or language key to broadcast
     * @param parts Optional objects to add to formattable message
     */
    public static void to(CommandSourceStack source, String msg, Object... parts)
    {
        to(source, Component.translatable(msg, parts));
    }

    public static void to(CommandSourceStack source, Component message)
    {
        source.sendSystemMessage(message);
    }
}
