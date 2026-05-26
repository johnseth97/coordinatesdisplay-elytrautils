# Disable Carvers

**Property:** `MC_DEBUG_DISABLE_CARVERS`
**Tab:** World Generation
**Requires restart:** Yes

## Description

Skips all cave and canyon carver passes during world generation. The terrain is still shaped by the 3D noise density function, but no post-pass carving volumes are applied, resulting in solid terrain with no naturally generated caves or ravines.

## When to use

Use when testing surface and feature generation in isolation from cave systems, or when comparing terrain profiles before and after carver application.
