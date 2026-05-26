# Allow Low Sim Distance

**Property:** `MC_DEBUG_ALLOW_LOW_SIM_DISTANCE`
**Tab:** Gameplay
**Requires restart:** Yes

## Description

Removes the lower bound on the simulation distance slider, allowing values as low as 2 chunks. Normally the game enforces a minimum simulation distance to prevent logic failures near the player. With this flag the slider can be set to any value the UI allows.

## When to use

Enable when profiling tick performance at minimal load, or when testing behavior at the boundary between simulated and unsimulated chunks.
