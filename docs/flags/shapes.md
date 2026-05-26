# Shapes

**Property:** `MC_DEBUG_SHAPES`
**Tab:** Renderers
**Requires restart:** Yes

## Description

Draws the collision shape, outline shape, and visual shape of every block in the world as colored wireframes. Collision shapes govern physical interactions, outline shapes define the cursor highlight, and visual shapes affect rendering. When these three shapes differ — as they do for many custom or irregular blocks — this flag makes those differences visible simultaneously.

## When to use

Enable this when implementing or verifying custom block shapes, checking hitbox accuracy, or diagnosing mismatches between a block's visual appearance and its collision or selection behavior.
