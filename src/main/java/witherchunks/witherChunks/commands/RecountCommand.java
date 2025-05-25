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
        plugin.countExistingSkeletons(); // Assuming this method exists and handles the logic
        sender.sendMessage("§aWither Skeletons recounted across all wither chunks.");
        sender.sendMessage("§eCurrent count: " + plugin.getSpawnedSkeletons()); // Display the new count
        return true;
    }

    @Override
    public String getDescription() {
        return "Recounts all Wither Skeletons across tracked chunks";
    }
} 