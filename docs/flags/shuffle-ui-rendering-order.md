# Shuffle UI Rendering Order

**Property:** `MC_DEBUG_SHUFFLE_UI_RENDERING_ORDER`
**Tab:** Misc
**Requires restart:** Yes

## Description

Randomizes the rendering order of UI widgets each frame. This is a stress-test tool for detecting z-ordering bugs that only manifest when widgets are drawn in a non-standard sequence. Any visual artifact that appears under shuffled order reveals an implicit ordering dependency in the rendering code.

## When to use

Enable when stress-testing UI rendering code for ordering dependencies. Not intended for normal gameplay — the screen will appear visually broken.
