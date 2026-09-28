package net.teogemini.nova.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkData;
import net.teogemini.nova.screen.NovaInventoryMenu;

public final class NovaInventoryScreen extends AbstractContainerScreen<NovaInventoryMenu> {
    private static final Identifier BACKGROUND = ROBOTNOVA_MOD.id("textures/gui/nova_inventory.png");
    private static final Identifier BARS = ROBOTNOVA_MOD.id("textures/gui/nova_status_bars.png");
    private static final Identifier SCROLLBAR = ROBOTNOVA_MOD.id("textures/gui/nova_inventory_scrollbar.png");
    private boolean draggingScrollbar;

    public NovaInventoryScreen(NovaInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 256;
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0, 0, 256, 256, 256, 256);
        if (minecraft.level != null && minecraft.level.getEntity(menu.entityId()) instanceof NovaEntity nova) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g,
                    x + 14, y + 30, x + 70, y + 90,
                    28, 0.0F, mouseX, mouseY, nova);
        }
        g.drawString(font, "Lv. " + menu.skillLevel(), x + 108, y + 33, 0xFF123D5A, false);
        drawStatus(g, x + 87, y + 46, 0,
                menu.skillLevel() >= 5 ? 1 : (float) menu.experience() / menu.maxExperience());
        drawStatus(g, x + 87, y + 60, 1, menu.health() / menu.maxHealth());
        drawStatus(g, x + 87, y + 74, 2, menu.energyFraction());

        for (int row = 0; row < 4; row++) {
            int tool = row + menu.toolScroll();
            int rowY = y + 55 + row * 44;
            if (tool == 0) g.renderOutline(x + 204, rowY - 2, 20, 20, 0xFF00D9EE);
            g.drawString(font, tool == 0 ? "*" : ">", x + 195, rowY + 4, 0xFF12506A, false);
            g.drawString(font, Integer.toString(tool + 1), x + 226, rowY + 4, 0xFF12506A, false);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR,
                x + 240, y + 48 + menu.toolScroll() * 144, 0, 0, 6, 28, 6, 28);
        g.drawCenteredString(font, menu.toolScroll() == 0 ? "1-4 / 5" : "2-5 / 5",
                x + 213, y + 230, 0xFF12506A);
    }

    // Read the original 1991 x 790 atlas; no resize or destructive image conversion.
    private void drawStatus(GuiGraphics g, int x, int y, int row, float fraction) {
        int[] frameY = {98, 324, 553};
        int[] iconY = {83, 316, 553};
        g.blit(RenderPipelines.GUI_TEXTURED, BARS, x, y - 1,
                38, iconY[row], 10, 10, 184, row == 2 ? 147 : 170, 1991, 790);
        int barX = x + 12;
        g.blit(RenderPipelines.GUI_TEXTURED, BARS, barX, y,
                238, frameY[row], 72, 8, 1704, 146, 1991, 790);
        // Keep the original frame and icon, clear only the coloured interior.
        g.fill(barX + 2, y + 2, barX + 70, y + 6, 0xFF072636);
        int fill = Mth.clamp(Math.round(68 * fraction), 0, 68);
        if (fill > 0) {
            g.enableScissor(barX + 2, y + 2, barX + 2 + fill, y + 6);
            g.blit(RenderPipelines.GUI_TEXTURED, BARS, barX, y,
                    238, frameY[row], 72, 8, 1704, 146, 1991, 790);
            g.disableScissor();
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Titles are already drawn in the supplied background.
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        String tip = null;
        if (isHovering(85, 44, 88, 12, mouseX, mouseY)) {
            tip = menu.skillLevel() >= 5 ? "Kinh nghiệm: đã đạt cấp tối đa (5)"
                    : "Kinh nghiệm: " + menu.experience() + " / " + menu.maxExperience();
        } else if (isHovering(85, 58, 88, 12, mouseX, mouseY)) {
            tip = "Máu: " + menu.health() + " / " + menu.maxHealth();
        } else if (isHovering(85, 72, 88, 12, mouseX, mouseY)) {
            tip = "Pin: " + menu.energy() * 100 / NovaWorkData.MAX_ENERGY
                    + "% — dùng redstone lên NOVA để sạc 20%";
        } else if (isHovering(239, 48, 9, 172, mouseX, mouseY)) {
            tip = "Cuộn hoặc kéo để xem ô công cụ thứ 5";
        }
        for (int row = 0; row < 4; row++) {
            if (isHovering(194, 53 + row * 44, 10, 20, mouseX, mouseY)) {
                tip = row + menu.toolScroll() == 0 ? "Công cụ đang cầm"
                        : "Đổi công cụ này với món đang cầm (ô 1)";
            }
        }
        if (tip != null) g.setTooltipForNextFrame(font, Component.literal(tip), mouseX, mouseY);
    }
    private boolean canScrollTools() { return !isQuickCrafting && menu.getCarried().isEmpty(); }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (isHovering(189, 30, 59, 191, x, y) && vertical != 0 && canScrollTools()) {
            menu.setToolScroll(vertical < 0 ? 1 : 0);
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    private void dragScrollbar(double mouseY) {
        double progress = (mouseY - topPos - 48 - 14) / 144.0;
        menu.setToolScroll(progress < 0.5 ? 0 : 1);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && canScrollTools()) {
            if (isHovering(239, 48, 9, 172, event.x(), event.y())) {
                draggingScrollbar = true;
                dragScrollbar(event.y());
                return true;
            }
            for (int row = 0; row < 4; row++) {
                int tool = row + menu.toolScroll();
                if (tool > 0 && isHovering(194, 53 + row * 44, 10, 20, event.x(), event.y())) {
                    if (minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, tool);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScrollbar && event.button() == 0) {
            dragScrollbar(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingScrollbar && event.button() == 0) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }
}
