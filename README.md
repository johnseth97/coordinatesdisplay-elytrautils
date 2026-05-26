# devflags-fabric

![Brain and pathfinding debug overlays](screenshots/Brain-Path-Example.png)

Fabric mod for Minecraft 26.1.2 that unlocks all hidden developer features normally only available inside Mojang's IDE.

Sets `SharedConstants.IS_RUNNING_IN_IDE = true` and `MC_DEBUG_HOTKEYS=true` at class-load time.

---

## Server Commands

These commands are registered only when `IS_RUNNING_IN_IDE` is true. All require operator permissions at the level noted.

### `/raid` — Admin
Controls and inspects raid state. Requires a player to be in a valid raid region.

| Subcommand | Description |
|---|---|
| `start [omenlvl]` | Force-start a raid at your position with optional omen level |
| `stop` | Immediately end the active raid |
| `check` | Print current raid status to chat |
| `sound <type>` | Play a raid sound |
| `spawnleader` | Force-spawn a raid captain |
| `setomen <level>` | Override the current omen level |
| `glow` | Toggle glow on all active raiders |

### `/debugpath to <x> <y> <z>` — Gamemaster
Computes and visualizes the pathfinding route from your entity to the given block position. Renders the path in the world and reports whether the target is reachable.

### `/debugmobspawning at <x> <y> <z>` — Gamemaster
Forces the mob spawning evaluation at the specified position and prints the result for each spawn category. Useful for diagnosing why mobs are or aren't spawning.

### `/warden_spawn_tracker` — Gamemaster
Directly reads and writes the warden spawn tracker state for players in range.

| Subcommand | Description |
|---|---|
| `clear` | Reset the warning level for all nearby players |
| `set <warning_level>` | Set warning level to 0–4 on all nearby players |

### `/spawn_armor_trims *_lag_my_game [pattern]` — Gamemaster
Spawns a complete set of armor stands wearing every armor trim combination (all materials × all patterns). The subcommand is intentionally named `*_lag_my_game` — this will tank FPS on large registries.

### `/serverpack push <url> [uuid] [hash]` / `pop` — Gamemaster
Pushes a server resource pack URL to all connected clients, or pops the most recently pushed pack. Useful for testing pack delivery without restarting.

### `/debugconfig config <target>` / `unconfig <target>` — Admin (dedicated server only)
Sends or revokes a dialog config to a specific player by UUID. Used for testing the dialog/screen config system.

---

## Always-Available Commands

These are registered regardless of dev flags.

### `/chase` — Any op
Connects two game instances together so one follows another's camera. Intended for internal Mojang recording/QA sessions.

| Subcommand | Description |
|---|---|
| `lead [bind_address] [port]` | Start leading — other instances can follow you |
| `follow <host> [port]` | Follow another instance's camera |
| `stop` | Disconnect |

### `/test` — Any op
Runs the built-in game test framework. Only `minecraft:always_pass` is present in the public jar — Mojang's internal test suite is not shipped.

```
/test run minecraft:always_pass
```

---

## Debug Hotkeys (Client)

All hotkeys below require F3 to be held. They are unlocked by `MC_DEBUG_HOTKEYS=true` and are handled by `handleChunkDebugKeys` inside `KeyboardHandler`.

> **Note:** F3+F with this mod enabled toggles fog instead of cycling render distance. This is the dev-mode override.

| Hotkey | Action |
|---|---|
| F3+E | Toggle chunk section path debug overlay |
| F3+F | Toggle fog rendering on/off |
| F3+L | Toggle smart culling (`Minecraft.smartCull`) |
| F3+O | Toggle frustum culling octree overlay |
| F3+U | Capture/freeze frustum at current camera position |
| F3+Shift+U | Kill (release) the captured frustum |
| F3+V | Toggle chunk section visibility overlay |
| F3+W | Toggle wireframe rendering mode |

### What frustum capture does
F3+U freezes the view frustum in place. The game continues rendering from your new camera position but only draws chunks/entities that were visible from where you pressed the key. This lets you fly around and observe exactly what the frustum culling algorithm sees.

---

## Bonus: Verbose Command Errors

With `IS_RUNNING_IN_IDE = true`, command execution errors print a full stack trace to chat rather than just the error message. Useful when writing or debugging datapacks and function files.

---

## Installation

**Requirements**
- Minecraft **26.1.2**
- [Fabric Loader](https://fabricmc.net/use/installer/) **0.19.2** or later

**Steps**

1. Download the latest `devflags-fabric-*.jar` from the [Releases](../../releases/latest) page.
2. Place the jar in your instance's `mods/` folder:
   - **Prism Launcher** — right-click your instance → *Edit* → *Mods* → *Add from file*, or drop the jar into `<instance>/minecraft/mods/`
   - **Default launcher** — `%APPDATA%\.minecraft\mods\` (Windows) or `~/.minecraft/mods/` (macOS/Linux)
3. Launch Minecraft. No configuration needed — all debug flags are enabled automatically on startup.

> While waiting for Modrinth approval, builds are only available via the Releases page above.

---

## Build

```
./gradlew build
```

Output: `build/libs/devflags-fabric-1.0.0.jar`

Copy to your instance's `minecraft/mods/` folder, or use the VS Code task (`Cmd+Shift+B`) which builds and deploys to the "26.1.2 Dev Flags Enabled" Prism instance automatically.
