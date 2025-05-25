package witherchunks.witherChunks.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import witherchunks.witherChunks.WitherChunks;

public class CreatureSpawnListener implements Listener {

    private final WitherChunks plugin;

    public CreatureSpawnListener(WitherChunks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Location loc = event.getLocation();
        Chunk chunk = loc.getChunk();
        String chunkKey = plugin.getChunkKey(chunk);

        if (!plugin.isWitherChunk(chunkKey)) {
            return;
        }

        if (event.getEntityType() != EntityType.WITHER_SKELETON) {
            if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL ||
                    event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
                event.setCancelled(true);
                if (Math.random() < 0.3) {
                    plugin.getSpawningService().spawnWitherSkeleton(loc, false);
                }
            }
        } else {
            // Check if the spawn limit has been reached for natural/spawner spawns
            if (plugin.getSpawnedSkeletons() >= plugin.getMaxWitherSkeletons() &&
                (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL ||
                 event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER)) {
                event.setCancelled(true);
                return; // Do not proceed with this spawn or additional spawns
            }

            if (event.getEntity() instanceof WitherSkeleton) {
                WitherSkeleton skeleton = (WitherSkeleton) event.getEntity();
                skeleton.setPersistent(true);
                skeleton.setRemoveWhenFarAway(false);
                skeleton.setMetadata(WitherChunks.METADATA_KEY, new FixedMetadataValue(plugin, true));
            }
            if (Math.random() < 0.25) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getSpawningService().spawnWitherSkeleton(loc, false);
                }, 1L);
            }
        }
    }
} 