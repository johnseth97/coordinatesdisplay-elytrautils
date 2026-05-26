# Disable Surface

**Property:** `MC_DEBUG_DISABLE_SURFACE`
**Tab:** World Generation
**Requires restart:** Yes

## Description

Skips the surface builder pass during world generation. The top layer of biome-specific blocks — grass, sand, gravel, podzol, and similar surface materials — is not applied, leaving raw stone exposed at the terrain surface across all biomes.

## When to use

Use when debugging noise-layer terrain shapes where surface decoration would hide the underlying density function output, or when testing cave and underground biome logic in isolation.
