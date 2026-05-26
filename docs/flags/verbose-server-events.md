# Verbose Server Events

**Property:** `MC_DEBUG_VERBOSE_SERVER_EVENTS`
**Tab:** Gameplay
**Requires restart:** Yes

## Description

Logs server-side game events — including vibration events, block events, and entity events — verbosely to the log file. Each event includes its type, source position, and affected listeners. Can produce a large volume of log output in active worlds.

## When to use

Enable when tracing sculk sensor triggers, warden detection chains, or block event propagation where the normal log output is too sparse to follow the event flow.
