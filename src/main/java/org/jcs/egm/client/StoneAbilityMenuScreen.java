package org.jcs.egm.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class StoneAbilityMenuScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("egm:textures/gui/stoneabilitygui.png");
    private static final ResourceLocation ARROW_TEX =
            new ResourceLocation("egm:textures/gui/arrow.png");

    private final ItemStack stoneStack;
    private final InteractionHand hand;
    private final List<Component> abilityNames;
    private int selectedIndex;
    private final int menuWidth = 176;
    private final int menuHeight = 107;
    private int guiLeft;
    private int guiTop;

    // Scrolling
    private static final int VISIBLE_ENTRIES = 4;
    private int scrollOffset = 0;

    public StoneAbilityMenuScreen(ItemStack stoneStack, InteractionHand hand, List<Component> abilityNames, int selectedIndex) {
        super(Component.literal("Select Ability"));
        this.stoneStack = stoneStack;
        this.hand = hand;
        this.abilityNames = abilityNames;
        this.selectedIndex = selectedIndex;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - menuWidth) / 2;
        this.guiTop = (this.height - menuHeight) / 2;
        updateScrollOffset();
        super.init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        RenderSystem.setShaderTexture(0, TEXTURE);
        graphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, menuWidth, menuHeight);

        // Draw the title at the fixed position (do not change this Y)
        graphics.drawCenteredString(this.font, this.title, this.width / 2, guiTop + 10, 0xFFFFFF);

        // Spacing setup
        int entryHeight = 18;
        int startY = guiTop + 34; // Leaves room for the title

        // Calculate visible abilities (scrolling)
        int total = abilityNames.size();
        int firstIdx = scrollOffset;
        int lastIdx = Math.min(firstIdx + VISIBLE_ENTRIES, total);

        for (int i = firstIdx; i < lastIdx; i++) {
            int row = i - firstIdx;
            int y = startY + row * entryHeight;

            // Draw arrow for selected
            if (i == selectedIndex) {
                RenderSystem.setShaderTexture(0, ARROW_TEX);
                // Position: 24px left of text center, 4px up to center arrow with text (tweak as needed)
                graphics.blit(ARROW_TEX,
                        this.width / 2 - 55, // arrow relative left/right
                        y - 2,   // arrow relative to text
                        0, 0,
                        12, 12,
                        12, 12);
            }

            graphics.drawCenteredString(this.font, abilityNames.get(i), this.width / 2, y, 0xCCCCCC);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void updateScrollOffset() {
        // Ensure selected is visible if there are more than 4 abilities
        if (abilityNames.size() <= VISIBLE_ENTRIES) {
            scrollOffset = 0;
        } else {
            if (selectedIndex < scrollOffset) {
                scrollOffset = selectedIndex;
            } else if (selectedIndex >= scrollOffset + VISIBLE_ENTRIES) {
                scrollOffset = selectedIndex - VISIBLE_ENTRIES + 1;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int size = abilityNames.size();
        if (size > 0) {
            if (delta < 0) {
                selectedIndex = (selectedIndex + 1) % size;
            } else if (delta > 0) {
                selectedIndex = (selectedIndex - 1 + size) % size;
            }
            saveSelection();
            updateScrollOffset();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            int size = abilityNames.size();
            selectedIndex = (selectedIndex + 1) % size;
            saveSelection();
            updateScrollOffset();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            int size = abilityNames.size();
            selectedIndex = (selectedIndex - 1 + size) % size;
            saveSelection();
            updateScrollOffset();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        saveSelection();
        super.onClose();
    }

    /** Save the current selection to the stack (client side) */
    private void saveSelection() {
        if (stoneStack != null && stoneStack.hasTag()) {
            stoneStack.getTag().putInt("AbilityIndex", selectedIndex);
        } else if (stoneStack != null) {
            stoneStack.getOrCreateTag().putInt("AbilityIndex", selectedIndex);
        }
    }
}
