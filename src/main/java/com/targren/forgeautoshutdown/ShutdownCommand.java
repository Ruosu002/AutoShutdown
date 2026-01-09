package com.targren.forgeautoshutdown;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.targren.forgeautoshutdown.util.Chat;
import com.targren.forgeautoshutdown.util.Server;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Singleton that handles the `/shutdown` voting command
 */
public class ShutdownCommand
{
    private static final SimpleCommandExceptionType PLAYERS_ONLY =
        new SimpleCommandExceptionType(Component.translatable("forgeautoshutdown.error.playersonly"));
    private static final SimpleCommandExceptionType NO_VOTE_IN_PROGRESS =
        new SimpleCommandExceptionType(Component.translatable("forgeautoshutdown.error.novoteinprogress"));
    private static final SimpleCommandExceptionType VOTE_IN_PROGRESS =
        new SimpleCommandExceptionType(Component.translatable("forgeautoshutdown.error.voteinprogress"));
    private static final DynamicCommandExceptionType TOO_SOON =
        new DynamicCommandExceptionType(seconds ->
            Component.translatable("forgeautoshutdown.error.toosoon", seconds)
        );
    private static final DynamicCommandExceptionType NOT_ENOUGH_PLAYERS =
        new DynamicCommandExceptionType(required ->
            Component.translatable("forgeautoshutdown.error.notenoughplayers", required)
        );

    private static final ShutdownCommand INSTANCE = new ShutdownCommand();

    private final Map<UUID, Boolean> votes = new HashMap<>();
    private long lastVoteMillis = 0L;
    private boolean voting = false;

    /** Registers the `/shutdown` command for use */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("shutdown")
            .requires(source -> Config.voteEnabled.get())
            .executes(context -> INSTANCE.initiateVote(context.getSource()))
            .then(Commands.literal("yes")
                .executes(context -> INSTANCE.processVote(context.getSource(), true))
            )
            .then(Commands.literal("no")
                .executes(context -> INSTANCE.processVote(context.getSource(), false))
            )
        );

        ForgeAutoShutdown.LOGGER.debug("`/shutdown` command registered");
    }

    private ShutdownCommand() { }

    private int initiateVote(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = getPlayer(source);
        Logger logger = ForgeAutoShutdown.LOGGER;

        if (voting)
            throw VOTE_IN_PROGRESS.create();

        long now = System.currentTimeMillis();
        long interval = (long) Config.voteInterval.get() * 60 * 1000;
        long difference = now - lastVoteMillis;

        if (difference < interval)
            throw TOO_SOON.create((interval - difference) / 1000);

        MinecraftServer server = source.getServer();
        int players = server.getPlayerList().getPlayers().size();

        if (players < Config.minVoters.get())
            throw NOT_ENOUGH_PLAYERS.create(Config.minVoters.get());

        votes.clear();
        voting = true;

        Chat.toAll(server, "forgeautoshutdown.msg.votebegun");
        logger.info("ForgeAutoShutdown: {} called for a shutdown vote", player.getScoreboardName());
        return 1;
    }

    private int processVote(CommandSourceStack source, boolean vote) throws CommandSyntaxException
    {
        ServerPlayer player = getPlayer(source);
        Logger logger = ForgeAutoShutdown.LOGGER;

        if (!voting)
            throw NO_VOTE_IN_PROGRESS.create();

        UUID id = player.getUUID();

        if (votes.containsKey(id))
            Chat.to(source, "forgeautoshutdown.msg.votecleared");

        votes.put(id, vote);
        Chat.to(source, "forgeautoshutdown.msg.voterecorded");

        logger.info("ForgeAutoShutdown: {} voted {}", player.getScoreboardName(), vote ? "yes" : "no");
        checkVotes(source.getServer());
        return 1;
    }

    private void checkVotes(MinecraftServer server)
    {
        int players = server.getPlayerList().getPlayers().size();

        if (players < Config.minVoters.get())
        {
            voteFailure(server, "forgeautoshutdown.fail.notenoughplayers");
            return;
        }

        int yes = 0;
        int no = 0;
        for (boolean vote : votes.values())
        {
            if (vote)
                yes++;
            else
                no++;
        }

        if (no >= Config.maxNoVotes.get())
        {
            voteFailure(server, "forgeautoshutdown.fail.maxnovotes");
            return;
        }

        if (yes + no == players)
            voteSuccess(server);
    }

    private void voteSuccess(MinecraftServer server)
    {
        ForgeAutoShutdown.LOGGER.info("Server shutdown initiated by vote");
        Server.shutdown(server, Component.translatable("forgeautoshutdown.msg.usershutdown"));
    }

    private void voteFailure(MinecraftServer server, String reason)
    {
        Chat.toAll(server, reason);
        votes.clear();

        lastVoteMillis = System.currentTimeMillis();
        voting = false;
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) throws CommandSyntaxException
    {
        if (source.getEntity() instanceof ServerPlayer player)
            return player;

        throw PLAYERS_ONLY.create();
    }
}
