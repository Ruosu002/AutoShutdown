package com.targren.forgeautoshutdown;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.Logger;

/**
 * Static container class for mod's configuration values. Handles saving and loading.
 */
class Config
{
    private static final String SCHEDULE = "Schedule";
    private static final String VOTING = "Voting";
    private static final String WATCHDOG = "Watchdog";
    private static final String MESSAGES = "Messages";

    static final ForgeConfigSpec SPEC;

    static final ForgeConfigSpec.BooleanValue scheduleEnabled;
    static final ForgeConfigSpec.BooleanValue scheduleWarning;
    static final ForgeConfigSpec.BooleanValue scheduleDelay;
    static final ForgeConfigSpec.BooleanValue scheduleUptime;
    static final ForgeConfigSpec.IntValue scheduleHour;
    static final ForgeConfigSpec.IntValue scheduleMinute;
    static final ForgeConfigSpec.IntValue scheduleDelayBy;

    static final ForgeConfigSpec.BooleanValue voteEnabled;
    static final ForgeConfigSpec.IntValue voteInterval;
    static final ForgeConfigSpec.IntValue minVoters;
    static final ForgeConfigSpec.IntValue maxNoVotes;

    static final ForgeConfigSpec.BooleanValue watchdogEnabled;
    static final ForgeConfigSpec.BooleanValue attemptSoftKill;
    static final ForgeConfigSpec.IntValue watchdogInterval;
    static final ForgeConfigSpec.IntValue maxTickTimeout;
    static final ForgeConfigSpec.IntValue lowTPSThreshold;
    static final ForgeConfigSpec.IntValue lowTPSTimeout;

    static final ForgeConfigSpec.ConfigValue<String> msgWarn;
    static final ForgeConfigSpec.ConfigValue<String> msgKick;

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("All times are 24 hour (military) format, relative to machine's local time")
            .push(SCHEDULE);

        scheduleEnabled = builder.define("Enabled", true);
        scheduleWarning = builder.define("Warnings", true);
        scheduleDelay = builder.define("Delay", false);
        scheduleUptime = builder.define("Uptime", false);
        scheduleHour = builder.defineInRange("Hour", 5, 0, 720);
        scheduleMinute = builder.defineInRange("Minute", 0, 0, 59);
        scheduleDelayBy = builder.defineInRange("DelayBy", 5, 1, 1440);
        builder.pop();

        builder.comment("Allows players to shut down the server without admin intervention")
            .push(VOTING);

        voteEnabled = builder.define("VoteEnabled", false);
        voteInterval = builder.defineInRange("VoteInterval", 15, 0, 1440);
        minVoters = builder.defineInRange("MinVoters", 4, 1, 999);
        maxNoVotes = builder.defineInRange("MaxNoVotes", 2, 1, 999);
        builder.pop();

        builder.comment(
            "Monitors the server and tries to kill it if unresponsive. " +
            "USE AT RISK: May corrupt data if killed before or during save"
        ).push(WATCHDOG);

        watchdogEnabled = builder.define("Enabled", false);
        attemptSoftKill = builder.define("AttemptSoftKill", true);
        watchdogInterval = builder.defineInRange("Interval", 10, 1, 3600);
        maxTickTimeout = builder.defineInRange("Timeout", 40, 1, 3600);
        lowTPSThreshold = builder.defineInRange("LowTPSThreshold", 10, 0, 19);
        lowTPSTimeout = builder.defineInRange("LowTPSTimeout", 30, 1, 3600);
        builder.pop();

        builder.comment("Customizable messages for the shutdown process")
            .push(MESSAGES);

        msgWarn = builder.define("Warn", "Server is shutting down in %m minute(s).");
        msgKick = builder.define("Kick", "Scheduled server shutdown");
        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Checks the loaded configuration and makes adjustments based on other config
     */
    static void validate()
    {
        Logger logger = ForgeAutoShutdown.LOGGER;

        int hour = scheduleHour.get();
        int minute = scheduleMinute.get();

        if (!scheduleUptime.get() && hour >= 24)
        {
            logger.warn("Uptime shutdown is disabled, but the shutdown hour is more " +
                "than 23! Please fix this in the config. It will be set to 00 hours.");
            scheduleHour.set(0);
        }

        if (scheduleUptime.get() && hour == 0 && minute == 0)
        {
            logger.warn("Uptime shutdown is enabled, but is set to shutdown after " +
                "0 hours and 0 minutes of uptime! Please fix this in the config. " +
                "It will be set to 24 hours.");
            scheduleHour.set(24);
        }
    }

    static boolean isNothingEnabled()
    {
        return !scheduleEnabled.get() && !voteEnabled.get() && !watchdogEnabled.get();
    }

    private Config() { }
}
