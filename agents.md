# agents.md

Conventions and hard-won gotchas for agents working in this repo. Assumes you
can read Java and Gradle.

## What this is

A **single-module Fabric** (client-only) mod, built on **fabric-loom**, that is a
*consumer* of [CoordinatesDisplay](https://modrinth.com/mod/coordinates-display):
it injects an elytra flight-instrument overlay into CD's HUD via mixin rather
than reimplementing HUD rendering. Fabric only — no NeoForge/Forge/Quilt, no
multiplatform split. Target Minecraft **1.21.x and forward**; do not add
backwards compatibility.

## Source map

- `CoordinatesDisplayElytraUtils` — `ClientModInitializer`; registers config +
  `MasterCautionOverlay`. Holds the public `CONFIG` handle.
- `ElytraUtilsConfig` — BoxLib `BConfig` data class (all settings).
- `ElytraUtilsConfigScreen` — BoxLib `BOptionScreen` settings UI.
- `ElytraUtilsModMenuIntegration` — optional ModMenu entrypoint.
- `AltimeterMode` — RADAR / BAROMETRIC / AUTO enum.
- `FlightMath` — shared ground-distance + durability math (the single source of
  truth for anything the HUD lines *and* Master Caution both need).
- `MasterCautionOverlay` — screen-centered warning, its own HUD layer.
- `mixin/HudMixin` — the one mixin; builds and attaches the overlay rows.

## The dependency boundary

**Never modify CoordinatesDisplay or BoxLib.** They're external
`modImplementation` deps from the Modrinth Maven (pinned by **version ID**, not
version number, in `gradle.properties`), required at runtime via
`fabric.mod.json` `depends`, not bundled. If CD doesn't expose a hook, that's a
mixin target to find — not a reason to fork it. **Mixin-first:** prefer injecting
into CD's existing behavior over reimplementing it.

## Build model

Standard **fabric-loom** + `loom.officialMojangMappings()`. Java 21 (matches MC
1.21.x). `org.gradle.java.home` in `gradle.properties` is a local override; CI
strips it. ModMenu is `modCompileOnly` (optional integration). Always verify with
`./gradlew build` and dev-deploy via the `.env` editor task.

## Gotchas that cost real debugging time — read before touching these areas

### Mixin injection point (`HudMixin`)
Injects at **`@Inject(method="preRender", at=@At("RETURN"))`** on
`dev.boxadactle.coordinatesdisplay.Hud`. It pins CD's already-position-resolved
layout where CD put it, then stacks our rows next to it. For bottom-anchored
corners (`BOTTOM_LEFT/RIGHT/BOTTOM`) it grows the wrapper **upward** so the
original block's bottom edge stays fixed.
- Do **not** revert to `@ModifyVariable`/intercepting before position is
  computed — feeding the taller layout into CD's anchor math makes corner HUDs
  visibly jump every time flight state toggles. This was tried and reverted.

### remap on mixin members
- MC members: loom remaps them — do **not** set `remap = false`.
- CD/BoxLib members: not Minecraft, so **not** remapped — the `@Mixin` and any
  `@At` targeting their methods use `remap = false`. See
  `@Mixin(value = Hud.class, remap = false)`.

### Client entrypoint, not main
Uses `ClientModInitializer` + the `"client"` entrypoint. Registering client
render registries (`HudElementRegistry`) from a `"main"` entrypoint is not
guaranteed-safe timing and silently failed to take effect. Keep it `client`.

### Zero-alpha colors silently don't render
`GuiGraphics#drawString` early-returns when `ARGB.alpha(color) == 0`. `0xFFFFFF`
has a zero alpha byte, so it draws **nothing** with no error — this cost a whole
debugging session on Master Caution. Config colors are stored **RGB-only** and
alpha is forced opaque at render time (color carried via `Style`, draw-call color
`0xFFFFFFFF`).

### Master Caution compares TIME, not distance ("bingo fuel")
`FlightMath.ticksToGround` vs `durabilityLimitedTicks`. An earlier
distance-vs-distance version multiplied a long, ~constant durability window by
*instantaneous* horizontal speed, so a rocket-boost speed spike inflated
"flyable distance" to absurd values and suppressed the warning. Neither
tick-based quantity depends on speed — keep it that way. The threshold adds a
speed-derived early-warning *margin*, but the margin-free crossover stays
speed-independent so a boost can't cause a missed warning at the real critical
point.

## Physics is derived, not guessed

All flight thresholds/optimal angles come from decompiling and numerically
simulating the actual game, not intuition or wiki paraphrase. The workflow, if
you need to (re)derive something:

1. loom already produces a Mojmap-named MC jar at
   `.gradle/loom-cache/minecraftMaven/.../minecraft-merged-*.jar`. CD/BoxLib
   remapped jars are under `.gradle/loom-cache/remapped_mods/`.
2. Decompile with CFR (`org.benf:cfr`) to read real source.
3. For dynamics, port the exact per-tick formula to a scratch simulation and
   scan the parameter (see the closed GitHub issues for the scripts/results).

Key sources already mined (cite these in code comments when relevant):
- `LivingEntity#updateFallFlyingMovement` — glide physics (gravity 0.08). Ideal
  glide ≈ 0° (max range); STALL is an airspeed floor (~0.35 blocks/tick), **not**
  a pitch — check horizontal speed, not angle.
- `FireworkRocketEntity#tick` boost branch — ideal rocket-climb ≈ -34°.
- `LivingEntity#updateFallFlying` — durability rolls once / 20 ticks; Unbreaking
  only reduces per-roll *chance*, so `remaining * 20` ticks is a hard floor.
- `LivingEntity` damage + `unbreaking.json`/`feather_falling.json`/tag files —
  wall (`fly_into_wall`) vs fall damage; both bypass armor points but not
  enchantment `damage_protection`; Feather Falling is `is_fall`-tagged only.

## BoxLib config UI

Reference implementation: CoordinatesDisplay's own `ConfigScreen` (decompile it).
- `addConfigLine(widget)` in `addOptions()`; footer via `createCancelButton` /
  `createSaveButton` in `initFooter`. Cache on open, restore on cancel, save on
  save.
- Widget labels are `Component.translatable(key, value)` format strings — **you
  must add a lang entry** (`assets/<modid>/lang/en_us.json`) as `"Label: %s"` or
  it renders the raw key and shows no live value. `BEnumButton` also needs a
  per-value key: `<key>.<enumname_lowercase>`.
- Slider display text: override `roundNumber(...)` (e.g. return `"Off"` at 0).

## Conventions

- Small, focused PRs — one concern per branch.
- Match surrounding style; comment *why*, especially for the gotchas above.
- Don't commit build output (`build/`, `bin/`, `.gradle/`); `.env` is per-machine.
- CI must stay green on `./gradlew build`.
- Track future work as GitHub issues; ground physics claims in derivation, not
  assumption.

## Publishing

`.github/workflows/publish.yml` fires on `v*` tags. GitHub release always runs;
Modrinth/CurseForge run only when their `*_PROJECT_ID` repo variables are set.
Use `./scripts/release.sh <version>`.
