package com.targren.forgeautoshutdown;

import com.targren.forgeautoshutdown.util.Chat;
import com.targren.forgeautoshutdown.util.Server;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Singleton that handles the `/shutdown` voting command
 */
public class ShutdownCommand extends CommandBase
{
    static final List<String> ALIASES = Collections.singletonList("shutdown");
    static final List<String> OPTIONS = Arrays.asList("yes", "no");

    final static int RequiredPermissionLevel = 0;
    /*
        0 - All players
        1 - Can bypass spawn protection
        2 - Functions, command blocks
        3 - More commands (MP management)
        4 - Even more commands (Cheats Enabled, OP, Console)
     */
    private static ShutdownCommand INSTANCE;
    private static MinecraftServer SERVER;
    private static Logger          LOGGER;

    HashMap<String, Boolean> votes = new HashMap<>();

    Date    lastVote = new Date(0);
    boolean voting   = false;

    /** Creates and registers the `/shutdown` command for use */
    public static void create(FMLServerStartingEvent event)
    {
        if (INSTANCE != null)
            throw new RuntimeException("ShutdownCommand can only be created once");

        INSTANCE = new ShutdownCommand();
        SERVER   = ForgeAutoShutdown.server;
        LOGGER   = ForgeAutoShutdown.LOGGER;

        event.registerServerCommand(INSTANCE);
        LOGGER.debug("`/shutdown` command registered");
    }

    @Override
    public int getRequiredPermissionLevel(){
        return RequiredPermissionLevel;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (sender == SERVER || !(sender instanceof EntityPlayerMP)) {
            throw new CommandException("forgeautoshutdown.error.playersonly");
        }

        if (voting) {
            LOGGER.info("ForgeAutoShutdown: " + sender.getDisplayName().getUnformattedText() + " voted " + args[0]);
            processVote(sender, args);
        } else {
            LOGGER.info("ForgeAutoShutdown: " + sender.getDisplayName().getUnformattedText() + " called for a shutdown vote ");
            initiateVote(args);
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return Config.voteEnabled;
    }

    private ShutdownCommand() { }

    private void initiateVote(String[] args) throws CommandException
    {

        if (args.length >= 1) {
            throw new CommandException("forgeautoshutdown.error.novoteinprogress");
        }

        Date now        = new Date();
        long interval   = (long)Config.voteInterval * 60 * 1000;
        long difference = now.getTime() - lastVote.getTime();

        if (difference < interval) {
            throw new CommandException("forgeautoshutdown.error.toosoon", (interval - difference) / 1000);
        }

        List<EntityPlayerMP> players = SERVER.getPlayerList().getPlayers();

        if (players.size() < Config.minVoters) {
            throw new CommandException("forgeautoshutdown.error.notenoughplayers", Config.minVoters);
        }

        Chat.toAll(SERVER, "forgeautoshutdown.msg.votebegun");
        voting = true;
    }

    private void processVote(ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 1)
            throw new CommandException("forgeautoshutdown.error.voteinprogress");
        else if ( !OPTIONS.contains( args[0].toLowerCase() ) )
            throw new CommandException("forgeautoshutdown.error.votebadsyntax");

        String  name = sender.getName();
        Boolean vote = args[0].equalsIgnoreCase("yes");

        if ( votes.containsKey(name) )
            Chat.to(sender, "forgeautoshutdown.msg.votecleared");

        votes.put(name, vote);
        Chat.to(sender, "forgeautoshutdown.msg.voterecorded");
        checkVotes();
    }

    private void checkVotes()
    {
        int players = SERVER.getPlayerList().getPlayers().size();

        if (players < Config.minVoters)
        {
            voteFailure("forgeautoshutdown.fail.notenoughplayers");
            return;
        }

        int yes = Collections.frequency(votes.values(), true);
        int no  = Collections.frequency(votes.values(), false);

        if (no >= Config.maxNoVotes)
        {
            voteFailure("forgeautoshutdown.fail.maxnovotes");
            return;
        }

        if (yes + no == players)
            voteSuccess();
    }

    private void voteSuccess()
    {
        LOGGER.info("Server shutdown initiated by vote");
        Server.shutdown("forgeautoshutdown.msg.usershutdown");
    }

    private void voteFailure(String reason)
    {
        Chat.toAll(SERVER, reason);
        votes.clear();

        lastVote = new Date();
        voting   = false;
    }

    // <editor-fold desc="ICommand">
    @Override
    public String getName()
    {
        return "shutdown";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/shutdown <yes|no>";
    }

    @Override
    public List<String> getAliases()
    {
        return ALIASES;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int idx)
    {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return o.getName().compareTo( getName() );
    }
    // </editor-fold>

    /**
     * Get a list of options for when the user presses the TAB key
     */
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos){
       return OPTIONS;
    }


}
