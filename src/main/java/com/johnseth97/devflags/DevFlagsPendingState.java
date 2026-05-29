package com.johnseth97.devflags;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public final class DevFlagsPendingState {

  private static final File PENDING_FILE = new File(
    System.getProperty("user.dir"),
    "config/devflags-pending.properties"
  );

  private static final Properties props = new Properties();

  static {
    load();
  }

  private DevFlagsPendingState() {}

  private static void load() {
    if (PENDING_FILE.exists()) {
      try (FileInputStream fis = new FileInputStream(PENDING_FILE)) {
        props.load(fis);
      } catch (IOException ignored) {}
    }
  }

  /** Called at game startup after flags have been applied — wipes any pending state. */
  public static void clear() {
    props.clear();
    PENDING_FILE.delete();
  }

  /** Call whenever a restart-required flag is toggled. */
  public static void onToggle(String key, boolean newValue) {
    boolean oldValue = !newValue;
    props.putIfAbsent(key, Boolean.toString(oldValue));
    boolean original = Boolean.parseBoolean(props.getProperty(key));
    if (newValue == original) {
      props.remove(key);
    }
    save();
  }

  public static boolean isDirty(String key) {
    if (!props.containsKey(key)) return false;
    boolean original = Boolean.parseBoolean(props.getProperty(key));
    return DevFlagsConfig.isEnabled(key) != original;
  }

  public static Set<String> getDirtyKeys() {
    Set<String> dirty = new LinkedHashSet<>();
    for (String key : props.stringPropertyNames()) {
      if (isDirty(key)) dirty.add(key);
    }
    return dirty;
  }

  private static void save() {
    try {
      PENDING_FILE.getParentFile().mkdirs();
      try (FileOutputStream fos = new FileOutputStream(PENDING_FILE)) {
        props.store(fos, "DevFlags pending restart state — cleared automatically on game launch.");
      }
    } catch (IOException ignored) {}
  }
}
