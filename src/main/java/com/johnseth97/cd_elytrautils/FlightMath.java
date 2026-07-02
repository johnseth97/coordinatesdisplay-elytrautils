package com.johnseth97.cd_elytrautils;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
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
     * Distance to the ground below the player. Raycasts straight down for a
     * real "radar altimeter" reading; falls back to the current dimension's
     * configured sea level (itself dimension-type data, not an
     * Overworld-specific constant) when the raycast finds nothing.
     */
    public static GroundReading findGroundDistance(LocalPlayer player) {
        Level level = player.level();
        Vec3 from = player.position();
        double minY = Math.max(level.getMinY(), from.y - MAX_RAYCAST_DISTANCE);
        Vec3 to = new Vec3(from.x, minY, from.z);

        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult hit = level.clip(context);
        if (hit.getType() != HitResult.Type.MISS) {
            return new GroundReading(from.y - hit.getLocation().y, true);
        }

        double seaLevelDistance = from.y - level.getSeaLevel();
        return new GroundReading(Math.max(0.0, seaLevelDistance), false);
    }

    /** Ticks remaining before ground impact at the current descent rate, or -1 if not descending. */
    public static double ticksToGround(LocalPlayer player, double vy) {
        if (vy >= 0.0) {
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
