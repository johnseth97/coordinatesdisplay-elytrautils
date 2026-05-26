# Disable Below Zero Retrogeneration

**Property:** `MC_DEBUG_DISABLE_BELOW_ZERO_RETROGENERATION`
**Tab:** World Generation
**Requires restart:** Yes

## Description

Skips below-zero retrogen when loading old chunks into a 1.18+ world. Retrogen fills the newly accessible Y -64 to Y 0 range in pre-1.18 chunks with generated terrain matching the surrounding biome. With this flag enabled, that range remains empty (void) in old chunks instead.

## When to use

Use when testing how code handles the absence of below-zero terrain in legacy chunks, or when profiling the performance impact of retrogen on worlds with large areas of old chunk data.
