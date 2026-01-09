package com.targren.forgeautoshutdown;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ForgeAutoShutdown.MODID)
public class ForgeAutoShutdown
{
    public static final String MODID = "forgeautoshutdown";
    public static final String VERSION = "1.19.2-1.1.0";
    public static final Logger LOGGER = LogManager.getLogger();

    private static MinecraftServer server;

    public ForgeAutoShutdown()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    public static MinecraftServer getServer()
    {
        return server;
    }

    private void onRegisterCommands(RegisterCommandsEvent event)
    {
        ShutdownCommand.register(event.getDispatcher());
    }

    private void onServerStarting(ServerStartingEvent event)
    {
        server = event.getServer();
        Config.validate();

        if (Config.isNothingEnabled())
        {
            LOGGER.warn("It appears no ForgeAutoShutdown features are enabled.");
            LOGGER.warn("Please check the config at `world/serverconfig/forgeautoshutdown-server.toml`.");
            return;
        }

        if (Config.scheduleEnabled.get())
            ShutdownTask.create(server);

        if (Config.watchdogEnabled.get())
            WatchdogTask.create(server);
    }
}
