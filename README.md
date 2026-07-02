# ElytraUtils

A Fabric companion mod that turns
[CoordinatesDisplay](https://modrinth.com/mod/coordinates-display)'s HUD into a
proper elytra flight instrument panel — pitch/attitude guidance, impact damage
prediction, glide range, and a "bingo fuel" durability warning, all derived
from Minecraft's actual flight physics rather than eyeballed guesses.

## What it shows

While gliding — or while standing on the ground with an elytra equipped and
rockets in hand (pre-launch) — extra rows appear attached to the
CoordinatesDisplay overlay:

```
Elytra  -18.4°  ✓ GLIDE  Vy -0.18  H 1.24
  Impact H:2.5❤ V:0.0❤
  Range 342 blocks
  Time to ground ~1:12
```

### Flight line

| Field | Meaning |
|---|---|
| Pitch | Signed degrees; negative = nose up |
| Status | Flight-regime indicator (below) |
| Vy | Vertical speed, blocks/tick |
| H | Horizontal speed, blocks/tick |

The **status** indicator, with derived optimal angles:

| Status | When | Color |
|---|---|---|
| `LAUNCH` | Pre-launch (grounded, elytra + rockets). Gradient vs. ideal takeoff angle | green→red gradient + ▲/▼/● arrow |
| `⚠ STALL` | Horizontal speed dropped near the ~0.35 blocks/tick airspeed floor | red |
| `CLIMB` | Nose-up. Gradient centered on the ideal rocket-climb angle (≈ -34°) | green→red gradient + arrow |
| `GLIDE` | Shallow. Green at level (max range) → red toward +30° (max speed) | green→red gradient + arrow |
| `DIVE` | Steep nose-down. Gradient on descent rate; red ≈ "pull up now" | green→red gradient |

The gradient arrows (▲ pitch up / ▼ pitch down / ● on target) tell you which way
to correct toward the optimal angle for that regime.

### Impact line

Predicted damage in hearts if you hit something *right now*, accounting for your
actual armor enchantments:

- **H** — horizontal wall-splat damage (reduced by Protection).
- **V** — vertical ground-impact damage (reduced by Protection **and** Feather
  Falling — Feather Falling correctly does *not* reduce wall impacts).

### Range & Time to ground

Distance and time until you meet the ground at the current glide slope, using
the configured altimeter mode (see below). Both hide themselves when you're not
descending.

### Master Caution

A screen-centered, slowly-flashing (epilepsy-safe 1 Hz) **ELYTRA DESTRUCTION
IMMINENT** warning that fires when your elytra will break from durability loss
*before* you reach the ground — i.e. you'd fall to your death mid-flight. It's a
"bingo fuel" style time-vs-time comparison, so it isn't fooled by rocket-boost
speed spikes.

## Configuration

Configure in-game via [ModMenu](https://modrinth.com/mod/modmenu) (optional), or
edit `config/coordinatesdisplay_elytrautils.json`.

| Setting | Default | Description |
|---|---|---|
| Show Elytra Overlay | on | Master toggle for the whole mod |
| Caution Threshold | 100 | Blocks of early-warning lead time before the death point; **0 = Master Caution off** |
| Master Caution Color | red | Color of the warning text |
| Altimeter Mode | Auto | `Radar` (raycast terrain), `Barometric` (fixed reference), or `Auto` (radar low, barometric high) |
| Switchover Height | 100 | In Auto mode, the AGL height to switch radar→barometric |
| Barometric = Sea Level | on | Use the dimension's sea level as the barometric reference |
| Barometric Y | 63 | Custom reference height when the above is off |
| Show Impact Line | on | Toggle the impact-damage row |
| Show Range Line | on | Toggle the range row |
| Show Flight Time Line | on | Toggle the time-to-ground row |

## Requirements

- Minecraft **1.21.11**, Fabric Loader **0.19.2+**
- [CoordinatesDisplay](https://modrinth.com/mod/coordinates-display) 19.x
- [BoxLib](https://modrinth.com/mod/boxlib) 20.x (CoordinatesDisplay's own dependency)
- Fabric API
- [ModMenu](https://modrinth.com/mod/modmenu) — optional, only for the in-game config screen

## Build & dev deploy

```
./gradlew build          # -> build/libs/coordinatesdisplay-elytrautils-fabric-<version>.jar
```

`cp .env.example .env`, edit the paths, then use the "Build & Deploy to Dev
Instance" editor task to build and copy the jar straight into your PrismLauncher
instance's `mods/` folder.

## Publishing

```
./scripts/release.sh 1.0.0
```

Cuts a GitHub release; also publishes to Modrinth/CurseForge when the
`MODRINTH_PROJECT_ID` / `CURSEFORGE_PROJECT_ID` repo variables are set.

---

Scaffolded from [modrinth-curseforge-repo-template](https://github.com/johnseth97/modrinth-curseforge-repo-template).
See [`agents.md`](agents.md) for the architecture and conventions.
