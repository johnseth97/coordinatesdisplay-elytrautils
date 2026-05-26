# Show Local Server Entity Hit Boxes

**Property:** `MC_DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES`
**Tab:** Renderers
**Requires restart:** Yes

## Description

Renders the server-side entity hitboxes as reported by the integrated server, which may differ slightly from the client-side interpolated positions shown by the standard F3+B hitbox toggle. The server positions are authoritative for hit detection and damage, so discrepancies between these boxes and client-visual positions indicate interpolation or lag compensation artifacts.

## When to use

Use this when investigating hit registration issues, comparing server versus client entity positions, or debugging desync between the client rendering and server-side collision detection.
