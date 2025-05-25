package witherchunks.witherChunks;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import witherchunks.witherChunks.commands.CommandManager;
import witherchunks.witherChunks.helpers.ChunkDataManager;
import witherchunks.witherChunks.helpers.SpawningService;
import witherchunks.witherChunks.listeners.ListenerManager;

import java.util.LinkedHashMap;
import java.util.Map;

public class WitherChunks extends JavaPlugin {
    
    private Map<Integer, String> witherChunks = new LinkedHashMap<>();
    private int nextId = 1;
    private int spawnedSkeletons = 0;
    private SpawningService spawningService;
    private ChunkDataManager chunkDataManager;
    private int maxWitherSkeletons;
    private ListenerManager listenerManager;

    // Constants for config.yml
    private static final String CONFIG_MAX_WITHER = "max-wither";
    private static final String CONFIG_SPAWN_INTERVAL_SEC = "spawn-interval-sec";
    private static final String CONFIG_SPAWN_CHANCE = "spawn-chance";

    // New fields for config values
    private int spawnIntervalSec;
    private double spawnChance;
    
    // Constants for metadata (can remain here or move to SpawningService if only used there)
    public static final String METADATA_KEY = "witherchunk_spawned"; 
    // MAX_SPAWNED is now loaded from config
    
    @Override
    public void onEnable() {
        // Setup config.yml
        saveDefaultConfig(); // Creates config.yml if it doesn't exist with defaults from plugin.yml
        FileConfiguration configFile = getConfig();
        configFile.addDefault(CONFIG_MAX_WITHER, 100);
        configFile.addDefault(CONFIG_SPAWN_INTERVAL_SEC, 30); // Default to 30 seconds
        configFile.addDefault(CONFIG_SPAWN_CHANCE, 1.0); // Default to 100% chance
        configFile.options().copyDefaults(true);
        saveConfig(); // Save any defaults that were added
        maxWitherSkeletons = configFile.getInt(CONFIG_MAX_WITHER);
        spawnIntervalSec = configFile.getInt(CONFIG_SPAWN_INTERVAL_SEC);
        spawnChance = configFile.getDouble(CONFIG_SPAWN_CHANCE);

        // Validate spawnChance
        if (spawnChance < 0.0) {
            spawnChance = 0.0;
            getLogger().warning(CONFIG_SPAWN_CHANCE + " was less than 0.0, corrected to 0.0");
        } else if (spawnChance > 1.0) {
            spawnChance = 1.0;
            getLogger().warning(CONFIG_SPAWN_CHANCE + " was greater than 1.0, corrected to 1.0");
        }

        // Initialize services and managers
        spawningService = new SpawningService(this);
        chunkDataManager = new ChunkDataManager(this);
        new CommandManager(this);
        listenerManager = new ListenerManager(this);
        listenerManager.registerAllListeners();
        
        // Load saved chunk data from data.yml
        loadChunkData();
        
        // Force load all saved chunks
        forceLoadSavedChunks();
        
        // Start the wither skeleton spawning task
        spawningService.startSpawnTask();
        
        // Count existing skeletons on startup
        Bukkit.getScheduler().runTaskLater(this, this::countExistingSkeletons, 100L);
        
        getLogger().info("WitherChunk plugin enabled! Max skeletons: " + maxWitherSkeletons + ". Spawned skeletons: " + spawnedSkeletons + "/" + maxWitherSkeletons);
    }
    
    @Override
    public void onDisable() {
        // Cancel the spawn task
        if (spawningService != null) {
            spawningService.stopSpawnTask();
        }
        
        // Save chunk data to data.yml
        persistChunkData();
        getLogger().info("WitherChunk plugin disabled!");
    }

    public int getMaxWitherSkeletons() {
        return maxWitherSkeletons;
    }
    
    public int getSpawnIntervalSec() {
        return spawnIntervalSec;
    }

    public double getSpawnChance() {
        return spawnChance;
    }
    
    // Count existing skeletons in wither chunks
    public void countExistingSkeletons() {
        int totalCount = 0;
        
        for (Map.Entry<Integer, String> entry : witherChunks.entrySet()) {
            String chunkKey = entry.getValue();
            String[] parts = chunkKey.split(":");
            if (parts.length == 3) {
                World world = Bukkit.getWorld(parts[0]);
                if (world != null) {
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        Chunk chunk = world.getChunkAt(x, z);
                        
                        for (Entity entity : chunk.getEntities()) {
                            if (entity instanceof WitherSkeleton) {
                                // Re-mark existing skeletons
                                entity.setMetadata(METADATA_KEY, new FixedMetadataValue(this, true));
                                ((WitherSkeleton) entity).setPersistent(true);
                                ((WitherSkeleton) entity).setRemoveWhenFarAway(false);
                                totalCount++;
                            }
                        }
                    } catch (NumberFormatException e) {
                        getLogger().warning("Invalid chunk coordinates: " + chunkKey);
                    }
                }
            }
        }
        
        spawnedSkeletons = totalCount;
        getLogger().info("Counted " + totalCount + " existing wither skeletons across " + witherChunks.size() + " chunks");
        persistChunkData();
    }
    
    public void incrementSpawnedSkeletons() {
        spawnedSkeletons++;
        // Consider if persistChunkData() should be called here immediately or batched.
        // For now, assuming SpawningService will call it after a successful counted spawn.
    }

    public void decrementSpawnedSkeletons() {
        spawnedSkeletons--;
        if (spawnedSkeletons < 0) {
            spawnedSkeletons = 0; // Safety check
        }
    }

    public SpawningService getSpawningService() {
        return spawningService;
    }

    public Map<Integer, String> getWitherChunks() {
        return witherChunks;
    }

    public int getSpawnedSkeletons() {
        return spawnedSkeletons;
    }

    public boolean isWitherChunk(String chunkKey) {
        return witherChunks.containsValue(chunkKey);
    }
    
    public Integer getChunkId(String chunkKey) {
        for (Map.Entry<Integer, String> entry : witherChunks.entrySet()) {
            if (entry.getValue().equals(chunkKey)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    public String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
    
    public int getNextId() {
        return nextId++; // Returns current nextId then increments it for the next use
    }

    // If separate increment is needed, or if getNextId should not auto-increment:
    // public int getNextIdForDisplay() { return nextId; }
    // public void incrementNextId() { this.nextId++; }

    public void loadChunkData() {
        if (chunkDataManager == null) {
            getLogger().severe("ChunkDataManager not initialized!");
            return;
        }
        ChunkDataManager.ChunkData data = chunkDataManager.loadData();
        this.witherChunks = new LinkedHashMap<>(data.witherChunks); // Ensure mutable map
        this.nextId = data.nextId;
        this.spawnedSkeletons = data.spawnedSkeletons;
        getLogger().info("Loaded " + this.witherChunks.size() + " wither chunks from data.yml");
    }
    
    public void persistChunkData() {
        if (chunkDataManager != null) {
            chunkDataManager.saveData(this.witherChunks, this.nextId, this.spawnedSkeletons);
            // getLogger().info("Saved chunk data to data.yml"); // Optional: for debugging
        }
    }
    
    private void forceLoadSavedChunks() {
        int loaded = 0;
        for (Map.Entry<Integer, String> entry : witherChunks.entrySet()) {
            String chunkKey = entry.getValue();
            String[] parts = chunkKey.split(":");
            if (parts.length == 3) {
                World world = Bukkit.getWorld(parts[0]);
                if (world != null) {
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        Chunk chunk = world.getChunkAt(x, z);
                        chunk.setForceLoaded(true);
                        loaded++;
                    } catch (NumberFormatException e) {
                        getLogger().warning("Invalid chunk coordinates in data: " + chunkKey);
                    }
                } else {
                    getLogger().warning("World not found for chunk #" + entry.getKey() + ": " + chunkKey);
                }
            }
        }
        if (loaded > 0) {
            getLogger().info("Force loaded " + loaded + " wither chunks");
        }
    }

    public void reloadPluginConfig() {
        reloadConfig(); // Bukkit method to reload config.yml from disk
        FileConfiguration configFile = getConfig();
        // Ensure defaults are still present if the user deleted the line, though reloadConfig() should handle this.
        // For safety, one might re-apply defaults if needed, but typically not required if plugin.yml has defaults.
        maxWitherSkeletons = configFile.getInt(CONFIG_MAX_WITHER, 100); // Re-read with a default
        spawnIntervalSec = configFile.getInt(CONFIG_SPAWN_INTERVAL_SEC, 30);
        spawnChance = configFile.getDouble(CONFIG_SPAWN_CHANCE, 1.0);

        // Validate spawnChance again after reload
        if (spawnChance < 0.0) {
            spawnChance = 0.0;
            getLogger().warning(CONFIG_SPAWN_CHANCE + " was less than 0.0 during reload, corrected to 0.0");
        } else if (spawnChance > 1.0) {
            spawnChance = 1.0;
            getLogger().warning(CONFIG_SPAWN_CHANCE + " was greater than 1.0 during reload, corrected to 1.0");
        }

        getLogger().info("Configuration reloaded. New max-wither: " + maxWitherSkeletons +
                ", New spawn-interval-sec: " + spawnIntervalSec +
                ", New spawn-chance: " + spawnChance);
        // The SpawningService will pick up the new maxWitherSkeletons value on its next check.
    }
}