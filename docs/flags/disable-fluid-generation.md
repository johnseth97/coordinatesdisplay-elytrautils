# Disable Fluid Generation

**Property:** `MC_DEBUG_DISABLE_FLUID_GENERATION`
**Tab:** World Generation
**Requires restart:** Yes

## Description

Suppresses placement of fluid blocks during the world generation pipeline. Oceans, lakes, lava pools, and underwater terrain still carve the correct block shapes but no water or lava fill is applied, leaving the raw stone and dirt geometry exposed.

## When to use

Use when inspecting raw terrain topology or carver output without fluid volumes obscuring the underlying geometry.
