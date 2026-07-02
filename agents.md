# agents.md

Conventions for agents working in this repo. Assumes you can read Java and Gradle.

## What this is

A **single-module Fabric** mod, built on **fabric-loom**, that is a *consumer* of
[CoordinatesDisplay](https://modrinth.com/mod/coordinates-display) — it injects
into CoordinatesDisplay's HUD render pipeline via mixin rather than reimplementing
HUD rendering. Fabric only — no NeoForge, no Forge, no Quilt, no multiplatform
`common/`+`loader/` split.

Target Minecraft **1.21.x and forward**. Do not add backwards compatibility.

## The dependency boundary

**Never modify CoordinatesDisplay or BoxLib source.** They are external
`modImplementation` dependencies pulled from Modrinth (see `gradle.properties`
for pinned version IDs), required at runtime via `fabric.mod.json` `depends`,
and not bundled into our jar. If CoordinatesDisplay doesn't expose a hook we
need, that's a mixin target to find — not a reason to fork it.

**Mixin-first philosophy:** if something can be achieved by injecting into
CoordinatesDisplay's existing behavior, always prefer that over reimplementing
it ourselves. The elytra row, for example, is added by wrapping the
`RenderingLayout` that `Hud.preRender` already builds (see `HudMixin`), not by
writing a parallel HUD renderer.

## Build model

Standard **fabric-loom** with `loom.officialMojangMappings()`. `build.gradle`
resolves CoordinatesDisplay + BoxLib from the Modrinth Maven as
`modImplementation`; bump `coordinatesdisplay_version`/`boxlib_version` in
`gradle.properties` (these are Modrinth **version IDs**, not version numbers)
when updating.

- Java 21 (matches MC 1.21.x).
- `org.gradle.java.home` in `gradle.properties` is a local-machine override; CI
  strips it.

## Mixins

- References to **Minecraft** members: loom remaps them, do NOT set
  `remap = false`.
- References to **CoordinatesDisplay/BoxLib** members: not remapped by loom
  (they're not Minecraft), so mixins targeting them use `remap = false`. See
  `HudMixin`'s `@Mixin(value = Hud.class, remap = false)`.
- `HudMixin` uses `@ModifyVariable(... at = @At("STORE"))` to intercept the
  `RenderingLayout` local right after CoordinatesDisplay builds it, before
  `preRender` computes anchor position — this ensures our added row's size is
  included in that layout math. Don't switch this to an `@Inject` at `RETURN`;
  by then position is already computed against the un-widened layout and
  anchored HUDs (bottom/right corners) will clip.

## Conventions

- Prefer small, focused PRs — one concern per branch.
- Match surrounding code style; keep changes minimal.
- Do not commit build output (`build/`, `bin/`, `.gradle/`) — gitignored.
- `.env` is per-machine and gitignored.
- CI must stay green on `./gradlew build`.

## Publishing

`.github/workflows/publish.yml` fires on `v*` tags. GitHub release always runs;
Modrinth/CurseForge run only when their `*_PROJECT_ID` repo variables are set.
Use `./scripts/release.sh <version>` to tag and push.
