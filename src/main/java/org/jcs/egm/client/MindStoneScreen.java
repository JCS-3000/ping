package org.jcs.egm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jcs.egm.stones.stone_mind.MindStoneSuggestionEffect;

public class MindStoneScreen extends Screen {
    private int selected = 0;
    private final MindStoneSuggestionEffect[] options = MindStoneSuggestionEffect.values();

    public MindStoneScreen() {
        super(Component.literal("Mind Stone Suggestion Wheel"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        PoseStack pose = graphics.pose();
        int w = this.width / 2;
        int h = this.height / 2;
        int r = 60;

        for (int i = 0; i < options.length; ++i) {
            double angle = (Math.PI * 2.0 / options.length) * i - Math.PI / 2;
            int x = (int) (w + Math.cos(angle) * r);
            int y = (int) (h + Math.sin(angle) * r);

            boolean sel = (i == selected);
            Integer chatYellow = ChatFormatting.YELLOW.getColor();
            int color = sel ? (chatYellow != null ? chatYellow : 0xFFFF00) : 0xCCCCCC;


            graphics.drawCenteredString(this.font, options[i].getDisplayName(), x, y, color);
        }
        graphics.drawCenteredString(this.font, "Use A/D or Scroll to Select", w, h + r + 30, 0xAAAAAA);
        graphics.drawCenteredString(this.font, "Click to Confirm", w, h + r + 45, 0xAAAAAA);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        selected = (selected + options.length + (delta > 0 ? 1 : -1)) % options.length;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 65) selected = (selected + options.length - 1) % options.length;
        if (keyCode == 68) selected = (selected + 1) % options.length;
        if (keyCode == 256) { // ESC
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Store selection, menu closes
        org.jcs.egm.stones.stone_mind.MindStoneAbility.pendingEffect = options[selected];
        Minecraft.getInstance().setScreen(null);
        return true;
    }
}
