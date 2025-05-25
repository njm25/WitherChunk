package witherchunks.witherChunks.commands;

import org.bukkit.command.CommandSender;
import witherchunks.witherChunks.WitherChunks;

public class InfoCommand implements SubCommand {

    private final WitherChunks plugin;

    public InfoCommand(WitherChunks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6=== WitherChunk Info ===");
        sender.sendMessage("§aActive Chunks: §f" + plugin.getWitherChunks().size());
        sender.sendMessage("§aSpawned Skeletons: §f" + plugin.getSpawnedSkeletons() + "/" + plugin.getMaxWitherSkeletons());
        return true;
    }
} 