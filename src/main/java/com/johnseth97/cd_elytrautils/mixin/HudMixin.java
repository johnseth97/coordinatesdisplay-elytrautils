package com.johnseth97.cd_elytrautils.mixin;

import com.johnseth97.cd_elytrautils.CoordinatesDisplayElytraUtils;
import com.johnseth97.cd_elytrautils.FlightMath;
import dev.boxadactle.boxlib.layouts.RenderingLayout;
import dev.boxadactle.boxlib.layouts.component.LayoutContainerComponent;
import dev.boxadactle.boxlib.layouts.component.ParagraphComponent;
import dev.boxadactle.boxlib.layouts.layout.ColumnLayout;
import dev.boxadactle.boxlib.math.geometry.Rect;
import dev.boxadactle.coordinatesdisplay.Hud;
import dev.boxadactle.coordinatesdisplay.position.Position;
import dev.boxadactle.coordinatesdisplay.registry.DisplayMode;
import dev.boxadactle.coordinatesdisplay.registry.StartCorner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Appends an elytra telemetry row next to CoordinatesDisplay's HUD.
 *
 * Injects at {@code RETURN} of {@link Hud#preRender}, after CoordinatesDisplay
 * has already resolved the layout's on-screen anchor position from its own
 * (un-widened) size. The original layout is pinned at that resolved position
 * and the elytra row is stacked next to it — it must never feed back into the
 * anchor math, or corner-anchored HUDs visibly jump every time flight state
 * toggles (the row's height would change the size the anchor is computed from).
 */
@Mixin(value = Hud.class, remap = false)
public abstract class HudMixin {

    @Inject(method = "preRender", at = @At("RETURN"), cancellable = true)
    private void cd_elytrautils$appendElytraRow(
            Hud.RenderType thread, Position pos, int x, int y, DisplayMode renderMode, StartCorner startCorner,
            CallbackInfoReturnable<RenderingLayout> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !shouldShowOverlay(player)) {
            return;
        }

        RenderingLayout original = cir.getReturnValue();
        Rect<Integer> rect = original.calculateRect();

        boolean growUpward = startCorner == StartCorner.BOTTOM_LEFT
                || startCorner == StartCorner.BOTTOM_RIGHT
                || startCorner == StartCorner.BOTTOM;

        ParagraphComponent rowComponent = new ParagraphComponent(0, buildFlightLine(player), buildImpactLine(player), buildRangeLine(player), buildFlightTimeLine(player));
        ColumnLayout wrapper = new ColumnLayout(rect.getX(), rect.getY(), 2);
        if (growUpward) {
            // Keep the original block's bottom edge fixed: shift the wrapper
            // up by the row's height (+ the same padding ColumnLayout uses
            // between components) so the original still renders at rect.getY().
            wrapper.addComponent(rowComponent);
            wrapper.addComponent(new LayoutContainerComponent(original));
            wrapper.setPosition(rect.getX(), rect.getY() - rowComponent.getHeight() - 4);
        } else {
            wrapper.addComponent(new LayoutContainerComponent(original));
            wrapper.addComponent(rowComponent);
        }

        cir.setReturnValue(wrapper);
    }

    // Bucket boundaries and gradient targets below are derived by decompiling
    // the actual vanilla physics (LivingEntity#updateFallFlyingMovement,
    // FireworkRocketEntity#tick's boost term against an attached fall-flying
    // entity) and numerically simulating them, rather than assumed — see
    // GitHub issues #2 and #3 for the full derivation and simulation method.

    // CLIMB / GLIDE / DIVE pitch-bucket edges. STALL is no longer one of
    // these — see STALL_HORIZONTAL_SPEED below. There is no APPROACH bucket:
    // it added a static-color label with no real signal of its own once the
    // Impact line (issue #4) already covers "what happens if I touch down
    // now" directly — pitch >= GLIDE_RED_PITCH goes straight to DIVE.
    private static final float CLIMB_UPPER_PITCH = -8f;
    private static final float GLIDE_RED_PITCH = 30f;

    // STALL keys off horizontal airspeed, not pitch (issue #9). A pitch-only
    // check conflated the approach-to-stall region with legitimately good
    // climb angles — the -34 deg ideal climb angle (see IDEAL_CLIMB_PITCH)
    // sits well past where the old pitch-only STALL threshold used to fire.
    // ~0.35 blocks/tick is the simulated steady-state minimum airspeed
    // confirmed in #3's derivation (matches the wiki's ~7.2 m/s claim); 0.4
    // gives a small buffer above that asymptote.
    private static final float STALL_HORIZONTAL_SPEED = 0.4f;

    // DIVE's danger gradient is keyed on descent rate (vy), not pitch — pitch
    // only gates entry into the DIVE bucket itself (pitch >= GLIDE_RED_PITCH).
    // -0.45 is the exact threshold the old standalone "LAND NOW" override used
    // (issue #8); kept here as the gradient's red endpoint so removing that
    // override doesn't lose its meaning.
    private static final float DIVE_GREEN_VY = -0.05f;
    private static final float DIVE_RED_VY = -0.45f;

    // Ideal pitch to hold during a rocket's burn to maximize peak altitude
    // gained from that single burst (simulated: boost formula + burn duration
    // + post-burn glide, scanned across pitch). -34 deg sits just past the
    // -30 deg stall-onset threshold — the best climb angle is deliberately
    // right at the edge of losing airspeed, not comfortably inside it.
    private static final float IDEAL_CLIMB_PITCH = -34f;
    private static final float CLIMB_GRADIENT_HALF_WIDTH = 25f;

    // Ideal pitch for maximum glide ratio (distance per altitude lost) is
    // dead level; the gradient's speed-optimal end is the pitch nearest the
    // maximum simulated steady-state horizontal speed (~+53 deg), trimmed to
    // +30 deg since that's already ~93% of max speed and more likely to be
    // where players actually fly.
    private static final float IDEAL_GLIDE_PITCH = 0f;

    private static final float ARROW_TOLERANCE = 3f;

    private static final int COLOR_RED = 0xFF5555;
    private static final int COLOR_ORANGE = 0xFFAA00;
    private static final int COLOR_GREEN = 0x55FF55;

    // Impact damage estimate. Derived by decompiling LivingEntity's actual
    // damage formulas — not guessed:
    //   - Wall impact (fly_into_wall damage type): raw damage = horizontalSpeed*10 - 3
    //     (LivingEntity#handleFallFlyingCollisions models this as
    //     (speedBefore - speedAfter)*10 - 3; a HUD prediction assumes a wall
    //     fully arrests horizontal speed, i.e. speedAfter ~= 0).
    //   - Ground impact (fall damage type): raw damage = floor(max(0, fallDistance - safeFallDistance) * fallDamageMultiplier),
    //     using the player's live Entity#fallDistance and their actual
    //     SAFE_FALL_DISTANCE/FALL_DAMAGE_MULTIPLIER attributes rather than
    //     hardcoded vanilla defaults (3.0 / 1.0), so effects/attribute
    //     modifiers are respected.
    //   - Both damage types are tagged bypasses_armor (base armor points/
    //     toughness never apply), but enchantment "damage_protection" effects
    //     are a separate mechanic that still applies. Protection (1 point/level,
    //     summed across all 4 armor pieces) reduces both. Feather Falling
    //     (3 points/level, summed across all 4 armor pieces) is only tagged
    //     for is_fall damage, so it reduces ground impact only, NOT wall impact.
    //   - Reduction formula (CombatRules#getDamageAfterMagicAbsorb):
    //     damage * (1 - min(20, totalPoints) / 25).
    // See GitHub issue #4 for the full derivation.
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static boolean shouldShowOverlay(LocalPlayer player) {
        if (!CoordinatesDisplayElytraUtils.getConfig().showElytraOverlay) {
            return false;
        }
        if (player.isFallFlying()) {
            return true;
        }
        boolean hasElytraEquipped = player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        boolean holdingRockets = player.getMainHandItem().is(Items.FIREWORK_ROCKET)
                || player.getOffhandItem().is(Items.FIREWORK_ROCKET);
        return hasElytraEquipped && holdingRockets;
    }

    private static Component buildFlightLine(LocalPlayer player) {
        float pitch = player.getXRot();
        Vec3 velocity = player.getDeltaMovement();
        double vy = velocity.y;
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        Component status;
        if (!player.isFallFlying()) {
            // Pre-launch: elytra equipped + rockets in hand, but not airborne
            // yet (see shouldShowOverlay). Horizontal airspeed is ~0 on the
            // ground, so the STALL check below is meaningless here and would
            // always fire — show the same climb-angle gradient CLIMB uses
            // instead, since that's exactly what matters before jumping: is
            // this look angle a good one to launch and boost into?
            status = gradientStatus("LAUNCH", pitch, IDEAL_CLIMB_PITCH, CLIMB_GRADIENT_HALF_WIDTH, true);
        } else if (horizontalSpeed < STALL_HORIZONTAL_SPEED) {
            status = coloredText("⚠ STALL", COLOR_RED);
        } else if (pitch < CLIMB_UPPER_PITCH) {
            status = gradientStatus("CLIMB", pitch, IDEAL_CLIMB_PITCH, CLIMB_GRADIENT_HALF_WIDTH, true);
        } else if (pitch < GLIDE_RED_PITCH) {
            status = gradientStatus("GLIDE", pitch, IDEAL_GLIDE_PITCH, GLIDE_RED_PITCH, false);
        } else {
            status = diveGradientStatus(vy);
        }

        MutableComponent row = Component.literal("Elytra  ").withStyle(ChatFormatting.GRAY);
        row.append(Component.literal(String.format("%.1f°  ", pitch)).withStyle(ChatFormatting.WHITE));
        row.append(status);
        row.append(Component.literal("  "));
        row.append(Component.literal(String.format("Vy %.2f  ", vy)).withStyle(ChatFormatting.WHITE));
        row.append(Component.literal(String.format("H %.2f", horizontalSpeed)).withStyle(ChatFormatting.WHITE));
        return row;
    }

    private static Component buildImpactLine(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float wallImpactHearts = estimateWallImpactHearts(player, horizontalSpeed);
        float fallImpactHearts = estimateFallImpactHearts(player);

        MutableComponent row = Component.literal("  Impact ").withStyle(ChatFormatting.GRAY);
        row.append(impactHeartsText("H", wallImpactHearts));
        row.append(Component.literal(" "));
        row.append(impactHeartsText("V", fallImpactHearts));
        return row;
    }

    /** Damage from flying horizontally into a wall right now, in hearts, after Protection. */
    private static float estimateWallImpactHearts(LocalPlayer player, double horizontalSpeed) {
        float rawDamage = (float) (horizontalSpeed * 10.0 - 3.0);
        if (rawDamage <= 0f) {
            return 0f;
        }
        float protection = sumEnchantmentPoints(player, Enchantments.PROTECTION, 1f, 1f);
        float epf = Math.min(20f, protection);
        return (rawDamage * (1f - epf / 25f)) / 2f;
    }

    /** Damage from hitting the ground right now, in hearts, after Protection + Feather Falling. */
    private static float estimateFallImpactHearts(LocalPlayer player) {
        float safeFallDistance = (float) player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        float fallDamageMultiplier = (float) player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER);
        float fallDistance = (float) player.fallDistance;
        float rawDamage = (float) Math.floor(Math.max(0f, fallDistance - safeFallDistance) * fallDamageMultiplier);
        if (rawDamage <= 0f) {
            return 0f;
        }
        float protection = sumEnchantmentPoints(player, Enchantments.PROTECTION, 1f, 1f);
        float featherFalling = sumEnchantmentPoints(player, Enchantments.FEATHER_FALLING, 3f, 3f);
        float epf = Math.min(20f, protection + featherFalling);
        return (rawDamage * (1f - epf / 25f)) / 2f;
    }

    // Glide range math lives in FlightMath, shared with MasterCautionOverlay
    // (issue #7), which needs the same ground-distance and durability-range
    // numbers to decide when to warn. See GitHub issue #6.
    private static Component buildRangeLine(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        double vy = velocity.y;
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        MutableComponent row = Component.literal("  Range ").withStyle(ChatFormatting.GRAY);
        if (vy >= 0.0) {
            row.append(Component.literal("-- (not descending)").withStyle(ChatFormatting.DARK_GRAY));
            return row;
        }

        FlightMath.GroundReading ground = FlightMath.findGroundDistance(player);
        double blocksRemaining = ground.distance() * (horizontalSpeed / -vy);

        row.append(Component.literal(String.format("%.0f blocks", blocksRemaining)).withStyle(ChatFormatting.WHITE));
        if (!ground.raycastHit()) {
            row.append(Component.literal(" (sea level est.)").withStyle(ChatFormatting.DARK_GRAY));
        }
        return row;
    }

    // Flight time estimate. Elytra durability rolls every 20 ticks of
    // continuous flight (FlightMath.DURABILITY_ROLL_INTERVAL_TICKS); Unbreaking
    // (armor-slot branch, verified against unbreaking.json) gives a chance to
    // avoid losing a point per roll: (2*level) / (10 + 5*(level-1)). Shows both
    // the statistically-expected time and the guaranteed-floor (worst-case,
    // fastest-possible-break) time when Unbreaking makes them differ — see
    // issue #5.
    private static Component buildFlightTimeLine(LocalPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(Items.ELYTRA) || !chest.isDamageableItem()) {
            return Component.literal("  Flight time --").withStyle(ChatFormatting.DARK_GRAY);
        }

        int remainingDurability = chest.getMaxDamage() - chest.getDamageValue();
        int unbreakingLevel = getEnchantmentLevel(player, chest, Enchantments.UNBREAKING);
        double avoidChance = unbreakingLevel <= 0 ? 0.0 : (2.0 * unbreakingLevel) / (10.0 + 5.0 * (unbreakingLevel - 1));
        double loseChance = 1.0 - avoidChance;

        double expectedTicks = remainingDurability / (loseChance / FlightMath.DURABILITY_ROLL_INTERVAL_TICKS);
        double guaranteedFloorTicks = (double) remainingDurability * FlightMath.DURABILITY_ROLL_INTERVAL_TICKS;

        MutableComponent row = Component.literal("  Flight time ~").withStyle(ChatFormatting.GRAY);
        row.append(Component.literal(formatDuration(expectedTicks / 20.0)).withStyle(ChatFormatting.WHITE));
        if (unbreakingLevel > 0) {
            row.append(Component.literal(" (worst ").withStyle(ChatFormatting.DARK_GRAY));
            row.append(Component.literal(formatDuration(guaranteedFloorTicks / 20.0)).withStyle(ChatFormatting.DARK_GRAY));
            row.append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
        }
        return row;
    }

    private static String formatDuration(double seconds) {
        int totalSeconds = (int) Math.round(seconds);
        int minutes = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    private static int getEnchantmentLevel(LocalPlayer player, ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> enchantmentKey) {
        try {
            Holder<Enchantment> holder = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantmentKey);
            return stack.getEnchantments().getLevel(holder);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** Sums (base + perLevelAboveFirst*(level-1)) across every worn armor piece that has this enchantment. */
    private static float sumEnchantmentPoints(LocalPlayer player, net.minecraft.resources.ResourceKey<Enchantment> enchantmentKey, float base, float perLevelAboveFirst) {
        Holder<Enchantment> holder;
        try {
            holder = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantmentKey);
        } catch (RuntimeException e) {
            return 0f;
        }
        float total = 0f;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            int level = player.getItemBySlot(slot).getEnchantments().getLevel(holder);
            if (level > 0) {
                total += base + perLevelAboveFirst * (level - 1);
            }
        }
        return total;
    }

    private static Component impactHeartsText(String axisLabel, float hearts) {
        int color = hearts <= 0f ? COLOR_GREEN : hearts >= 10f ? COLOR_RED : threeStopGradient(hearts / 10f);
        return coloredText(String.format("%s:%.1f❤", axisLabel, hearts), color);
    }

    /**
     * Builds a status label whose color shifts green -> orange -> red as pitch
     * moves away from an ideal value, with a leading arrow indicating which
     * way to correct. If {@code symmetric}, distance from ideal is measured in
     * both directions (climb); otherwise only pitch beyond ideal counts
     * (glide's one-directional distance-vs-speed spectrum).
     */
    private static Component gradientStatus(String label, float pitch, float idealPitch, float span, boolean symmetric) {
        float signedOffset = pitch - idealPitch;
        float distance = symmetric ? Math.abs(signedOffset) : Math.max(0f, signedOffset);
        float t = clamp01(distance / span);
        int color = threeStopGradient(t);

        // Pitch is more positive (more nose-down) than ideal in both the climb
        // and glide cases, so "pitch up to correct" always means the same
        // sign of offset regardless of which bucket this is called from.
        String arrow;
        if (signedOffset > ARROW_TOLERANCE) {
            arrow = "▲";
        } else if (signedOffset < -ARROW_TOLERANCE) {
            arrow = "▼";
        } else {
            arrow = "●";
        }

        return coloredText(arrow + " " + label, color);
    }

    /** DIVE's danger gradient: green at a mild descent rate, red at the old LAND NOW threshold. See issue #8. */
    private static Component diveGradientStatus(double vy) {
        float t = clamp01((float) ((DIVE_GREEN_VY - vy) / (DIVE_GREEN_VY - DIVE_RED_VY)));
        int color = threeStopGradient(t);
        String arrow = t > 0.5f ? "▲" : "↓";
        return coloredText(arrow + " DIVE", color);
    }

    private static Component coloredText(String text, int rgb) {
        return Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    private static int threeStopGradient(float t) {
        t = clamp01(t);
        if (t < 0.5f) {
            return lerpColor(COLOR_GREEN, COLOR_ORANGE, t / 0.5f);
        }
        return lerpColor(COLOR_ORANGE, COLOR_RED, (t - 0.5f) / 0.5f);
    }

    private static int lerpColor(int from, int to, float t) {
        t = clamp01(t);
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
