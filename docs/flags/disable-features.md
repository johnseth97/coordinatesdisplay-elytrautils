# Disable Features

**Property:** `MC_DEBUG_DISABLE_FEATURES`
**Tab:** World Generation
**Requires restart:** Yes

## Description

Skips the entire feature decoration pass during world generation. Trees, flowers, grass, ore veins, geodes, amethyst clusters, and all other registered features are not placed. Chunks contain only raw terrain geometry, surface blocks, and fluid fills.

## When to use

Use when inspecting bare terrain shapes, testing surface rules, or measuring the performance cost of feature placement by comparing generation times with this flag on and off.
