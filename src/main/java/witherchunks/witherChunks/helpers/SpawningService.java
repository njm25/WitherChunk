package witherchunks.witherChunks.helpers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;

import witherchunks.witherChunks.WitherChunks;

import java.util.ArrayList;
import java.util.List;

public class SpawningService {

    private final WitherChunks plugin;
    private BukkitTask spawnTask;

    public SpawningService(WitherChunks plugin) {
        this.plugin = plugin;
    }

    public void startSpawnTask() {
        // Convert seconds to ticks (20 ticks per second)
        long spawnIntervalTicks = plugin.getSpawnIntervalSec() * 20L;
        if (spawnIntervalTicks <= 0) {
            spawnIntervalTicks = 600L; // Default to 30 seconds if config is invalid
            plugin.getLogger().warning("spawn-interval-sec is invalid, defaulting to 30 seconds.");
        }

        spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            plugin.countExistingSkeletons(); // Recount before attempting to spawn

            if (plugin.getSpawnedSkeletons() >= plugin.getMaxWitherSkeletons() || plugin.getWitherChunks().isEmpty()) {
                return;
            }

            // Try to spawn in chunks that need more skeletons
            List<String> availableChunks = new ArrayList<>(plugin.getWitherChunks().values());

            if (!availableChunks.isEmpty()) {
                for (int attempts = 0; attempts < 3 && plugin.getSpawnedSkeletons() < plugin.getMaxWitherSkeletons(); attempts++) {
                    String randomChunk = availableChunks.get((int) (Math.random() * availableChunks.size()));
                    trySpawnInChunk(randomChunk);
                }
            }

        }, spawnIntervalTicks, spawnIntervalTicks); // Use configured interval
    }

    public void stopSpawnTask() {
        if (spawnTask != null) {
            spawnTask.cancel();
        }
    }

    private void trySpawnInChunk(String chunkKey) {
        String[] parts = chunkKey.split(":");
        if (parts.length != 3) return;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return;

        try {
            int chunkX = Integer.parseInt(parts[1]);
            int chunkZ = Integer.parseInt(parts[2]);

            // Check spawn chance first
            if (Math.random() >= plugin.getSpawnChance()) {
                return; // Spawn chance failed
            }

            // Try to find a suitable spawn location
            for (int attempt = 0; attempt < 10; attempt++) {
                int x = (chunkX * 16) + (int) (Math.random() * 16);
                int z = (chunkZ * 16) + (int) (Math.random() * 16);

                // Find the highest nether brick block in this column
                Location spawnLoc = findNetherBrickSpawnLocation(world, x, z);
                if (spawnLoc != null) {
                    if (spawnWitherSkeleton(spawnLoc, true)) {
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid chunk coordinates: " + chunkKey);
        }
    }

    private Location findNetherBrickSpawnLocation(World world, int x, int z) {
        // Search from top to bottom for nether brick blocks
        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);

            if (block.getType() == Material.NETHER_BRICKS) {
                // Check if there's enough space above for the wither skeleton (2 blocks high)
                Block above1 = world.getBlockAt(x, y + 1, z);
                Block above2 = world.getBlockAt(x, y + 2, z);

                if (above1.getType().isAir() && above2.getType().isAir()) {
                    return new Location(world, x + 0.5, y + 1, z + 0.5);
                }
            }
        }
        return null;
    }

    public boolean spawnWitherSkeleton(Location location, boolean countSpawn) {
        try {
            WitherSkeleton witherSkeleton = location.getWorld().spawn(location, WitherSkeleton.class);

            if (countSpawn) {
                // Mark this skeleton as spawned by our plugin
                witherSkeleton.setMetadata(WitherChunks.METADATA_KEY, new FixedMetadataValue(plugin, true));
                plugin.incrementSpawnedSkeletons();
            }

            // Always set these properties
            witherSkeleton.setPersistent(true);
            witherSkeleton.setRemoveWhenFarAway(false);
            // Ensure metadata is set even if not counted (e.g. for natural spawn replacements)
            witherSkeleton.setMetadata(WitherChunks.METADATA_KEY, new FixedMetadataValue(plugin, true));


            if (countSpawn) {
                // Save the updated count
                Bukkit.getScheduler().runTaskLater(plugin, plugin::persistChunkData, 1L);
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn wither skeleton at " + location + ": " + e.getMessage());
            return false;
        }
    }
} 