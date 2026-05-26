# Entity Block Intersection

**Property:** `MC_DEBUG_ENTITY_BLOCK_INTERSECTION`
**Tab:** Renderers
**Requires restart:** Yes

## Description

Highlights every block whose volume overlaps with an entity's bounding box during the current tick, giving a per-tick view of which blocks the game considers the entity to be intersecting. This is distinct from collision resolution — it shows raw bounding box overlap rather than the blocks an entity is pushing against. The overlay updates each tick as entities move.

## When to use

Enable this when debugging suffocation damage, entity-block interaction triggers, or any behavior that depends on knowing which blocks an entity is currently overlapping.
