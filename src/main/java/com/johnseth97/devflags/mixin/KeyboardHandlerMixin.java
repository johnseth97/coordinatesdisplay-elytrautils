package com.johnseth97.devflags.mixin;

import com.johnseth97.devflags.screen.DebugRenderScreen;
import com.johnseth97.devflags.screen.DevFlagsConfigScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Shadow(remap = false) private Minecraft minecraft;

    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true, remap = false)
    private void devflags$openDebugScreen(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.key() == 90) {
            this.minecraft.setScreen(new DebugRenderScreen());
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true, remap = false)
    private void devflags$openConfigScreen(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.key() == 71) {
            this.minecraft.setScreen(new DevFlagsConfigScreen());
            cir.setReturnValue(true);
        }
    }
}
