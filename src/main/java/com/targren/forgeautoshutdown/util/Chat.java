package com.targren.forgeautoshutdown.util;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * Static utility class for chat functions (syntactic sugar)
 */
public class Chat
{
    /**
     * Broadcasts an auto. translated, formatted encapsulated message to all players
     * @param server Server instance to broadcast to
     * @param msg String or language key to broadcast
     * @param parts Optional objects to add to formattable message
     */
    public static void toAll(MinecraftServer server, String msg, Object... parts)
    {
        server.getServer().getPlayerList().sendMessage(new TextComponentTranslation(msg, parts), false);
    }

    /**
     * Sends an automatically translated, formatted & encapsulated message to a player
     * @param sender Target to send message to
     * @param msg String or language key to broadcast
     * @param parts Optional objects to add to formattable message
     */
    public static void to(ICommandSender sender, String msg, Object... parts)
    {
        sender.sendMessage(new TextComponentTranslation(msg, parts));
    }

}
