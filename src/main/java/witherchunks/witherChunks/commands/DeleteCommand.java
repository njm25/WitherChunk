package witherchunks.witherChunks.commands;

import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import witherchunks.witherChunks.WitherChunks;

public class DeleteCommand implements SubCommand {

    private final WitherChunks plugin;

    public DeleteCommand(WitherChunks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /witherchunk delete <id>");
            return true;
        }

        try {
            int id = Integer.parseInt(args[1]);
            String chunkKey = plugin.getWitherChunks().get(id);

            if (chunkKey == null) {
                sender.sendMessage("§cWither chunk with ID " + id + " not found.");
                return true;
            }

            // Attempt to get chunk and unforce-load it
            String[] parts = chunkKey.split(":");
            if (parts.length == 3) {
                try {
                    Chunk chunk = plugin.getServer().getWorld(parts[0]).getChunkAt(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    if (chunk.isForceLoaded()) {
                        chunk.setForceLoaded(false);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not unforce load chunk " + chunkKey + " for ID " + id + ": " + e.getMessage());
                }
            }

            plugin.getWitherChunks().remove(id);
            sender.sendMessage("§eWither chunk #" + id + " removed!");
            plugin.persistChunkData(); // Save changes
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid ID format. Please use a number.");
            return true;
        }
    }

    @Override
    public String getDescription() {
        return "Deletes a wither chunk by ID";
    }
} 