package witherchunks.witherChunks.commands;

import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import witherchunks.witherChunks.WitherChunks;

public class AddCommand implements SubCommand {

    private final WitherChunks plugin;

    public AddCommand(WitherChunks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can add chunks!");
            return true;
        }

        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = plugin.getChunkKey(chunk); // Assumes getChunkKey is public in WitherChunks

        Integer existingId = plugin.getChunkId(chunkKey);
        if (existingId != null) {
            player.sendMessage("§eThis chunk is already a wither chunk (#" + existingId + ")!");
            return true;
        }

        int id = plugin.getNextId(); // Assumes getNextId() and incrementNextId() exist
        plugin.getWitherChunks().put(id, chunkKey);
        chunk.setForceLoaded(true);
        player.sendMessage("§aChunk #" + id + " added to wither chunks! (" + chunk.getX() + ", " + chunk.getZ() + ")");

        plugin.persistChunkData();
        return true;
    }
} 