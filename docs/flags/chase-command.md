# Chase Command

**Property:** `MC_DEBUG_CHASE_COMMAND`
**Tab:** Gameplay
**Requires restart:** Yes

## Description

Enables the `/chase` command, which links two running game instances so one camera follows another. The leading instance broadcasts its camera position; the following instance renders from that viewpoint. Used internally by Mojang for recording gameplay footage with a controlled camera.

## When to use

Enable when you need a second game instance to track a player's perspective for recording, streaming, or QA walkthroughs.
