package com.targren.forgeautoshutdown;

import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Singleton that acts as a timer task for monitoring server stalls
 */
public class WatchdogTask extends TimerTask
{
    private static WatchdogTask INSTANCE;
    private static MinecraftServer SERVER;
    private static Logger LOGGER;

    public static void create(MinecraftServer server)
    {
        if (INSTANCE != null)
            throw new RuntimeException("WatchdogTask can only be created once");

        INSTANCE = new WatchdogTask();
        SERVER = server;
        LOGGER = ForgeAutoShutdown.LOGGER;

        Timer timer = new Timer("ForgeAutoShutdown watchdog");
        int intervalMs = Config.watchdogInterval.get() * 1000;
        timer.schedule(INSTANCE, intervalMs, intervalMs);
        LOGGER.debug("Watchdog timer running");
    }

    private int lastTick = 0;
    private int hungTicks = 0;
    private int lagTicks = 0;

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
        double averageTickTime = SERVER.getAverageTickTime();
        double tps = averageTickTime <= 0 ? 20.0 : Math.min(1000.0 / averageTickTime, 20.0);

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Watchdog: 100 tick avg. latency: {} / 50 ms", averageTickTime);
            LOGGER.trace("Watchdog: 100 tick avg. TPS: {} / 20", String.format("%.2f", tps));
        }

        int serverTick = SERVER.getTickCount();
        if (serverTick == lastTick)
        {
            LOGGER.debug("No advance in server ticks; server is hanging");
            isHanging = true;
            hungTicks = 1;
            return;
        }
        else
            lastTick = serverTick;

        if (tps < Config.lowTPSThreshold.get())
        {
            lagTicks++;
            int lagSec = lagTicks * Config.watchdogInterval.get();
            LOGGER.trace("TPS too low since {} seconds", lagSec);

            if (lagSec >= Config.lowTPSTimeout.get())
            {
                LOGGER.warn(
                    "TPS below {} since {} seconds",
                    Config.lowTPSThreshold.get(),
                    lagSec
                );

                if (Config.attemptSoftKill.get())
                    performSoftKill();
                else
                    performHardKill();
            }
        }
        else
            lagTicks = 0;
    }

    /** Regular check of a hanging server; kills if confirmed hung */
    private void doHanging()
    {
        int serverTick = SERVER.getTickCount();
        if (serverTick != lastTick)
        {
            LOGGER.debug("Server no longer hanging");
            isHanging = false;
            return;
        }

        hungTicks++;
        int hangSec = hungTicks * Config.watchdogInterval.get();
        LOGGER.trace("Server hanging for {} seconds", hangSec);

        if (hangSec >= Config.maxTickTimeout.get())
        {
            LOGGER.warn("Server is hung on a tick after {} seconds", hangSec);

            if (Config.attemptSoftKill.get())
                performSoftKill();
            else
                performHardKill();
        }
    }

    private void performSoftKill()
    {
        LOGGER.warn("Attempting a soft kill of the server...");

        Thread hardKillCheck = new Thread("Shutdown watchdog")
        {
            public void run()
            {
                try
                {
                    Thread.sleep(10000);
                    System.out.println("Hung during soft kill; trying a hard kill..");
                    performHardKill();
                }
                catch (InterruptedException ignored) { }
            }
        };

        hardKillCheck.setDaemon(true);
        hardKillCheck.start();

        SERVER.halt(false);
    }

    private void performHardKill()
    {
        LOGGER.warn("Attempting a hard kill of the server - data may be lost!");
        Runtime.getRuntime().halt(1);
    }

    private WatchdogTask() { }
}
