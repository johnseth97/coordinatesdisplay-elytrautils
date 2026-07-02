# CoordinatesDisplay ElytraUtils

A Fabric companion mod that injects an elytra flight telemetry row into
[CoordinatesDisplay](https://modrinth.com/mod/coordinates-display)'s HUD.

While gliding (`isFallFlying()`), an extra row appears beneath the normal
CoordinatesDisplay overlay:

```
Elytra  -18.4°  ✓ GLIDE  Vy -0.18  H 1.24
```

| Field | Meaning |
|---|---|
| Pitch | `-18.4°` — signed, negative = nose up |
| Status | `⚠ STALL` / `↑ CLIMB` / `✓ GLIDE` / `→ APPROACH` / `↓ DIVE`, overridden to `LAND NOW` when descending fast |
| Vy | Vertical speed, blocks/tick |
| H | Horizontal speed, blocks/tick |

The row disappears the instant you land. Toggle it off entirely with
`showElytraOverlay` in the mod's config file.

## Requirements

- Minecraft **1.21.11**, Fabric Loader **0.19.2+**
- [CoordinatesDisplay](https://modrinth.com/mod/coordinates-display) 19.x
- [BoxLib](https://modrinth.com/mod/boxlib) 20.x (CoordinatesDisplay's own dependency)
- Fabric API

## How it works

`HudMixin` (`src/main/java/com/johnseth97/cd_elytrautils/mixin/HudMixin.java`)
targets CoordinatesDisplay's `Hud.preRender(...)` — the single chokepoint every
display mode routes through — and wraps its assembled `RenderingLayout` in a new
outer column containing the original layout plus the elytra row. Because this
happens before `preRender` computes the on-screen anchor position, the added
row's size is folded into that calculation and the whole HUD repositions
correctly regardless of corner/anchor settings.

See [`agents.md`](agents.md) for the mixin-boundary rules this repo follows.

## Build

```
./gradlew build
```

Output: `build/libs/coordinatesdisplay-elytrautils-fabric-1.0.0.jar`

`cp .env.example .env` and edit the paths, then use the "Build & Deploy to Dev
Instance" editor task to build and copy the jar into your PrismLauncher
instance's `mods/` folder automatically.

## Publishing

```
./scripts/release.sh 1.0.0
```

Cuts a GitHub release; also publishes to Modrinth/CurseForge if
`MODRINTH_PROJECT_ID`/`CURSEFORGE_PROJECT_ID` repo variables are set.

---

Scaffolded from [modrinth-curseforge-repo-template](https://github.com/johnseth97/modrinth-curseforge-repo-template).
