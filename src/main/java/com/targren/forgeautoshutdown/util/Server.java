package com.targren.forgeautoshutdown.util;

import com.targren.forgeautoshutdown.ForgeAutoShutdown;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.util.FakePlayer;
import org.apache.logging.log4j.Logger;

/**
 * Static utility class for server functions
 */
public class Server
{
    // Safe to declare here. This class should only ever be loaded if any of its methods
    // are called, which by then these should be available.
    static final MinecraftServer SERVER = ForgeAutoShutdown.server;
    static final Logger          LOGGER = ForgeAutoShutdown.LOGGER;

    /** Kicks all players from the server with given reason, then shuts server down */
    public static void shutdown(String reason)
    {
        ITextComponent message = new TextComponentString(reason);
        reason = message.getFormattedText();

        //for ( Object value : SERVER.getPlayerList().getPlayerList().toArray() )
        for ( Object value : SERVER.getPlayerList().getPlayers().toArray() )
        {
            EntityPlayerMP player = (EntityPlayerMP) value;
            player.connection.disconnect(message);
            //player.connection.kickPlayerFromServer(reason);
        }

        LOGGER.debug("Shutdown initiated because: %s", reason);
        SERVER.initiateShutdown();
    }

    /** Checks if any non-fake player is present on the server */
    public static boolean hasRealPlayers()
    {
        for ( Object value : SERVER.getPlayerList().getPlayers().toArray() )
            if (value instanceof EntityPlayerMP)
            if ( !(value instanceof FakePlayer) )
                return true;
        return false;
    }
}
