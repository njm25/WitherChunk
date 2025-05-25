package witherchunks.witherChunks.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import witherchunks.witherChunks.WitherChunks;

public class ChunkLoadListener implements Listener {

    private final WitherChunks plugin;

    public ChunkLoadListener(WitherChunks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        String chunkKey = plugin.getChunkKey(event.getChunk());
        if (plugin.isWitherChunk(chunkKey)) {
            // Recount skeletons when chunk loads (this might be intensive, consider if necessary)
            // Or simply re-apply metadata if that's the main goal
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Entity entity : event.getChunk().getEntities()) {
                    if (entity instanceof WitherSkeleton) {
                        entity.setMetadata(WitherChunks.METADATA_KEY, new FixedMetadataValue(plugin, true));
                        ((WitherSkeleton) entity).setPersistent(true);
                        ((WitherSkeleton) entity).setRemoveWhenFarAway(false);
                    }
                }
            }, 20L);
        }
    }
} 