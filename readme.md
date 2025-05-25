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

| Command                    | Alias(es)                                  | Description                                          |
| -------------------------- | ------------------------------------------ | ---------------------------------------------------- |
| `/witherchunk`             | `/wc`                                      | Toggles the player's current chunk as a wither chunk |
| `/witherchunk add`         | `/wc add`                                  | Adds the player's current chunk as a wither chunk    |
| `/witherchunk delete <id>` | `/wc delete <id>`, `/witherchunk remove <id>`, `/wc remove <id>` | Deletes a wither chunk by ID                         |
| `/witherchunk list`        | `/wc list`                                 | Lists all wither chunks and their coordinates        |
| `/witherchunk info`        | `/wc info`                                 | Shows plugin status and skeleton count               |
| `/witherchunk recount`     | `/wc recount`                              | Recounts all Wither Skeletons across tracked chunks  |
| `/witherchunk reload`      | `/wc reload`                               | Reloads the plugin configuration                     |
| `/witherchunk help`        | `/wc help`                                 | Shows description of each command                    |

---

## Configuration

The plugin's configuration is managed in `plugins/WitherChunks/config.yml`.

* `max-wither`: The maximum number of Wither Skeletons allowed to spawn across all wither chunks (default: 100). This can be adjusted to control server performance and gameplay balance.
* `spawn-interval-sec`: The interval in seconds at which the plugin attempts to spawn Wither Skeletons (default: 30). 
* `spawn-chance`: The chance (from 0.0 to 1.0) that a Wither Skeleton will spawn during a spawn attempt (default: 1.0, meaning 100% chance).

---

## Requirements

* Minecraft server (Bukkit/Spigot)
* Java 17 or higher


