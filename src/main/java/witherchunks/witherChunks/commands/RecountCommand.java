package witherchunks.witherChunks.commands;

import org.bukkit.command.CommandSender;
import witherchunks.witherChunks.WitherChunks;

public class RecountCommand implements SubCommand {

    private final WitherChunks plugin;

    public RecountCommand(WitherChunks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("§eRecounting existing wither skeletons...");
        plugin.countExistingSkeletons(); // Assumes countExistingSkeletons is public
        sender.sendMessage("§aRecount complete! Found " + plugin.getSpawnedSkeletons() + " wither skeletons.");
        return true;
    }
} 