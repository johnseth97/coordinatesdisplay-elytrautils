# Dev Commands

**Property:** `MC_DEBUG_DEV_COMMANDS`
**Tab:** Gameplay
**Requires restart:** Yes

## Description

Registers additional internal Mojang development commands that are not present in normal gameplay, beyond what `IS_RUNNING_IN_IDE` already enables. These commands are gated separately so that even a dev-mode session can opt in or out of the extra command surface. The exact commands exposed depend on the current game version's internal tooling.

## When to use

Enable when you need access to the full set of internal dev commands and suspect some are missing even after `IS_RUNNING_IN_IDE` is set.
