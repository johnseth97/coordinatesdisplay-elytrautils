package com.johnseth97.devflags.screen;

import com.johnseth97.devflags.DebugRendererState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game overlay for toggling individual debug renderers at runtime.
 *
 * Opens via F3+Z (intercepted by {@link com.johnseth97.devflags.mixin.KeyboardHandlerMixin}).
 * Reads and mutates {@link DebugRendererState#ENABLED}; changes take effect on the
 * next frame because {@link com.johnseth97.devflags.TogglableRenderer#emitGizmos} checks
 * the map each call.
 */
public class DebugRenderScreen extends Screen {

    private static final int BTN_W = 200;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 4;
    private static final int BTN_STRIDE = BTN_H + BTN_GAP;
    private static final int COLS = 2;

    public DebugRenderScreen() {
        super(Component.literal("Debug Renderers"));
    }

    @Override
    protected void init() {
        List<String> keys = new ArrayList<>(DebugRendererState.ENABLED.keySet());
        int rows = (int) Math.ceil((double) keys.size() / COLS);
        int startX = this.width / 2 - (COLS * BTN_W + (COLS - 1) * BTN_GAP) / 2;
        int startY = 36;

        for (int i = 0; i < keys.size(); i++) {
            String cls = keys.get(i);
            int col = i % COLS;
            int row = i / COLS;
            int x = startX + col * (BTN_W + BTN_GAP);
            int y = startY + row * BTN_STRIDE;

            addRenderableWidget(Button.builder(toggleLabel(cls), b -> {
                DebugRendererState.toggle(cls);
                b.setMessage(toggleLabel(cls));
            }).pos(x, y).size(BTN_W, BTN_H).build());
        }

        int closeY = startY + rows * BTN_STRIDE + 8;
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
            .pos(this.width / 2 - 50, closeY).size(100, BTN_H).build());
    }

    // MC 26.1.2 replaced Screen#render with extractRenderState for the new render-state
    // pipeline; this is the correct override point for background fill + title in this version.
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, this.width, this.height, 0xB0000000);
        g.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        g.centeredText(this.font, Component.literal("F3+Z to open | Click to toggle"), this.width / 2, 21, 0x888888);
        super.extractRenderState(g, mouseX, mouseY, partial);
    }

    // Keep the game running while the overlay is open so renderers keep drawing.
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component toggleLabel(String cls) {
        boolean on = DebugRendererState.isEnabled(cls);
        String name = DebugRendererState.friendlyName(cls);
        MutableComponent status = Component.literal(on ? " [ON]" : " [OFF]")
            .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
        return Component.literal(name).append(status);
    }
}
