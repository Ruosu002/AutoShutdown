package com.targren.forgeautoshutdown;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(
    modid   = ForgeAutoShutdown.MODID,
    name    = ForgeAutoShutdown.MODID,
    version = ForgeAutoShutdown.VERSION,

    acceptableRemoteVersions = "*",
    acceptableSaveVersions   = "",
        serverSideOnly = true
)
public class ForgeAutoShutdown
{
    public static final String VERSION = "1.12.2-0.0.1";
    public static final String MODID   = "forgeautoshutdown";
    public static final Logger LOGGER  = LogManager.getFormatterLogger(MODID);

    public static MinecraftServer server;

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void clientPreInit(FMLPreInitializationEvent event)
    {
        LOGGER.error("This mod is intended only for use on servers.");
        LOGGER.error("Please consider removing this mod from your installation.");
    }

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverPreInit(FMLPreInitializationEvent event)
    {
        Config.init( event.getSuggestedConfigurationFile() );
    }

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverStart(FMLServerStartingEvent event)
    {
        server = event.getServer();
        if ( Config.isNothingEnabled() )
        {
            LOGGER.warn("It appears no ForgeAutoShutdown features are enabled.");
            LOGGER.warn("Please check the config at `config/forgeautoshutdown.cfg`.");
            return;
        }

        if (Config.scheduleEnabled)
            ShutdownTask.create();

        if (Config.voteEnabled)
            ShutdownCommand.create(event);

        if (Config.watchdogEnabled)
            WatchdogTask.create();
    }
}
