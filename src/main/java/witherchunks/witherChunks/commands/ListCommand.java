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
        Map<Integer, String> witherChunks = plugin.getWitherChunks();
        if (witherChunks.isEmpty()) {
            sender.sendMessage("§eNo wither chunks are currently defined.");
            return true;
        }

        sender.sendMessage("§6--- Wither Chunks List ---");
        for (Map.Entry<Integer, String> entry : witherChunks.entrySet()) {
            String[] parts = entry.getValue().split(":");
            String worldName = parts.length > 0 ? parts[0] : "UnknownWorld";
            String chunkX = parts.length > 1 ? parts[1] : "?";
            String chunkZ = parts.length > 2 ? parts[2] : "?";
            sender.sendMessage("§aID: " + entry.getKey() + " §7- §e" + worldName + " (" + chunkX + ", " + chunkZ + ")");
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "Lists all wither chunks and their coordinates";
    }
} 