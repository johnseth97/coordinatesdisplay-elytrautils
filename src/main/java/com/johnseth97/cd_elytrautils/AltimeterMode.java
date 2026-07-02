package com.johnseth97.cd_elytrautils;

/**
 * How FlightMath resolves "distance to ground" for the Range/Time-to-ground
 * lines and Master Caution's comparison. See GitHub issue #11.
 */
public enum AltimeterMode {
    /** Straight-down raycast against real terrain. */
    RADAR,
    /** Fixed reference (sea level or a custom Y), no raycast. */
    BAROMETRIC,
    /** Radar below {@code autoSwitchoverHeight} AGL, barometric above it — mirrors real aircraft altimetry. */
    AUTO
}
