# Shuffle Models

**Property:** `MC_DEBUG_SHUFFLE_MODELS`
**Tab:** Misc
**Requires restart:** Yes

## Description

Randomizes model rendering order each frame as a stress test for the render pipeline. Any visual artifact that appears under shuffled order reveals an implicit draw-order dependency, such as incorrect blending, missing depth writes, or order-dependent transparency.

## When to use

Enable when stress-testing the render pipeline for order-dependent bugs. Not intended for normal gameplay — rendering will appear visually incorrect.
