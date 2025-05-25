package witherchunks.witherChunks.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import witherchunks.witherChunks.WitherChunks;

public class EntityDeathListener implements Listener {

    private final WitherChunks plugin;

    public EntityDeathListener(WitherChunks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof WitherSkeleton && entity.hasMetadata(WitherChunks.METADATA_KEY)) {
            plugin.decrementSpawnedSkeletons(); // Need to create this method in WitherChunks
            plugin.persistChunkData();
        }
    }
} 