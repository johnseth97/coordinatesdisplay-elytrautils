# Dont Send Telemetry To Backend

**Property:** `MC_DEBUG_DONT_SEND_TELEMETRY_TO_BACKEND`
**Tab:** Misc
**Requires restart:** Yes

## Description

Prevents telemetry data from being transmitted to Mojang's backend servers. Data is still collected locally by the telemetry system but the outbound HTTP request is suppressed. Useful for development environments where sending real telemetry events would pollute production analytics.

## When to use

Enable on any dev or testing instance to avoid sending test gameplay data to Mojang's telemetry pipeline.
