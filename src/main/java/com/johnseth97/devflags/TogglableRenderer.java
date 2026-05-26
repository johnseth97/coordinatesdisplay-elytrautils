package com.johnseth97.devflags;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.util.debug.DebugValueAccess;

/**
 * Wraps a {@link DebugRenderer.SimpleDebugRenderer} with a per-name enable gate.
 *
 * {@code refreshRendererList} creates brand-new renderer instances each call, so
 * this wrapper is recreated too. {@code putIfAbsent} preserves whatever the user
 * already toggled instead of resetting it to true on every rebuild.
 */
public final class TogglableRenderer implements DebugRenderer.SimpleDebugRenderer {

    private final DebugRenderer.SimpleDebugRenderer delegate;
    public final String name;

    public TogglableRenderer(DebugRenderer.SimpleDebugRenderer delegate) {
        this.delegate = delegate;
        this.name = delegate.getClass().getSimpleName();
        // putIfAbsent: retain existing toggle state across refreshRendererList rebuilds.
        DebugRendererState.ENABLED.putIfAbsent(this.name, true);
    }

    @Override
    public void emitGizmos(double x, double y, double z, DebugValueAccess access, Frustum frustum, float partialTick) {
        if (DebugRendererState.isEnabled(name)) {
            delegate.emitGizmos(x, y, z, access, frustum, partialTick);
        }
    }
}
