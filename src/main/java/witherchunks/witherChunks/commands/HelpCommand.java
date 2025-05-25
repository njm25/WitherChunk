package witherchunks.witherChunks.commands;

import org.bukkit.command.CommandSender;
import witherchunks.witherChunks.WitherChunks;

import java.util.Map;

public class HelpCommand implements SubCommand {

    private final WitherChunks plugin;
    private final Map<String, SubCommand> subCommands;

    public HelpCommand(WitherChunks plugin, Map<String, SubCommand> subCommands) {
        this.plugin = plugin;
        this.subCommands = subCommands;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6--- WitherChunks Help ---");
        // Iterate over unique command instances, not aliases
        subCommands.entrySet().stream()
            .filter(entry -> !entry.getKey().equals("remove")) // Exclude alias
            .filter(entry -> !entry.getKey().equals("wc")) // Exclude alias
            .forEach(entry -> {
                String commandName = entry.getKey();
                SubCommand subCommand = entry.getValue();
                String description = subCommand.getDescription();
                if (description == null || description.trim().isEmpty()) {
                    description = "No description available.";
                }
                sender.sendMessage("§a/" + plugin.getCommand("witherchunk").getLabel() + " " + commandName + "§7 - " + description);
            });
        return true;
    }

    @Override
    public String getDescription() {
        return "Shows this help message.";
    }
} 