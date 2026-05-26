# Neighborsupdate

**Property:** `MC_DEBUG_NEIGHBORSUPDATE`
**Tab:** Renderers
**Requires restart:** Yes

## Description

Renders a colored overlay on every block that received a neighbor update during the current tick, with brighter or more saturated colors indicating higher-frequency updates. This provides a visual heatmap of block update propagation, making it easy to trace how updates spread from a source through adjacent blocks. The overlay is highly laggy in areas with active redstone contraptions due to the volume of updates.

## When to use

Use this when investigating unexpected redstone behavior, block update loops, or to audit the update frequency of a complex contraption.
