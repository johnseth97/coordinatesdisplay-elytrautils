# Disable Blending

**Property:** `MC_DEBUG_DISABLE_BLENDING`
**Tab:** World Generation
**Requires restart:** Yes

## Description

Skips the blending pass that smooths terrain at the boundary between old pre-1.18 chunks and newly generated 1.18+ chunks. Without blending, the height discontinuity at old/new chunk borders is fully visible as a sharp seam rather than being interpolated into surrounding terrain.

## When to use

Use when debugging blending logic itself, or when testing how terrain generation behaves at old-world boundaries without the blending mask applied.
