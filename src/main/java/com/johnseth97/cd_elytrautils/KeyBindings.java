package com.johnseth97.cd_elytrautils;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;

/**
 * Hotkeys to toggle the immersive (flight instrument) HUD and the legacy
 * text HUD independently of the config screen. Both are unbound by default —
 * unbound rather than defaulting to some key that might collide with another
 * mod or vanilla binding — so the player opts in via the Controls menu.
 *
 * <p>Pressing a hotkey only flips the same config booleans the config screen
 * buttons already control ({@code showFlightInstruments} /
 * {@code showElytraOverlay}); it doesn't change either overlay's own
 * visibility gate (fall-flying, or elytra-equipped-plus-rockets — see
 * {@code FlightInstrumentOverlay} / {@link FlightMath#elytraFlightActiveOrReady}).
 * When that gate isn't currently satisfied, toggling produces no visible
 * change on its own, so a fading {@link HudToastOverlay} message confirms the
 * key press actually registered.
 */
public final class KeyBindings {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(CoordinatesDisplayElytraUtils.MOD_ID, "main"));

    private static final KeyMapping TOGGLE_IMMERSIVE_HUD = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.coordinatesdisplay_elytrautils.toggle_immersive_hud",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY));

    private static final KeyMapping TOGGLE_TEXT_HUD = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.coordinatesdisplay_elytrautils.toggle_text_hud",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY));

    private KeyBindings() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(KeyBindings::tick);
    }

    private static void tick(Minecraft client) {
        while (TOGGLE_IMMERSIVE_HUD.consumeClick()) {
            toggleImmersiveHud(client.player);
        }
        while (TOGGLE_TEXT_HUD.consumeClick()) {
            toggleTextHud(client.player);
        }
    }

    private static void toggleImmersiveHud(LocalPlayer player) {
        ElytraUtilsConfig config = CoordinatesDisplayElytraUtils.getConfig();
        config.showFlightInstruments = !config.showFlightInstruments;
        CoordinatesDisplayElytraUtils.CONFIG.save();

        // FlightInstrumentOverlay's actual gate is fall-flying alone (not the
        // equipped+rockets pre-launch state the text HUD also accepts) — this
        // check mirrors that exactly so the popup only appears when the
        // toggle genuinely wouldn't otherwise show on screen.
        if (player != null && !player.isFallFlying()) {
            showToggleToast("Flight Instruments", config.showFlightInstruments);
        }
    }

    private static void toggleTextHud(LocalPlayer player) {
        ElytraUtilsConfig config = CoordinatesDisplayElytraUtils.getConfig();
        config.showElytraOverlay = !config.showElytraOverlay;
        CoordinatesDisplayElytraUtils.CONFIG.save();

        if (player != null && !FlightMath.elytraFlightActiveOrReady(player)) {
            showToggleToast("Text Hud", config.showElytraOverlay);
        }
    }

    /** "<label> Enabled" in green, or "<label> Disabled" in red — reflects the new state, not a fixed message. */
    private static void showToggleToast(String label, boolean enabled) {
        String suffix = enabled ? " Enabled" : " Disabled";
        int color = enabled ? FlightColors.COLOR_GREEN : FlightColors.COLOR_RED;
        HudToastOverlay.show(label + suffix, color);
    }
}
