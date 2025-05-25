package witherchunks.witherChunks.commands;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
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
                sender.sendMessage("§cNo wither chunk found with ID #" + id);
                return true;
            }

            plugin.getWitherChunks().remove(id);

            String[] parts = chunkKey.split(":");
            if (parts.length == 3) {
                World world = Bukkit.getWorld(parts[0]);
                if (world != null) {
                    int x = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    Chunk chunkToUnload = world.getChunkAt(x, z);
                    chunkToUnload.setForceLoaded(false);
                }
            }

            sender.sendMessage("§eWither chunk #" + id + " deleted!");
            plugin.persistChunkData();

        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid ID! Please enter a number.");
        }
        return true;
    }
} 