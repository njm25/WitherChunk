package witherchunks.witherChunks.listeners;

import org.bukkit.plugin.PluginManager;
import witherchunks.witherChunks.WitherChunks;

public class ListenerManager {

    private final WitherChunks plugin;

    public ListenerManager(WitherChunks plugin) {
        this.plugin = plugin;
    }

    public void registerAllListeners() {
        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(new ChunkLoadListener(plugin), plugin);
        pm.registerEvents(new CreatureSpawnListener(plugin), plugin);
        pm.registerEvents(new EntityDeathListener(plugin), plugin);
        
        plugin.getLogger().info("Registered custom event listeners.");
    }
} 