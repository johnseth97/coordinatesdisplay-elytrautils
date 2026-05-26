# Dont Save World

**Property:** `MC_DEBUG_DONT_SAVE_WORLD`
**Tab:** Misc
**Requires restart:** Yes

## Description

Prevents the world from being written to disk entirely. Chunk data, player state, and level metadata are processed normally in memory but no save calls ever flush to the filesystem. The world reverts to its pre-session state the moment the process exits.

## When to use

Enable when running automated world-gen stress tests or profiling sessions where you want a clean slate on each launch without the overhead of save I/O polluting results.
