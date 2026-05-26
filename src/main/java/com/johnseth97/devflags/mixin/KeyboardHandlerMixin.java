package com.johnseth97.devflags.mixin;

import com.johnseth97.devflags.screen.DebugRenderScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts F3+Z in the debug key handler to open {@link com.johnseth97.devflags.screen.DebugRenderScreen}.
 *
 * Injecting into {@code handleDebugKeys} (rather than the generic key handler) is intentional:
 * vanilla already gates this method behind the F3 chord, so we get that check for free
 * instead of re-implementing it.
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Shadow(remap = false) private Minecraft minecraft;

    // F3+Z opens the debug renderer toggle screen. GLFW_KEY_Z = 90.
    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true, remap = false)
    private void devflags$openDebugScreen(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.key() == 90) {
            this.minecraft.setScreen(new DebugRenderScreen());
            cir.setReturnValue(true);
        }
    }
}
