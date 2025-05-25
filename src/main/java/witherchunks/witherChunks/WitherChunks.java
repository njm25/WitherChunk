package witherchunks.witherChunks;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WitherChunks extends JavaPlugin implements Listener {
    
    private Map<Integer, String> witherChunks = new LinkedHashMap<>();
    private int nextId = 1;
    private int spawnedSkeletons = 0;
    private BukkitTask spawnTask;
    
    private static final String CONFIG_KEY = "wither-chunks";
    private static final String CONFIG_NEXT_ID = "next-id";
    private static final String CONFIG_SPAWNED_COUNT = "spawned-skeletons";
    private static final String CONFIG_CHUNK_COUNTS = "chunk-skeleton-counts";
    private static final String METADATA_KEY = "witherchunk_spawned";
    private static final int MAX_SPAWNED = 100;
    
    @Override
    public void onEnable() {
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        
        // Load saved chunks from config
        loadWitherChunks();
        
        // Force load all saved chunks
        forceLoadSavedChunks();
        
        // Start the wither skeleton spawning task (runs every 30 seconds)
        startSpawnTask();
        
        // Count existing skeletons on startup
        Bukkit.getScheduler().runTaskLater(this, this::countExistingSkeletons, 100L);
        
        getLogger().info("WitherChunk plugin enabled! Spawned skeletons: " + spawnedSkeletons + "/" + MAX_SPAWNED);
    }
    
    @Override
    public void onDisable() {
        // Cancel the spawn task
        if (spawnTask != null) {
            spawnTask.cancel();
        }
        
        // Save chunks to config
        saveWitherChunks();
        getLogger().info("WitherChunk plugin disabled!");
    }
    
    // Count existing skeletons in wither chunks
    private void countExistingSkeletons() {
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
        saveWitherChunks();
    }
    
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        String chunkKey = getChunkKey(event.getChunk());
        if (isWitherChunk(chunkKey)) {
            // Recount skeletons when chunk loads
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (Entity entity : event.getChunk().getEntities()) {
                    if (entity instanceof WitherSkeleton) {
                        entity.setMetadata(METADATA_KEY, new FixedMetadataValue(this, true));
                        ((WitherSkeleton) entity).setPersistent(true);
                        ((WitherSkeleton) entity).setRemoveWhenFarAway(false);
                    }
                }
                
            }, 20L); // Wait 1 second for chunk to fully load
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("witherchunk")) {
            return false;
        }
        
        // Check if sender is an operator
        if (!sender.isOp()) {
            sender.sendMessage("§cYou must be an operator to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            // Default behavior - toggle current chunk (player only)
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command requires a player or subcommand!");
                return true;
            }
            
            Player player = (Player) sender;
            Chunk chunk = player.getLocation().getChunk();
            String chunkKey = getChunkKey(chunk);
            
            // Check if chunk already exists
            Integer existingId = getChunkId(chunkKey);
            if (existingId != null) {
                // Remove from wither chunks
                witherChunks.remove(existingId);
                chunk.setForceLoaded(false);
                player.sendMessage("§eChunk #" + existingId + " removed from wither chunks! (" + chunk.getX() + ", " + chunk.getZ() + ")");
            } else {
                // Add to wither chunks
                int id = nextId++;
                witherChunks.put(id, chunkKey);
                chunk.setForceLoaded(true);
                player.sendMessage("§aChunk #" + id + " added to wither chunks! (" + chunk.getX() + ", " + chunk.getZ() + ")");
            }
            
            saveWitherChunks();
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "add":
                return handleAddCommand(sender, args);
            case "delete":
            case "remove":
                return handleDeleteCommand(sender, args);
            case "list":
                return handleListCommand(sender, args);
            case "info":
                return handleInfoCommand(sender, args);
            case "recount":
                return handleRecountCommand(sender, args);
            default:
                sender.sendMessage("§cUsage: /witherchunk [add|delete|list|info|recount]");
                return true;
        }
    }
    
    private boolean handleAddCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can add chunks!");
            return true;
        }
        
        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();
        String chunkKey = getChunkKey(chunk);
        
        // Check if chunk already exists
        Integer existingId = getChunkId(chunkKey);
        if (existingId != null) {
            player.sendMessage("§eThis chunk is already a wither chunk (#" + existingId + ")!");
            return true;
        }
        
        // Add to wither chunks
        int id = nextId++;
        witherChunks.put(id, chunkKey);
        chunk.setForceLoaded(true);
        player.sendMessage("§aChunk #" + id + " added to wither chunks! (" + chunk.getX() + ", " + chunk.getZ() + ")");
        
        saveWitherChunks();
        return true;
    }
    
    private boolean handleDeleteCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /witherchunk delete <id>");
            return true;
        }
        
        try {
            int id = Integer.parseInt(args[1]);
            String chunkKey = witherChunks.get(id);
            
            if (chunkKey == null) {
                sender.sendMessage("§cNo wither chunk found with ID #" + id);
                return true;
            }
            
            // Remove from maps
            witherChunks.remove(id);
            
            // Unload the chunk
            String[] parts = chunkKey.split(":");
            if (parts.length == 3) {
                World world = Bukkit.getWorld(parts[0]);
                if (world != null) {
                    int x = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    Chunk chunk = world.getChunkAt(x, z);
                    chunk.setForceLoaded(false);
                }
            }
            
            sender.sendMessage("§eWither chunk #" + id + " deleted!");
            saveWitherChunks();
            
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid ID! Please enter a number.");
        }
        
        return true;
    }
    
    private boolean handleListCommand(CommandSender sender, String[] args) {
        if (witherChunks.isEmpty()) {
            sender.sendMessage("§eNo wither chunks are currently active.");
            return true;
        }
        
        sender.sendMessage("§6=== Wither Chunks ===");
        for (Map.Entry<Integer, String> entry : witherChunks.entrySet()) {
            String[] parts = entry.getValue().split(":");
            if (parts.length == 3) {
                sender.sendMessage("§a#" + entry.getKey() + "§7: §f" + parts[0] + " (" + parts[1] + ", " + parts[2] + ")");
            }
        }
        sender.sendMessage("§6Total: " + witherChunks.size() + " chunks");
        sender.sendMessage("§6Spawned Skeletons: §e" + spawnedSkeletons + "§6/§e" + MAX_SPAWNED);
        return true;
    }
    
    private boolean handleInfoCommand(CommandSender sender, String[] args) {
        sender.sendMessage("§6=== WitherChunk Info ===");
        sender.sendMessage("§aActive Chunks: §f" + witherChunks.size());
        sender.sendMessage("§aSpawned Skeletons: §f" + spawnedSkeletons + "/" + MAX_SPAWNED);
        sender.sendMessage("§aSpawn Status: §f" + (spawnedSkeletons >= MAX_SPAWNED ? "§cMax Reached" : "§aActive"));
        return true;
    }
    
    private boolean handleRecountCommand(CommandSender sender, String[] args) {
        sender.sendMessage("§eRecounting existing wither skeletons...");
        countExistingSkeletons();
        sender.sendMessage("§aRecount complete! Found " + spawnedSkeletons + " wither skeletons.");
        return true;
    }
    
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Location loc = event.getLocation();
        Chunk chunk = loc.getChunk();
        String chunkKey = getChunkKey(chunk);
        
        // Check if this chunk is a wither chunk
        if (!isWitherChunk(chunkKey)) {
            return;
        }
        
        // Only allow wither skeletons to spawn in wither chunks
        if (event.getEntityType() != EntityType.WITHER_SKELETON) {
            // Cancel spawn of non-wither skeleton mobs
            if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL ||
                event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
                event.setCancelled(true);
                
                // Try to spawn a wither skeleton instead (increased mob cap effect)
                if (Math.random() < 0.3) { // 30% chance to spawn additional wither skeleton
                    spawnWitherSkeleton(loc, false); // Don't count natural replacements
                }
            }
        } else {
            // Make naturally spawned wither skeletons persistent
            if (event.getEntity() instanceof WitherSkeleton) {
                WitherSkeleton skeleton = (WitherSkeleton) event.getEntity();
                skeleton.setPersistent(true);
                skeleton.setRemoveWhenFarAway(false);
                skeleton.setMetadata(METADATA_KEY, new FixedMetadataValue(this, true));
            }
            
            // Increase spawn rate of wither skeletons in wither chunks
            if (Math.random() < 0.25) { // 25% chance to spawn additional wither skeleton
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    spawnWitherSkeleton(loc, false); // Don't count natural spawns
                }, 1L);
            }
        }
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        
        // Check if this is a wither skeleton spawned by our plugin
        if (entity instanceof WitherSkeleton && entity.hasMetadata(METADATA_KEY)) {
            
            spawnedSkeletons--;
            if (spawnedSkeletons < 0) spawnedSkeletons = 0; // Safety check
            
            // Save the updated count
            saveWitherChunks();
        }
    }
    
    private void startSpawnTask() {
        spawnTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (spawnedSkeletons >= MAX_SPAWNED || witherChunks.isEmpty()) {
                return;
            }
            
            // Try to spawn in chunks that need more skeletons
            List<String> availableChunks = new ArrayList<>();
            for (Map.Entry<Integer, String> entry : witherChunks.entrySet()) {
                String chunkKey = entry.getValue();
                availableChunks.add(chunkKey);
            }
            
            if (!availableChunks.isEmpty()) {
                for (int attempts = 0; attempts < 3 && spawnedSkeletons < MAX_SPAWNED; attempts++) {
                    String randomChunk = availableChunks.get((int) (Math.random() * availableChunks.size()));
                    trySpawnInChunk(randomChunk);
                }
            }
            
        }, 600L, 600L); // Run every 30 seconds (600 ticks)
    }
    
    private void trySpawnInChunk(String chunkKey) {
        String[] parts = chunkKey.split(":");
        if (parts.length != 3) return;
        
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return;
        
        try {
            int chunkX = Integer.parseInt(parts[1]);
            int chunkZ = Integer.parseInt(parts[2]);
            
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
            getLogger().warning("Invalid chunk coordinates: " + chunkKey);
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
    
    private boolean spawnWitherSkeleton(Location location, boolean countSpawn) {
        try {
            WitherSkeleton witherSkeleton = location.getWorld().spawn(location, WitherSkeleton.class);
            
            if (countSpawn) {
                // Mark this skeleton as spawned by our plugin
                witherSkeleton.setMetadata(METADATA_KEY, new FixedMetadataValue(this, true));
                spawnedSkeletons++;
            }
            
            // Always set these properties
            witherSkeleton.setPersistent(true);
            witherSkeleton.setRemoveWhenFarAway(false);
            witherSkeleton.setMetadata(METADATA_KEY, new FixedMetadataValue(this, true));
            
            if (countSpawn) {
                // Save the updated count
                Bukkit.getScheduler().runTaskLater(this, this::saveWitherChunks, 1L);
            }
            
            return true;
        } catch (Exception e) {
            getLogger().warning("Failed to spawn wither skeleton at " + location + ": " + e.getMessage());
            return false;
        }
    }
    
    private boolean isWitherChunk(String chunkKey) {
        return witherChunks.containsValue(chunkKey);
    }
    
    private Integer getChunkId(String chunkKey) {
        for (Map.Entry<Integer, String> entry : witherChunks.entrySet()) {
            if (entry.getValue().equals(chunkKey)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
    
    private void loadWitherChunks() {
        FileConfiguration config = getConfig();
        
        // Load the chunks map
        if (config.contains(CONFIG_KEY)) {
            for (String key : config.getConfigurationSection(CONFIG_KEY).getKeys(false)) {
                try {
                    int id = Integer.parseInt(key);
                    String chunkKey = config.getString(CONFIG_KEY + "." + key);
                    if (chunkKey != null) {
                        witherChunks.put(id, chunkKey);
                    }
                } catch (NumberFormatException e) {
                    getLogger().warning("Invalid chunk ID in config: " + key);
                }
            }
        }
        
        // Load next ID
        nextId = config.getInt(CONFIG_NEXT_ID, 1);
        
        // Load spawned skeleton count
        spawnedSkeletons = config.getInt(CONFIG_SPAWNED_COUNT, 0);
        
        // Ensure nextId is higher than any existing ID
        if (!witherChunks.isEmpty()) {
            int maxId = witherChunks.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
            if (nextId <= maxId) {
                nextId = maxId + 1;
            }
        }
        
        getLogger().info("Loaded " + witherChunks.size() + " wither chunks from config");
    }
    
    private void saveWitherChunks() {
        FileConfiguration config = getConfig();
        
        // Clear existing chunks
        config.set(CONFIG_KEY, null);
        config.set(CONFIG_CHUNK_COUNTS, null);
        
        // Save chunks map
        for (Map.Entry<Integer, String> entry : witherChunks.entrySet()) {
            config.set(CONFIG_KEY + "." + entry.getKey(), entry.getValue());
        }
        
        // Save next ID and spawned count
        config.set(CONFIG_NEXT_ID, nextId);
        config.set(CONFIG_SPAWNED_COUNT, spawnedSkeletons);
        
        saveConfig();
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
                        getLogger().warning("Invalid chunk coordinates in config: " + chunkKey);
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
}