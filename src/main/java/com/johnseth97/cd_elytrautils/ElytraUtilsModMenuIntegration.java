package com.johnseth97.cd_elytrautils;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Only ever instantiated if ModMenu is installed and queries Fabric Loader's
 * "modmenu" entrypoint — this class (and its ModMenu API imports) never gets
 * loaded otherwise, so this is safe without ModMenu being a hard dependency.
 */
public class ElytraUtilsModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ElytraUtilsConfigScreen::new;
    }
}
