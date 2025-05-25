package witherchunks.witherChunks.commands;

import org.bukkit.command.CommandSender;
import witherchunks.witherChunks.WitherChunks;

public class ReloadCommand implements SubCommand {

    private final WitherChunks plugin;

    public ReloadCommand(WitherChunks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.reloadPluginConfig();
        sender.sendMessage("§aWitherChunks configuration reloaded!");
        return true;
    }
} 