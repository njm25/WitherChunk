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
            sender.sendMessage("§cThis command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = plugin.getChunkKey(chunk);

        // Check if the chunk is already a wither chunk
        if (plugin.getChunkId(chunkKey) != null) {
            player.sendMessage("§cThis chunk is already a wither chunk (ID: " + plugin.getChunkId(chunkKey) + ").");
            return true;
        }

        int id = plugin.getNextId();
        plugin.getWitherChunks().put(id, chunkKey);
        chunk.setForceLoaded(true);
        player.sendMessage("§aChunk #" + id + " added to wither chunks! (" + chunk.getX() + ", " + chunk.getZ() + ")");
        plugin.persistChunkData(); // Save changes
        return true;
    }

    @Override
    public String getDescription() {
        return "Adds the player's current chunk as a wither chunk";
    }
} 