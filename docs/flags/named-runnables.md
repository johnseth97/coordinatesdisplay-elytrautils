# Named Runnables

**Property:** `MC_DEBUG_NAMED_RUNNABLES`
**Tab:** Misc
**Requires restart:** Yes

## Description

Wraps scheduled tasks and runnables with descriptive names derived from their call site. The names appear in thread dumps, profiler output, and async task queues, making it much easier to identify what each thread is working on during a performance investigation.

## When to use

Enable when taking thread dumps or profiler recordings and needing human-readable task names instead of anonymous lambda class identifiers.
