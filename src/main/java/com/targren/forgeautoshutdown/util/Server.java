package com.targren.forgeautoshutdown.util;

import com.targren.forgeautoshutdown.ForgeAutoShutdown;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import org.apache.logging.log4j.Logger;

/**
 * Static utility class for server functions
 */
public class Server
{
    private static final Logger LOGGER = ForgeAutoShutdown.LOGGER;

    /** Kicks all players from the server with given reason, then shuts server down */
    public static void shutdown(MinecraftServer server, Component message)
    {
        if (server == null)
            return;

        for (ServerPlayer player : server.getPlayerList().getPlayers())
            player.connection.disconnect(message);

        LOGGER.info("Shutdown initiated because: {}", message.getString());
        server.halt(false);
    }

    /** Checks if any non-fake player is present on the server */
    public static boolean hasRealPlayers(MinecraftServer server)
    {
        if (server == null)
            return false;

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            if (!(player instanceof FakePlayer))
                return true;
        }

        return false;
    }
}
