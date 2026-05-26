# Verbose Command Errors

**Property:** `MC_DEBUG_VERBOSE_COMMAND_ERRORS`
**Tab:** Gameplay
**Requires restart:** Yes

## Description

When a command throws an exception during execution, the full Java stack trace is printed to chat instead of only the short error message. This applies to both built-in commands and datapack function files. The output includes exception type, message, and each stack frame down to the brigadier dispatcher.

## When to use

Enable when debugging datapacks, function files, or command-block chains where the short error message is not enough to identify the cause.
