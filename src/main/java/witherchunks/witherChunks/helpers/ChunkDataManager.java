package witherchunks.witherChunks.helpers;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import witherchunks.witherChunks.WitherChunks;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class ChunkDataManager {

    private final WitherChunks plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    private static final String DATA_FILE_NAME = "data.yml";
    private static final String CHUNKS_KEY = "wither-chunks";
    private static final String NEXT_ID_KEY = "next-id";
    private static final String SPAWNED_SKELETONS_KEY = "spawned-skeletons";
    // private static final String CHUNK_SKELETON_COUNTS_KEY = "chunk-skeleton-counts"; // This seemed unused based on previous save logic

    public static class ChunkData {
        public final Map<Integer, String> witherChunks;
        public final int nextId;
        public final int spawnedSkeletons;

        public ChunkData(Map<Integer, String> witherChunks, int nextId, int spawnedSkeletons) {
            this.witherChunks = witherChunks != null ? new LinkedHashMap<>(witherChunks) : new LinkedHashMap<>();
            this.nextId = nextId;
            this.spawnedSkeletons = spawnedSkeletons;
        }
    }

    public ChunkDataManager(WitherChunks plugin) {
        this.plugin = plugin;
        setupDataFile();
    }

    private void setupDataFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs(); // Ensure plugin's data folder exists
        }
        dataFile = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile(); // Create an empty file if it doesn't exist
                plugin.getLogger().info("Created new empty " + DATA_FILE_NAME);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create " + DATA_FILE_NAME, e);
                // If file creation fails, dataConfig will likely fail to load or be null
            }
        }
        
        dataConfig = new YamlConfiguration();
        // It's important to try loading even if createNewFile failed, 
        // as YamlConfiguration might still work or fail more gracefully for subsequent calls.
        // However, subsequent saves might fail if the file path is problematic.
        try {
            dataConfig.load(dataFile); 
        } catch (IOException | InvalidConfigurationException e) {
            // Log error if loading fails. dataConfig might be empty/null which should be handled by getDataConfig if necessary.
            plugin.getLogger().log(Level.SEVERE, "Could not load " + DATA_FILE_NAME + ". Data might be lost or defaults used.", e);
        }
    }

    public FileConfiguration getDataConfig() {
        // If dataConfig is null due to a severe loading issue, 
        // this could re-attempt setup, or callers should be prepared for null.
        // For simplicity, we assume setupDataFile initializes it, even if loading fails (it would be an empty config).
        if (dataConfig == null) {
             plugin.getLogger().warning(DATA_FILE_NAME + " was not loaded properly. Re-attempting setup.");
            // This might be risky if the initial problem was due to permissions or disk issues.
            // Consider if re-attempting is wise or if it should just return a new empty YamlConfiguration.
            setupDataFile(); 
            if (dataConfig == null) { // If still null after re-attempt
                plugin.getLogger().severe("Failed to initialize " + DATA_FILE_NAME + " even after re-attempt. Returning new empty config.");
                return new YamlConfiguration(); // Fallback to an in-memory empty config
            }
        }
        return dataConfig;
    }

    public void saveData(Map<Integer, String> witherChunks, int nextId, int spawnedSkeletons) {
        FileConfiguration configToSave = getDataConfig();
        if (configToSave == null) { // Should ideally not happen if getDataConfig has fallbacks
            plugin.getLogger().severe("Cannot save data: data config is null for " + DATA_FILE_NAME);
            return;
        }

        configToSave.set(CHUNKS_KEY, null); 
        if (witherChunks != null) {
            for (Map.Entry<Integer, String> entry : witherChunks.entrySet()) {
                configToSave.set(CHUNKS_KEY + "." + entry.getKey(), entry.getValue());
            }
        }

        configToSave.set(NEXT_ID_KEY, nextId);
        configToSave.set(SPAWNED_SKELETONS_KEY, spawnedSkeletons);
        // config.set(CHUNK_SKELETON_COUNTS_KEY, null); // Clear this as well if it was used

        try {
            configToSave.save(dataFile); // dataFile should be non-null if constructor succeeded
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data to " + DATA_FILE_NAME, e);
        } catch (NullPointerException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data to " + DATA_FILE_NAME + " because dataFile is null. This indicates a problem in setupDataFile or constructor.", e);
        }
    }

    public ChunkData loadData() {
        FileConfiguration configToLoad = getDataConfig();
        Map<Integer, String> loadedChunks = new LinkedHashMap<>();
        
        if (configToLoad == null) { // Should ideally not happen
            plugin.getLogger().severe("Cannot load data: data config is null for " + DATA_FILE_NAME + ". Returning empty data.");
            return new ChunkData(loadedChunks, 1, 0); 
        }

        if (configToLoad.isConfigurationSection(CHUNKS_KEY)) {
            for (String key : configToLoad.getConfigurationSection(CHUNKS_KEY).getKeys(false)) {
                try {
                    int id = Integer.parseInt(key);
                    String chunkKey = configToLoad.getString(CHUNKS_KEY + "." + key);
                    if (chunkKey != null) {
                        loadedChunks.put(id, chunkKey);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid chunk ID in " + DATA_FILE_NAME + ": " + key);
                }
            }
        }

        int nextId = configToLoad.getInt(NEXT_ID_KEY, 1);
        int spawnedSkeletons = configToLoad.getInt(SPAWNED_SKELETONS_KEY, 0);
        
        // Ensure nextId is higher than any existing ID
        if (!loadedChunks.isEmpty()) {
            int maxId = loadedChunks.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
            if (nextId <= maxId) {
                nextId = maxId + 1;
            }
        }

        return new ChunkData(loadedChunks, nextId, spawnedSkeletons);
    }
    
    public void forceLoadSavedChunks(Map<Integer, String> witherChunksMap) {
        if (witherChunksMap == null || witherChunksMap.isEmpty()) {
            return;
        }
        int loaded = 0;
        for (Map.Entry<Integer, String> entry : witherChunksMap.entrySet()) {
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
                        plugin.getLogger().warning("Invalid chunk coordinates in data for force loading: " + chunkKey);
                    }
                } else {
                    plugin.getLogger().warning("World not found for chunk #" + entry.getKey() + " during force load: " + chunkKey);
                }
            }
        }
        if (loaded > 0) {
            plugin.getLogger().info("Force loaded " + loaded + " wither chunks by ChunkDataManager.");
        }
    }
} 