package com.johnseth97.cd_elytrautils;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared glide-range and durability-range math, used by both the HUD row
 * (informational display) and the Master Caution overlay (safety warning).
 * See GitHub issues #5, #6, #7 for the derivation of these formulas from
 * decompiled vanilla source.
 *
 * Time (ticks) is the canonical quantity here, not distance. A "bingo fuel"
 * style comparison — time until the elytra could break vs. time until the
 * ground — is safe against transient speed spikes (a rocket boost) because
 * neither side depends on horizontal speed. The blocks-based methods exist
 * for display purposes only and multiply a time window by *current*
 * horizontal speed, which is fine for an instantaneous "if this speed holds"
 * readout but was previously (incorrectly) also used for the Master Caution
 * safety comparison — a brief boost-induced speed spike, extrapolated across
 * the many-second-long durability window, grossly overstated flyable
 * distance and masked real risk. Don't use the blocks-based methods for
 * safety comparisons; use the ticks-based ones directly.
 */
public final class FlightMath {

    private static final double MAX_RAYCAST_DISTANCE = 320.0;

    // Elytra durability rolls once every 20 ticks of continuous fall-flying
    // (LivingEntity#updateFallFlying). Unbreaking only reduces the *chance*
    // a roll costs a point — it can never let more than 1 point break per
    // roll — so remainingDurability * 20 ticks is a hard floor on how fast
    // the elytra could possibly break, regardless of enchantments. Public so
    // HudMixin's flight-time estimate (issue #5) can share the same constant.
    public static final int DURABILITY_ROLL_INTERVAL_TICKS = 20;

    private FlightMath() {
    }

    public record GroundReading(double distance, boolean raycastHit) {
    }

    /**
     * Distance to the ground below the player, per the configured
     * {@link AltimeterMode} (issue #11):
     * <ul>
     *   <li>{@code RADAR} — straight-down raycast, falling back to the
     *       barometric reference if the raycast finds nothing.</li>
     *   <li>{@code BAROMETRIC} — always the fixed reference (sea level or a
     *       custom Y), no raycast.</li>
     *   <li>{@code AUTO} — radar below {@code autoSwitchoverHeight} AGL,
     *       barometric above it, mirroring real aircraft altimetry.</li>
     * </ul>
     */
    public static GroundReading findGroundDistance(LocalPlayer player) {
        ElytraUtilsConfig config = CoordinatesDisplayElytraUtils.getConfig();
        if (config.altimeterMode == AltimeterMode.BAROMETRIC) {
            return new GroundReading(barometricDistance(player, config), false);
        }

        GroundReading radar = raycastGroundDistance(player, config);
        if (config.altimeterMode == AltimeterMode.AUTO
                && radar.raycastHit()
                && radar.distance() > config.autoSwitchoverHeight) {
            return new GroundReading(barometricDistance(player, config), false);
        }
        return radar;
    }

    private static GroundReading raycastGroundDistance(LocalPlayer player, ElytraUtilsConfig config) {
        Level level = player.level();
        Vec3 from = player.position();
        double minY = Math.max(level.getMinY(), from.y - MAX_RAYCAST_DISTANCE);
        Vec3 to = new Vec3(from.x, minY, from.z);

        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult hit = level.clip(context);
        if (hit.getType() != HitResult.Type.MISS) {
            return new GroundReading(from.y - hit.getLocation().y, true);
        }
        return new GroundReading(barometricDistance(player, config), false);
    }

    private static double barometricDistance(LocalPlayer player, ElytraUtilsConfig config) {
        return Math.max(0.0, player.position().y - referenceY(player, config));
    }

    private static double referenceY(LocalPlayer player, ElytraUtilsConfig config) {
        return config.barometricUseSeaLevel ? player.level().getSeaLevel() : config.customBarometricY;
    }

    /**
     * Barometric altitude above the configured reference (sea level or a
     * custom Y), for the BALT tape (issue #10). Unlike {@link
     * #barometricDistance}, this is not clamped to zero — an altitude readout
     * below the reference (e.g. a ravine below sea level) should read
     * negative, not floor at the surface the way a "distance remaining"
     * quantity does.
     */
    public static double barometricAltitude(LocalPlayer player) {
        ElytraUtilsConfig config = CoordinatesDisplayElytraUtils.getConfig();
        return player.position().y - referenceY(player, config);
    }

    /**
     * Pure radar-altimeter reading for the RALT box (issue #10): straight-down
     * raycast only, with no barometric substitution on miss. Unlike {@link
     * #findGroundDistance} — which exists for safety/range math that must
     * always return *some* number — a dedicated RALT readout should show "no
     * lock" rather than silently displaying a barometric estimate as if it
     * were a raycast hit. Check {@link GroundReading#raycastHit()} before
     * trusting the distance.
     */
    public static GroundReading radarAltitude(LocalPlayer player) {
        return raycastGroundDistance(player, CoordinatesDisplayElytraUtils.getConfig());
    }

    /**
     * Ticks remaining before ground impact at the current descent rate, or -1
     * if not descending meaningfully.
     *
     * <p>The "meaningfully" part matters: this only projects an impact time
     * once {@code vy} is past {@link FlightConstants#DIVE_GREEN_VY}, the same
     * "mild, non-dangerous descent" boundary the text HUD's DIVE gradient
     * already uses — not just {@code vy >= 0}. Without that floor, {@code
     * distance / -vy} diverges toward infinity as {@code vy} approaches zero
     * from below, and Master Caution compares this against a *finite*
     * durability budget: once the projected time exceeds even a full-
     * durability elytra's tick count, "durability runs out before ground"
     * flips true and the warning fires — despite barely losing altitude,
     * which is the safest state, not the most dangerous one. This isn't a
     * rare corner case: rocket-boost-spam flying near the ground oscillates
     * {@code vy} across zero many times a second (each boost kicks it
     * positive, gravity pulls it back through zero before the next boost),
     * so the divergence was hit constantly at exactly the altitude/playstyle
     * where a false "ELYTRA DESTRUCTION IMMINENT" is most alarming. Every
     * consumer re-evaluates every tick, so there's no cost to treating "not
     * meaningfully descending right now" the same as "not descending" — next
     * tick recomputes from scratch regardless.
     */
    public static double ticksToGround(LocalPlayer player, double vy) {
        if (vy >= FlightConstants.DIVE_GREEN_VY) {
            return -1.0;
        }
        GroundReading ground = findGroundDistance(player);
        return ground.distance() / -vy;
    }

    /** Estimated blocks remaining before ground impact if the current speed holds, or -1 if not descending. Display only. */
    public static double glideRangeBlocks(LocalPlayer player, double horizontalSpeed, double vy) {
        double ticks = ticksToGround(player, vy);
        return ticks < 0.0 ? -1.0 : ticks * horizontalSpeed;
    }

    // Fall physics once fall-flying stops (LivingEntity#travelInAir, the
    // non-FlyingAnimal branch): vy = (vy - gravity) * FALL_DRAG each tick,
    // converging to a ~-3.92 blocks/tick terminal velocity for the default
    // 0.08 gravity. gravity itself is read live from the player's actual
    // Attributes.GRAVITY (matches how estimateFallImpactHearts in HudMixin
    // reads SAFE_FALL_DISTANCE/FALL_DAMAGE_MULTIPLIER live rather than
    // hardcoding vanilla defaults, so attribute modifiers are respected).
    private static final double FALL_DRAG = 0.98;

    // Sanity bound on the phase-2 simulation loop below — at terminal
    // velocity this is ~1000 blocks/tick*ticks of fall, far beyond any
    // realistic remaining distance; only exists to guarantee termination if
    // an upstream value is ever NaN/garbage, not something normal play can
    // reach.
    private static final int MAX_FALL_SIMULATION_TICKS = 12000;

    /**
     * "Truly accurate" ticks to impact — unlike {@link #ticksToGround}, this
     * doesn't naively extrapolate the *current glide rate* all the way to the
     * ground; it accounts for what actually happens if the elytra breaks
     * first. Two phases:
     * <ol>
     *   <li><b>Glide</b> — at the current descent rate, for
     *       {@link #durabilityLimitedTicks} (the guaranteed-floor durability
     *       window), or until the ground either way, whichever comes first.</li>
     *   <li><b>Free fall</b> — if durability runs out before the ground does,
     *       gliding stops ({@code canGlide()} goes false the tick after the
     *       item breaks) and normal gravity/drag physics take over for
     *       whatever distance is left, simulated tick-by-tick from the
     *       decompiled {@code travelInAir} formula rather than assumed.</li>
     * </ol>
     *
     * <p>Equals {@link #ticksToGround} exactly whenever durability isn't the
     * limiting factor (no elytra, or you'd land before it could break) — this
     * is a strict refinement, not a different number in the common case.
     * Margin-free by design (unlike Master Caution's configurable early-
     * warning lead time): it answers "will this genuinely happen," so a user
     * can disable the Master Caution banner and still get a truthful number
     * here. Returns -1 under the same "not meaningfully descending" condition
     * as {@link #ticksToGround}.
     */
    public static double accurateTicksToImpact(LocalPlayer player, double vy) {
        double ticksToGroundRaw = ticksToGround(player, vy);
        if (ticksToGroundRaw < 0.0) {
            return -1.0;
        }

        double durabilityTicks = durabilityLimitedTicks(player);
        if (durabilityTicks < 0.0 || durabilityTicks >= ticksToGroundRaw) {
            // No damageable elytra, or you reach the ground before it could
            // break — durability never becomes the limiting factor.
            return ticksToGroundRaw;
        }

        double glideDistance = durabilityTicks * -vy;
        double remainingDistance = findGroundDistance(player).distance() - glideDistance;
        if (remainingDistance <= 0.0) {
            return ticksToGroundRaw;
        }

        double gravity = player.getAttributeValue(Attributes.GRAVITY);
        double fallVy = vy;
        double fallenDistance = 0.0;
        int fallTicks = 0;
        while (fallenDistance < remainingDistance && fallTicks < MAX_FALL_SIMULATION_TICKS) {
            fallVy = (fallVy - gravity) * FALL_DRAG;
            fallenDistance += -fallVy;
            fallTicks++;
        }
        return durabilityTicks + fallTicks;
    }

    /**
     * Guaranteed-floor ticks flyable before the elytra breaks, or -1 if no
     * damageable elytra is equipped. Independent of speed entirely — this is
     * the "bingo fuel" quantity: how long until durability runs out, not how
     * far you'd travel if current speed held. Uses the fastest-possible break
     * time rather than the statistical average, since a death warning should
     * never have false negatives.
     */
    public static double durabilityLimitedTicks(LocalPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(Items.ELYTRA) || !chest.isDamageableItem()) {
            return -1.0;
        }
        int remainingDurability = chest.getMaxDamage() - chest.getDamageValue();
        return (double) remainingDurability * DURABILITY_ROLL_INTERVAL_TICKS;
    }

    /** Guaranteed-floor blocks flyable before the elytra breaks if the current speed holds, or -1 if no elytra equipped. Display only. */
    public static double durabilityLimitedBlocks(LocalPlayer player, double horizontalSpeed) {
        double ticks = durabilityLimitedTicks(player);
        return ticks < 0.0 ? -1.0 : ticks * horizontalSpeed;
    }
}
