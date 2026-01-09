package com.targren.forgeautoshutdown;

import com.targren.forgeautoshutdown.util.Chat;
import com.targren.forgeautoshutdown.util.Server;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Singleton that acts as a timer task and an event handler for daily shutdown.
 *
 * The use of a tick handler ensures the shutdown process is run in the main thread,
 * to prevent issues with cross-thread contamination. As the handler runs 20 times a
 * second, the event is just a boolean check. This means the scheduled task's role is
 * to unlock the tick handler.
 */
public class ShutdownTask extends TimerTask
{
    static final Format DATE = new SimpleDateFormat("HH:mm MMM d");

    private static ShutdownTask INSTANCE;
    private static MinecraftServer SERVER;
    private static Logger LOGGER;

    private boolean registered = false;

    /** Creates a timer task to run at the configured time of day */
    public static void create(MinecraftServer server)
    {
        if (INSTANCE != null)
            throw new RuntimeException("ShutdownTask can only be created once");

        INSTANCE = new ShutdownTask();
        SERVER = server;
        LOGGER = ForgeAutoShutdown.LOGGER;

        Timer timer = new Timer("ForgeAutoShutdown timer");
        Calendar shutdownAt = Calendar.getInstance();

        if (Config.scheduleUptime.get())
        {
            shutdownAt.add(Calendar.HOUR_OF_DAY, Config.scheduleHour.get());
            shutdownAt.add(Calendar.MINUTE, Config.scheduleMinute.get());
        }
        else
        {
            shutdownAt.set(Calendar.HOUR_OF_DAY, Config.scheduleHour.get());
            shutdownAt.set(Calendar.MINUTE, Config.scheduleMinute.get());
            shutdownAt.set(Calendar.SECOND, 0);

            if (shutdownAt.before(Calendar.getInstance()))
                shutdownAt.add(Calendar.DAY_OF_MONTH, 1);
        }

        Date shutdownAtDate = shutdownAt.getTime();

        timer.schedule(INSTANCE, shutdownAtDate, 60 * 1000);
        LOGGER.info("Next automatic shutdown: {}", DATE.format(shutdownAtDate));
    }

    boolean executeTick = false;
    byte warningsLeft = 5;
    int delayMinutes = 0;

    /** Runs from the timer thread */
    @Override
    public void run()
    {
        if (!registered)
        {
            MinecraftForge.EVENT_BUS.register(this);
            registered = true;
        }

        executeTick = true;
        LOGGER.debug("Timer called; next ShutdownTask tick will run");
    }

    /** Runs from the main server thread */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (!executeTick || event.phase == TickEvent.Phase.END)
            return;
        else
            executeTick = false;

        if (Config.scheduleDelay.get() && performDelay())
        {
            LOGGER.debug("ShutdownTask ticked; {} minute(s) of delay to go", delayMinutes);
            delayMinutes--;
            return;
        }

        if (Config.scheduleWarning.get() && warningsLeft > 0)
        {
            performWarning();
            LOGGER.debug("ShutdownTask ticked; {} warning(s) to go", warningsLeft);
        }
        else
        {
            Server.shutdown(SERVER, Component.literal(Config.msgKick.get()));
        }
    }

    private boolean performDelay()
    {
        if (delayMinutes > 0)
            return true;

        if (!Server.hasRealPlayers(SERVER))
            return false;

        warningsLeft = 5;
        delayMinutes += Config.scheduleDelayBy.get();
        LOGGER.info("Shutdown delayed by {} minutes; server is not empty", delayMinutes);
        return true;
    }

    private void performWarning()
    {
        String warning = Config.msgWarn.get().replace("%m", Byte.toString(warningsLeft));

        Chat.toAll(SERVER, Component.literal("*** " + warning));
        LOGGER.info(warning);
        warningsLeft--;
    }

    private ShutdownTask() { }
}
