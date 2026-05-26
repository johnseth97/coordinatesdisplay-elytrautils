package com.johnseth97.devflags.mixin;

import com.johnseth97.devflags.screen.DevFlagsConfigScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

  protected TitleScreenMixin() {
    super(Component.empty());
  }

  @Inject(method = "init", at = @At("TAIL"), remap = false)
  private void devflags$addMenuButton(CallbackInfo ci) {
    addRenderableWidget(
      Button.builder(Component.literal("Mojang Debug Tools"), b ->
        this.minecraft.setScreen(
          new DevFlagsConfigScreen((Screen) (Object) this)
        )
      )
        .pos(this.width - 126, 6)
        .size(120, 20)
        .build()
    );
  }
}
