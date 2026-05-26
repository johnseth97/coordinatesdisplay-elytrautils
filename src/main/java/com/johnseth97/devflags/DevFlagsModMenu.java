package com.johnseth97.devflags;

import com.johnseth97.devflags.screen.DevFlagsConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class DevFlagsModMenu implements ModMenuApi {

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return parent -> new DevFlagsConfigScreen(parent);
  }
}
