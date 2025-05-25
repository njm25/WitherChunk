package witherchunks.witherChunks.commands;

import org.bukkit.command.CommandSender;
import witherchunks.witherChunks.WitherChunks;

import java.util.Map;

public class ListCommand implements SubCommand {

    private final WitherChunks plugin;

    public ListCommand(WitherChunks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (plugin.getWitherChunks().isEmpty()) {
            sender.sendMessage("§eNo wither chunks are currently active.");
            return true;
        }

        sender.sendMessage("§6=== Wither Chunks ===");
        for (Map.Entry<Integer, String> entry : plugin.getWitherChunks().entrySet()) {
            String[] parts = entry.getValue().split(":");
            if (parts.length == 3) {
                sender.sendMessage("§a#" + entry.getKey() + "§7: §f" + parts[0] + " (" + parts[1] + ", " + parts[2] + ")");
            }
        }
        sender.sendMessage("§6Total: " + plugin.getWitherChunks().size() + " chunks");
        sender.sendMessage("§6Spawned Skeletons: §e" + plugin.getSpawnedSkeletons() + "§6/§e" + plugin.getMaxWitherSkeletons());
        return true;
    }
} 