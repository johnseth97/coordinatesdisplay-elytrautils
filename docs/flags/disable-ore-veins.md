# Disable Ore Veins

**Property:** `MC_DEBUG_DISABLE_ORE_VEINS`
**Tab:** World Generation
**Requires restart:** Yes

## Description

Skips the large ore vein generation pass (copper and iron proto-veins) introduced in 1.18. Standard ore blob placement via the feature pass is unaffected; only the dedicated vein noise layer that produces elongated ore bodies through the deep underground is suppressed.

## When to use

Use when isolating standard ore blob distribution from vein contributions, or when debugging ore density anomalies that may be caused by vein and blob overlap.
