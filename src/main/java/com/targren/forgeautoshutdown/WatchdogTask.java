package com.targren.forgeautoshutdown;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Singleton that acts as a timer task for monitoring server stalls
 */
public class WatchdogTask extends TimerTask
{
    private static WatchdogTask    INSTANCE;
    private static MinecraftServer SERVER;
    private static Logger          LOGGER;

    public static void create()
    {
        if (INSTANCE != null)
            throw new RuntimeException("WatchdogTask can only be created once");

        INSTANCE = new WatchdogTask();
        SERVER   = ForgeAutoShutdown.server;
        LOGGER   = ForgeAutoShutdown.LOGGER;

        Timer timer      = new Timer("ForgeAutoShutdown watchdog");
        int   intervalMs = Config.watchdogInterval * 1000;
        timer.schedule(INSTANCE, intervalMs, intervalMs);
        LOGGER.debug("Watchdog timer running");
    }

    private int lastTick  = 0;
    private int hungTicks = 0;
    private int lagTicks  = 0;

    private boolean isHanging = false;

    @Override
    public void run()
    {
        if (isHanging)
            doHanging();
        else
            doMonitor();
    }

    /** Checks if server is hung on a tick, then if TPS is too low for too long */
    private void doMonitor()
    {
        double latency = MathHelper.average(SERVER.tickTimeArray) * 0.000001;
        double tps     = Math.min(1000 / latency, 20);

        if ( LOGGER.isTraceEnabled() )
        {
            LOGGER.trace("Watchdog: 100 tick avg. latency: %.2f / 50 ms", latency);
            LOGGER.trace("Watchdog: 100 tick avg. TPS: %.2f / 20", tps);
        }

        // First, check if server is hung on a tick
        int serverTick = SERVER.getTickCounter();
        if (serverTick == lastTick)
        {
            LOGGER.debug("No advance in server ticks; server is hanging");
            isHanging = true;
            hungTicks = 1;
            return;
        }
        else lastTick = serverTick;

        // Second, check if TPS has been too low for too long
        if (tps < Config.lowTPSThreshold)
        {
            lagTicks++;
            int lagSec = lagTicks * Config.watchdogInterval;
            LOGGER.trace("TPS too low since %d seconds", lagSec);

            if (lagSec >= Config.lowTPSTimeout)
            {
                LOGGER.warn(
                    "TPS below %d since %d seconds",
                    Config.lowTPSThreshold, lagSec
                );

                if (Config.attemptSoftKill)
                    performSoftKill();
                else
                    performHardKill();
            }
        }
        else lagTicks = 0;
    }

    /** Regular check of a hanging server; kills if confirmed hung */
    private void doHanging()
    {
        int serverTick = SERVER.getTickCounter();
        if (serverTick != lastTick)
        {
            LOGGER.debug("Server no longer hanging");
            isHanging = false;
            return;
        }

        hungTicks++;
        int hangSec = hungTicks * Config.watchdogInterval;
        LOGGER.trace("Server hanging for %d seconds", hangSec);

        if (hangSec >= Config.maxTickTimeout)
        {
            LOGGER.warn("Server is hung on a tick after %d seconds", hangSec);

            if (Config.attemptSoftKill)
                performSoftKill();
            else
                performHardKill();
        }
    }

    private void performSoftKill()
    {
        LOGGER.warn("Attempting a soft kill of the server...");

        // Using a new thread as the shutdown watchdog, because all timer tasks appear to
        // get canceled whilst runtime is exiting
        Thread hardKillCheck = new Thread("Shutdown watchdog")
        {
            public void run()
            {
                try
                {
                    // This will not delay the soft kill; this is not a shutdown task, so
                    // the Runtime exiting process will abort this thread
                    Thread.sleep(10000);
                    System.out.println("Hung during soft kill; trying a hard kill..");
                    performHardKill();
                }
                catch (InterruptedException e) { }
            }
        };

        hardKillCheck.start();

        // Use exitJava because MinecraftServer registers a shutdown hook, which in turn
        // attempts to save all worlds. This is also the preferred method of exiting the
        // JVM within Forge, so that exits may be logged with a stack trace
        FMLCommonHandler.instance().exitJava(1, false);
    }

    private void performHardKill()
    {
        LOGGER.warn("Attempting a hard kill of the server - data may be lost!");
        FMLCommonHandler.instance().exitJava(1, true);
    }

    private WatchdogTask() { }
}
