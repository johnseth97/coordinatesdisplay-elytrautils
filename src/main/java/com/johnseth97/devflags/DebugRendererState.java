package com.johnseth97.devflags;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global toggle state for each debug renderer, keyed by simple class name.
 *
 * Kept separate from the mixin so state survives {@code refreshRendererList}, which
 * rebuilds the renderer list from scratch on every version change. Without this
 * class the mixin would lose user toggles on every rebuild.
 *
 * Unregistered keys default to {@code true} so newly discovered renderers are
 * visible until the user explicitly turns them off.
 */
public final class DebugRendererState {

  public static final Map<String, Boolean> ENABLED = new LinkedHashMap<>();

  private DebugRendererState() {}

  public static boolean isEnabled(String className) {
    // Default true: new renderers appear enabled until explicitly toggled off.
    return ENABLED.getOrDefault(className, true);
  }

  public static void setEnabled(String className, boolean enabled) {
    ENABLED.put(className, enabled);
  }

  public static void toggle(String className) {
    setEnabled(className, !isEnabled(className));
  }

  public static String friendlyName(String className) {
    String s = className
      .replaceAll("DebugRenderer$", "")
      .replaceAll("Renderer$", "")
      .replaceAll("Debug$", "");
    return s.replaceAll("([A-Z])", " $1").trim();
  }
}
