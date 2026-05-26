# JFR Profiling Enable Level Loading

**Property:** `MC_DEBUG_JFR_PROFILING_ENABLE_LEVEL_LOADING`
**Tab:** Gameplay
**Requires restart:** Yes

## Description

Enables Java Flight Recorder profiling events during level and chunk loading. JFR events capture method-level timing, allocation, and I/O data that can be analyzed in tools like JDK Mission Control. Without this flag, JFR events are not emitted during the loading phase.

## When to use

Enable when profiling world load times or chunk generation performance using JFR to identify hotspots in the loading pipeline.
