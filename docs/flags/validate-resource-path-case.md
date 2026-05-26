# Validate Resource Path Case

**Property:** `MC_DEBUG_VALIDATE_RESOURCE_PATH_CASE`
**Tab:** Misc
**Requires restart:** Yes

## Description

Enables case-sensitivity validation on all resource location paths. A warning is logged whenever a path uses casing that differs from the actual file on disk. On macOS and Windows (case-insensitive filesystems) these mismatches are silent; they become hard failures on Linux. This flag surfaces them early.

## When to use

Enable when developing resource packs or mods on macOS or Windows to catch path casing errors before deploying to a Linux server or CI environment.
