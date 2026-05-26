# Default Skin Override

**Property:** `MC_DEBUG_DEFAULT_SKIN_OVERRIDE`
**Tab:** Misc
**Requires restart:** Yes

## Description

Overrides all player skins with the default Steve or Alex skin, ignoring any custom skins fetched from Mojang's skin servers. This makes every player render with a predictable, known-good skin, removing skin fetching as a variable when testing skin-related rendering or UI code.

## When to use

Enable when testing skin rendering code, the skin selection UI, or any system that reacts to player appearance and needing a stable baseline skin state.
