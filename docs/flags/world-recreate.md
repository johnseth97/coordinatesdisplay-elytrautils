# World Recreate

**Property:** `MC_DEBUG_WORLD_RECREATE`
**Tab:** Gameplay
**Requires restart:** Yes

## Description

Enables internal world-recreation tooling used by Mojang to regenerate worlds from their original seed. This is used to test whether generation changes produce different output compared to a previously generated world, making it possible to detect unintended generation regressions.

## When to use

Enable when comparing world generation output between two code states using the same seed to identify what changed.
