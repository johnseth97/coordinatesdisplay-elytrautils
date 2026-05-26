# Force Telemetry

**Property:** `MC_DEBUG_FORCE_TELEMETRY`
**Tab:** Misc
**Requires restart:** Yes

## Description

Forces telemetry events to be submitted to Mojang's backend regardless of the player's telemetry opt-out setting. Intended for internal Mojang testing to verify that the telemetry pipeline is functioning end-to-end under conditions that would otherwise suppress submissions.

## When to use

Use when validating the telemetry submission path itself — for example, confirming that a new event type is being received by the backend — where the opt-out suppression would otherwise block the test.
