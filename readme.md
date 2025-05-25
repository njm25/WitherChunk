# WitherChunks

**WitherChunks** is a Minecraft plugin for Bukkit/Spigot servers that allows server operators to define specific chunks where Wither Skeletons will spawn persistently. It force-loads selected chunks, controls mob spawning behavior, and automatically manages Wither Skeleton population.

---

## Features

- Mark any chunk as a *wither chunk*.
- Only Wither Skeletons spawn in those chunks; other mobs are blocked.
- Wither Skeletons are persistent and never despawn.
- Supports:
  - Automatic chunk force-loading
  - Spawn cap enforcement (default: 100)
  - Admin commands for managing chunks and skeletons
- Automatically saves state across restarts

---

## Installation

### 1. Clone the repository

```bash
git clone https://github.com/njm25/WitherChunk.git
cd WitherChunks
````

### 2. Build the plugin

```bash
./gradlew clean build
```

The plugin JAR will be located in:

```
build/libs/WitherChunks-1.0.jar
```

### 3. Install on your Paper server

* Move the JAR file to your server's `plugins/` folder
* Start or reload your server

---

## Commands

All commands require operator permissions.

| Command                    | Description                                          |
| -------------------------- | ---------------------------------------------------- |
| `/witherchunk`             | Toggles the player's current chunk as a wither chunk |
| `/witherchunk add`         | Adds the player's current chunk as a wither chunk    |
| `/witherchunk delete <id>` | Deletes a wither chunk by ID                         |
| `/witherchunk list`        | Lists all wither chunks and their coordinates        |
| `/witherchunk info`        | Shows plugin status and skeleton count               |
| `/witherchunk recount`     | Recounts all Wither Skeletons across tracked chunks  |

---

## Configuration

The plugin's configuration is managed in `plugins/WitherChunks/config.yml`.

* `max-wither`: The maximum number of Wither Skeletons allowed to spawn across all wither chunks (default: 100). This can be adjusted to control server performance and gameplay balance.

---

## Requirements

* Minecraft server (Bukkit/Spigot)
* Java 17 or higher


