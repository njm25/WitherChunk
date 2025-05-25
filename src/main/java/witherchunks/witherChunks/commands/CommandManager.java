package witherchunks.witherChunks.commands;

import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import witherchunks.witherChunks.WitherChunks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final WitherChunks plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final List<String> subCommandNames = new ArrayList<>();

    public CommandManager(WitherChunks plugin) {
        this.plugin = plugin;
        // Register subcommands
        registerSubCommand("add", new AddCommand(plugin));
        registerSubCommand("delete", new DeleteCommand(plugin));
        registerSubCommand("remove", subCommands.get("delete")); // Alias for delete
        registerSubCommand("list", new ListCommand(plugin));
        registerSubCommand("info", new InfoCommand(plugin));
        registerSubCommand("recount", new RecountCommand(plugin));
        registerSubCommand("reload", new ReloadCommand(plugin));

        // Register the main command and its alias with Bukkit
        PluginCommand mainCommand = plugin.getCommand("witherchunk");
        if (mainCommand != null) {
            mainCommand.setExecutor(this);
            mainCommand.setTabCompleter(this);
        } else {
            plugin.getLogger().severe("Command 'witherchunk' not found in plugin.yml!");
        }
    }

    private void registerSubCommand(String name, SubCommand command) {
        subCommands.put(name.toLowerCase(), command);
        if (!subCommandNames.contains(name.toLowerCase())) { // Avoid adding aliases to the main suggestion list if already present by primary name
            subCommandNames.add(name.toLowerCase());
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou must be an operator to use this command!");
            return true;
        }

        if (args.length == 0) {
            // Default behavior: if sender is player, toggle current chunk. Otherwise, show usage.
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Chunk chunk = player.getLocation().getChunk();
                String chunkKey = plugin.getChunkKey(chunk);
                Integer existingId = plugin.getChunkId(chunkKey);

                if (existingId != null) {
                    plugin.getWitherChunks().remove(existingId);
                    chunk.setForceLoaded(false);
                    player.sendMessage("§eChunk #" + existingId + " removed from wither chunks! (" + chunk.getX() + ", " + chunk.getZ() + ")");
                } else {
                    int id = plugin.getNextId();
                    plugin.getWitherChunks().put(id, chunkKey);
                    chunk.setForceLoaded(true);
                    player.sendMessage("§aChunk #" + id + " added to wither chunks! (" + chunk.getX() + ", " + chunk.getZ() + ")");
                }
                plugin.persistChunkData();
            } else {
                sendUsage(sender, label);
            }
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sendUsage(sender, label);
            return true;
        }

        return subCommand.execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.isOp()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return subCommandNames.stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .sorted() // Optional: sort for consistent order
                    .collect(Collectors.toList());
        }
        
        // Specific tab completion for subcommands (e.g., delete <id>)
        if (args.length == 2) {
            String subCommandName = args[0].toLowerCase();
            if (subCommandName.equals("delete") || subCommandName.equals("remove")) {
                // Suggest existing chunk IDs
                return plugin.getWitherChunks().keySet().stream()
                        .map(String::valueOf)
                        .filter(id -> id.startsWith(args[1]))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList(); // No further suggestions
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage("§cUsage: /" + label + " [" + String.join(" | ", subCommandNames) + "]");
        // More detailed usage can be added here or per subcommand
    }
}
