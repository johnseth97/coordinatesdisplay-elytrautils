package com.johnseth97.cd_elytrautils;

import dev.boxadactle.boxlib.gui.config.BOptionScreen;
import dev.boxadactle.boxlib.gui.config.widget.button.BBooleanButton;
import dev.boxadactle.boxlib.gui.config.widget.button.BColorPickerButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings screen for this mod, built on the same BoxLib widget library
 * CoordinatesDisplay uses for its own config screen — {@code ConfigScreen}
 * in the CD source was used as the reference implementation for this class's
 * structure (addOptions/initFooter split, cache-on-open/restore-on-cancel
 * pattern).
 */
public class ElytraUtilsConfigScreen extends BOptionScreen {

    public ElytraUtilsConfigScreen(Screen parent) {
        super(parent, Component.literal("CoordinatesDisplay ElytraUtils"));
        CoordinatesDisplayElytraUtils.CONFIG.cacheConfig();
    }

    @Override
    protected void addOptions() {
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_overlay",
                CoordinatesDisplayElytraUtils.getConfig().showElytraOverlay,
                val -> CoordinatesDisplayElytraUtils.getConfig().showElytraOverlay = val));

        addConfigLine(new BColorPickerButton(
                "cd_elytrautils.config.master_caution_color",
                this,
                false,
                CoordinatesDisplayElytraUtils.getConfig().masterCautionColor,
                val -> CoordinatesDisplayElytraUtils.getConfig().masterCautionColor = val));
    }

    @Override
    protected void initFooter(LinearLayout layout) {
        layout.addChild(createCancelButton(b -> {
            Minecraft.getInstance().setScreen(this.parent);
            CoordinatesDisplayElytraUtils.CONFIG.restoreCache();
        }));
        setSaveButton(layout.addChild(createSaveButton(b -> {
            Minecraft.getInstance().setScreen(this.parent);
            CoordinatesDisplayElytraUtils.CONFIG.save();
        })));
    }
}
