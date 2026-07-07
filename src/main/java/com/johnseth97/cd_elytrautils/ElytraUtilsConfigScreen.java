package com.johnseth97.cd_elytrautils;

import dev.boxadactle.boxlib.gui.config.BOptionScreen;
import dev.boxadactle.boxlib.gui.config.widget.BSpacingEntry;
import dev.boxadactle.boxlib.gui.config.widget.button.BBooleanButton;
import dev.boxadactle.boxlib.gui.config.widget.button.BColorPickerButton;
import dev.boxadactle.boxlib.gui.config.widget.button.BEnumButton;
import dev.boxadactle.boxlib.gui.config.widget.label.BLabel;
import dev.boxadactle.boxlib.gui.config.widget.slider.BDoubleSlider;
import net.minecraft.ChatFormatting;
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

        addHeading("cd_elytrautils.config.heading_general");
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_overlay",
                config.showElytraOverlay,
                val -> config.showElytraOverlay = val));

        addConfigLine(new BSpacingEntry());

        addHeading("cd_elytrautils.config.heading_master_caution");
        addConfigLine(new BDoubleSlider(
                "cd_elytrautils.config.master_caution_threshold",
                0.0, 500.0,
                config.masterCautionThresholdBlocks,
                0,
                val -> config.masterCautionThresholdBlocks = val) {
            @Override
            protected String roundNumber(Double input) {
                return input <= 0.0 ? "Off" : super.roundNumber(input);
            }
        });
        addConfigLine(new BColorPickerButton(
                "cd_elytrautils.config.master_caution_color",
                this,
                false,
                config.masterCautionColor,
                val -> config.masterCautionColor = val));

        addConfigLine(new BSpacingEntry());

        addHeading("cd_elytrautils.config.heading_altimeter");
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

        addHeading("cd_elytrautils.config.heading_hud_lines");
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

        addConfigLine(new BSpacingEntry());

        addHeading("cd_elytrautils.config.heading_flight_instruments");
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_flight_instruments",
                config.showFlightInstruments,
                val -> config.showFlightInstruments = val));
        addConfigLine(new BDoubleSlider(
                "cd_elytrautils.config.flight_instrument_min_radar_altitude",
                0.0, 320.0,
                config.flightInstrumentMinRadarAltitude,
                0,
                val -> config.flightInstrumentMinRadarAltitude = val));
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.flight_instrument_min_radar_altitude_sticky",
                config.flightInstrumentMinRadarAltitudeSticky,
                val -> config.flightInstrumentMinRadarAltitudeSticky = val));
        addConfigLine(new BDoubleSlider(
                "cd_elytrautils.config.flight_instrument_scale",
                0.5, 2.0,
                config.flightInstrumentScale,
                2,
                val -> config.flightInstrumentScale = val));
        addConfigLine(new BColorPickerButton(
                "cd_elytrautils.config.flight_instrument_color",
                this,
                true,
                config.flightInstrumentColor,
                val -> config.flightInstrumentColor = val));
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_pitch_ladder",
                config.showPitchLadder,
                val -> config.showPitchLadder = val));
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_flight_path_marker",
                config.showFlightPathMarker,
                val -> config.showFlightPathMarker = val));
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_aoa_indicator",
                config.showAoaIndicator,
                val -> config.showAoaIndicator = val));
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_bearing_tape",
                config.showBearingTape,
                val -> config.showBearingTape = val));
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_airspeed_tape",
                config.showAirspeedTape,
                val -> config.showAirspeedTape = val));
        addConfigLine(new BBooleanButton(
                "cd_elytrautils.config.show_altitude_tape",
                config.showAltitudeTape,
                val -> config.showAltitudeTape = val));
        addConfigLine(new BDoubleSlider(
                "cd_elytrautils.config.immersive_hud_height",
                40.0, 300.0,
                config.immersiveHudHeightPixels,
                0,
                val -> config.immersiveHudHeightPixels = val));
    }

    /** Bold, non-interactive section header row (BLabel is inert — see BoxLib). */
    private void addHeading(String translationKey) {
        addConfigLine(new BLabel(Component.translatable(translationKey).withStyle(ChatFormatting.BOLD)));
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
