# Goal Selector

**Property:** `MC_DEBUG_GOAL_SELECTOR`
**Tab:** Renderers
**Requires restart:** Yes

## Description

Renders each mob's full list of AI goals as floating text above the entity, showing each goal's priority, name, and whether it is currently running or blocked. Active goals are distinguished from candidate goals that are eligible but not currently executing. This gives a complete picture of which behaviors are competing for control of the mob at any given moment.

## When to use

Enable this when debugging custom AI goal interactions, priority conflicts between goals, or unexpected mob behavior caused by a goal not starting or stopping at the right time.
