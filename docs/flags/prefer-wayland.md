# Prefer Wayland

**Property:** `MC_DEBUG_PREFER_WAYLAND`
**Tab:** Misc
**Requires restart:** Yes

## Description

On Linux systems where both Wayland and X11 are available, instructs LWJGL to prefer the Wayland display protocol. By default the game may prefer X11 for compatibility. This flag is a no-op on macOS and Windows.

## When to use

Enable on Linux when testing Wayland-specific rendering behavior, input handling, or HiDPI scaling that differs from the X11 path.
