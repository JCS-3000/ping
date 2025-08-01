package org.jcs.egm.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jcs.egm.holders.StoneHolderMenu;

public class StoneHolderScreen extends AbstractContainerScreen<StoneHolderMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("egm", "textures/gui/stonemenugui.png");

    public StoneHolderScreen(StoneHolderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;  // match your GUI size
        this.imageHeight = 169; // match your GUI size
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // No slot backgrounds (just like your Gauntlet GUI)
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // no titles or inventory label (matches gauntlet screen)
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
