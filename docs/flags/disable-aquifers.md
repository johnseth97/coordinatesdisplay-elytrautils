# Disable Aquifers

**Property:** `MC_DEBUG_DISABLE_AQUIFERS`
**Tab:** World Generation
**Requires restart:** Yes

## Description

Skips the aquifer pass during world generation, preventing underground water and lava bodies from being carved into cave systems. Cave geometry is still produced by carvers and noise, but the fluid fills that would normally appear at various depth layers are absent.

## When to use

Use when isolating carver output from aquifer influence, or when benchmarking generation speed with and without the aquifer calculation step.
