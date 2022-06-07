A 1.12.2 Fork of "ForgeAutoShutdown" for 1.10.2 ([Repository](https://github.com/abused/ForgeAutoShutdown))

This mod must be installed on both client and server sides. It doesn't add any commands to SP worlds, but is necessary for localized messages.

ForgeAutoShutdown is a mod that can:

* Schedule the server to automatically shut down at a specific time of day, or after X
hours and minutes of uptime. This allows the server to be automatically restarted by a
shell script, Windows batch file or service.
* Allow players to vote for a manual shutdown, so a lagged out server does not require
admin intervention
* Shutdown or kill a server that is hung (stalled) or laggy

# Requirements

* Minecraft Forge server 1.12.2, running forge [14.23.5.2859](http://files.minecraftforge.net/maven/net/minecraftforge/forge/index_1.12.2.html)
* A wrapper script that restarts the server after shutdown (optional, but highly recommended). 

# Features

*Any of these features may be disabled in the config*

## Scheduled shutdown
ForgeAutoShutdown will log a message at the INFO level on server startup, with a date and
time of the next scheduled shutdown. For example:

`[10:50:09] [Server thread/INFO] [ForgeAutoShutdown]: Next automatic shutdown: 08:30:00 09-June-2019`

If this message is missing, the mod has not been correctly installed or the schedule is
disabled in config. If the mod is installed on a Minecraft client, it will log an ERROR to
the console and not perform any function. It will not crash or disable the client.

### Mode
By default, the shutdown will be scheduled to happen at a specific time of day. This is
the time local to the server and will always happen within the next 24 hours after server
startup. This means that if the server starts and has missed the shutdown time even by a
few minutes, it will schedule for the next day.

Alternatively, setting `Uptime` to true means the server can shutdown after a specific
amount of hours or minutes instead. This can allow the server to restart multiple times a
day, or after a few days, etc.

### Warnings
By default a scheduled shutdown will give a warning to all players, each minute for five
minutes, after the scheduled time. This can be disabled by setting `Warnings` to `false`.
This means the server will shutdown, without warning, by the scheduled time.

### Delay
If desired, the shutdown can be delayed by a configurable amount if players are still on
the server. To enable this, set `Delay` to true and adjust `DelayBy` to the amount of
minutes to delay.

The shutdown will be repeatedly delayed until the server is empty. When checking if the
server for players, fake players are excluded. Note that shutdown warnings are ineffective 
with delays, and a pending shutdown will be cancelled if a player comes online during the countdown.

## Voting

If enabled, players may vote a manual shutdown. To do so, a player must execute
`/shutdown`. Then, **all** players (including the vote initiator) must vote using
`/shutdown yes` or `/shutdown no`.

If the amount of `no` votes reach a maximum threshold, the vote fails. If a vote is cast
and too many players have disconnected in the meantime, the vote fails. If a vote fails,
another one will not be able to start until a configured amount of minutes has passed.

If the vote succeeds, the server will instantly shutdown without warning. If an
appropriate means of automatic restart is in place, it should be expected that the server
will go up within a few minutes.

## Watchdog (Experimental)

If enabled, a watchdog thread can periodically watch the server for unresponsiveness. By
default, it checks every 10 seconds:

* Whether the server is hanging (or "stalling") on a tick
* Whether the TPS stays below a certain amount for a certain length of time

If either problem is detected, the watchdog will try a soft kill (or a hard kill, if
configured). This makes the server try to save all its data before shutting down. If a
soft kill takes longer than ten seconds, the watchdog will do a hard kill.
