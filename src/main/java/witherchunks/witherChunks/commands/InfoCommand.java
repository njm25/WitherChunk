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
        int totalChunks = plugin.getWitherChunks().size();
        long totalSkeletons = plugin.getSpawnedSkeletons(); // Assuming you have a way to get this count

        sender.sendMessage("§6--- WitherChunks Info ---");
        sender.sendMessage("§eTotal Wither Chunks: §f" + totalChunks);
        sender.sendMessage("§eTotal Wither Skeletons: §f" + totalSkeletons);
        sender.sendMessage("§eMax Wither Skeletons (Config): §f" + plugin.getConfig().getInt("max-wither", 100));
        sender.sendMessage("§eSpawn Interval (sec): §f" + plugin.getConfig().getInt("spawn-interval-sec", 30));
        sender.sendMessage("§eSpawn Chance: §f" + plugin.getConfig().getDouble("spawn-chance", 1.0));
        // Add more info as needed
        return true;
    }

    @Override
    public String getDescription() {
        return "Shows plugin status and skeleton count";
    }
} 