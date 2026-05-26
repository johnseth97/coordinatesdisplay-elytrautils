# Disable Liquid Spreading

**Property:** `MC_DEBUG_DISABLE_LIQUID_SPREADING`
**Tab:** World Generation
**Requires restart:** Yes

## Description

Freezes all liquid blocks in place, preventing water and lava from spreading to adjacent blocks during normal gameplay. Existing fluid source and flowing blocks remain but do not schedule tick updates that would cause them to expand. This affects the live simulation, not world generation.

## When to use

Use when debugging terrain or structure layouts where fluid spreading would obscure block states, or when profiling tick performance with liquid simulation as a controlled variable.
