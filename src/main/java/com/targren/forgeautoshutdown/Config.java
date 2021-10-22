package com.targren.forgeautoshutdown;

import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Static container class for mod's configuration values. Handles saving and loading.
 */
class Config
{
    private static final String SCHEDULE = "Schedule";
    private static final String VOTING   = "Voting";
    private static final String WATCHDOG = "Watchdog";
    private static final String MESSAGES = "Messages";

    static Configuration config;

    static boolean scheduleEnabled = true;
    static boolean scheduleWarning = true;
    static boolean scheduleDelay   = false;
    static boolean scheduleUptime  = false;
    static int     scheduleHour    = 5;
    static int     scheduleMinute  = 0;
    static int     scheduleDelayBy = 5;

    static boolean voteEnabled  = false;
    static int     voteInterval = 15;
    static int     minVoters    = 4;
    static int     maxNoVotes   = 2;

    static boolean watchdogEnabled  = false;
    static boolean attemptSoftKill  = true;
    static int     watchdogInterval = 10;
    static int     maxTickTimeout   = 40;
    static int     lowTPSThreshold  = 10;
    static int     lowTPSTimeout    = 30;

    static String msgWarn = "Server is shutting down in %m minute(s).";
    static String msgKick = "Scheduled server shutdown";

    /**
     * Populates the config values with saved or defaults. Automatically creates the
     * config file with defaults, if missing.
     * @param configFile File to use for loading/saving
     */
    static void init(File configFile)
    {
        config = new Configuration(configFile);

        config.setCategoryComment(SCHEDULE,
            "All times are 24 hour (military) format, relative to machine's local time");

        scheduleEnabled = config.getBoolean("Enabled", SCHEDULE, scheduleEnabled,
            "If true, server will automatically shutdown");
        scheduleWarning = config.getBoolean("Warnings", SCHEDULE, scheduleWarning,
            "If true, server will give five minutes of warnings prior to shutdown");
        scheduleDelay   = config.getBoolean("Delay", SCHEDULE, scheduleDelay,
            "If true, server will delay shutdown until server is empty");
        scheduleUptime  = config.getBoolean("Uptime", SCHEDULE, scheduleUptime,
            "If true, server will use Hour and Minute as uptime until shutdown.\n" +
            "If false, server will use Hour and Minute as time of day to shutdown.");
        scheduleHour    = config.getInt("Hour", SCHEDULE, scheduleHour, 0, 720,
            "Hour of the shutdown process (e.g. 8 for 8 AM OR 8 hours uptime)");
        scheduleMinute  = config.getInt("Minute", SCHEDULE, scheduleMinute, 0, 59,
            "Minute of the shutdown process (e.g. 30 for half-past OR 30 mins uptime)");

        scheduleDelayBy = config.getInt(
            "DelayBy", SCHEDULE, scheduleDelayBy, 1, 1440,
            "Minutes to delay scheduled shutdown by, if server is not empty"
        );

        config.setCategoryComment(VOTING,
            "Allows players to shut down the server without admin intervention");

        voteEnabled  = config.getBoolean("VoteEnabled", VOTING, voteEnabled,
            "If true, players may vote to shut server down using '/shutdown'");
        voteInterval = config.getInt("VoteInterval", VOTING, voteInterval, 0, 1440,
            "Min. minutes after a failed vote before new one can begin");
        minVoters    = config.getInt("MinVoters", VOTING, minVoters, 1, 999,
            "Min. players online required to begin a vote");
        maxNoVotes   = config.getInt("MaxNoVotes", VOTING, maxNoVotes, 1, 999,
            "Max. 'No' votes to cancel a shutdown");

        config.setCategoryComment(WATCHDOG,
            "Monitors the server and tries to kill it if unresponsive. " +
            "USE AT RISK: May corrupt data if killed before or during save");

        watchdogEnabled  = config.getBoolean("Enabled", WATCHDOG, watchdogEnabled,
            "If true, try to shutdown the server if unresponsive");
        attemptSoftKill  = config.getBoolean("AttemptSoftKill", WATCHDOG, attemptSoftKill,
            "If true, try to save worlds and data before forcing a kill. " +
            "WARNING: Setting 'false' is faster, but much higher risk of corruption");
        watchdogInterval = config.getInt("Interval", WATCHDOG, watchdogInterval, 1, 3600,
            "How many seconds between checking for an unresponsive server");
        maxTickTimeout   = config.getInt("Timeout", WATCHDOG, maxTickTimeout, 1, 3600,
            "Max. seconds a single server tick may last before killing");
        lowTPSThreshold  = config.getInt("LowTPSThreshold", WATCHDOG, lowTPSThreshold, 0, 19,
            "TPS below this value is considered 'too low'");
        lowTPSTimeout    = config.getInt("LowTPSTimeout", WATCHDOG, lowTPSTimeout, 1, 3600,
            "Max. seconds TPS may stay below threshold before killing");

        config.setCategoryComment(MESSAGES,
            "Customizable messages for the shutdown process");

        msgWarn = config.getString("Warn", MESSAGES, msgWarn,
            "Pre-shutdown warning message. Use %m for minutes remaining");
        msgKick = config.getString("Kick", MESSAGES, msgKick,
            "Message shown to player on disconnect during shutdown");

        check();
        config.save();
    }

    /**
     * Checks the loaded configuration and makes adjustments based on other config
     */
    static void check()
    {
        final Logger LOGGER = ForgeAutoShutdown.LOGGER;

        // Ensure daily shutdown hour is not set to more than 23:00
        if (!scheduleUptime && scheduleHour >= 24)
        {
            LOGGER.warn("Uptime shutdown is disabled, but the shutdown hour is more " +
                "than 23! Please fix this in the config. It will be set to 00 hours.");
            scheduleHour = 0;
        }

        // Ensure uptime shutdown is not set to zero hours zero minutes
        if (scheduleUptime && scheduleHour == 0 && scheduleMinute == 0)
        {
            LOGGER.warn("Uptime shutdown is enabled, but is set to shutdown after " +
                "0 hours and 0 minutes of uptime! Please fix this in the config. " +
                "It will be set to 24 hours.");
            scheduleHour = 24;
        }
    }

    static boolean isNothingEnabled()
    {
        return !scheduleEnabled && !voteEnabled && !watchdogEnabled;
    }

    private Config() { }
}
