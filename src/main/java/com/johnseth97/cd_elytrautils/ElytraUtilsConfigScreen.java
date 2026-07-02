package com.johnseth97.cd_elytrautils;

import dev.boxadactle.boxlib.gui.config.BOptionScreen;
import dev.boxadactle.boxlib.gui.config.widget.BSpacingEntry;
import dev.boxadactle.boxlib.gui.config.widget.button.BBooleanButton;
import dev.boxadactle.boxlib.gui.config.widget.button.BColorPickerButton;
import dev.boxadactle.boxlib.gui.config.widget.button.BEnumButton;
import dev.boxadactle.boxlib.gui.config.widget.slider.BDoubleSlider;
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
        super(parent, Component.literal("ElytraUtils"));
        CoordinatesDisplayElytraUtils.CONFIG.cacheConfig();
    }

    @Override
    protected void addOptions() {
        ElytraUtilsConfig config = CoordinatesDisplayElytraUtils.getConfig();

        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_overlay",
                config.showElytraOverlay,
                val -> config.showElytraOverlay = val));

        addConfigLine(new BSpacingEntry());

        // Master Caution
        addConfigLine(new BDoubleSlider(
                "cd_elytrautils.config.master_caution_threshold",
                0.0, 500.0,
                config.masterCautionThresholdBlocks,
                0,
                val -> config.masterCautionThresholdBlocks = val));
        addConfigLine(new BColorPickerButton(
                "cd_elytrautils.config.master_caution_color",
                this,
                false,
                config.masterCautionColor,
                val -> config.masterCautionColor = val));

        addConfigLine(new BSpacingEntry());

        // Altimeter
        addConfigLine(new BEnumButton<>(
                "cd_elytrautils.config.altimeter_mode",
                config.altimeterMode,
                AltimeterMode.class,
                val -> config.altimeterMode = val,
                0xFFFFFF));
        addConfigLine(new BDoubleSlider(
                "cd_elytrautils.config.auto_switchover_height",
                0.0, 320.0,
                config.autoSwitchoverHeight,
                0,
                val -> config.autoSwitchoverHeight = val));
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.barometric_use_sea_level",
                config.barometricUseSeaLevel,
                val -> config.barometricUseSeaLevel = val));
        addConfigLine(new BDoubleSlider(
                "cd_elytrautils.config.custom_barometric_y",
                -64.0, 320.0,
                config.customBarometricY,
                0,
                val -> config.customBarometricY = val));

        addConfigLine(new BSpacingEntry());

        // HUD line modules
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_impact_line",
                config.showImpactLine,
                val -> config.showImpactLine = val));
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_range_line",
                config.showRangeLine,
                val -> config.showRangeLine = val));
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_flight_time_line",
                config.showFlightTimeLine,
                val -> config.showFlightTimeLine = val));
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
