package com.johnseth97.devflags.mixin;

import com.johnseth97.devflags.TogglableRenderer;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Wraps every {@link DebugRenderer.SimpleDebugRenderer} in a {@link com.johnseth97.devflags.TogglableRenderer}
 * after each {@code refreshRendererList} call so the UI can individually gate them.
 */
@Mixin(DebugRenderer.class)
public class DebugRendererMixin {

    @Shadow(remap = false)
    private List<DebugRenderer.SimpleDebugRenderer> renderers;

    // Runs after refreshRendererList rebuilds the list each frame (version-change triggered).
    // Wraps every plain renderer in a TogglableRenderer so the UI can gate each one.
    @Inject(method = "refreshRendererList", at = @At("TAIL"), remap = false)
    private void devflags$wrapRenderers(CallbackInfo ci) {
        for (int i = 0; i < renderers.size(); i++) {
            DebugRenderer.SimpleDebugRenderer r = renderers.get(i);
            if (!(r instanceof TogglableRenderer)) {
                renderers.set(i, new TogglableRenderer(r));
            }
        }
    }
}
