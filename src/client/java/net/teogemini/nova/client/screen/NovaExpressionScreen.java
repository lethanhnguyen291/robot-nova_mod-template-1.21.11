package net.teogemini.nova.client.screen;

import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.network.NovaControlPayload;
import net.teogemini.nova.network.NovaRodControl;

/**
 * Màn hình chọn biểu cảm trực quan cho NOVA.
 * Không bị che tầm nhìn ở giữa màn hình để người chơi quan sát trực tiếp mặt robot.
 */
public final class NovaExpressionScreen extends Screen {
    private final int entityId;
    private final UUID entityUuid;

    public NovaExpressionScreen(NovaEntity nova) {
        super(Component.literal("NOVA • Biểu Cảm"));
        this.entityId = nova.getId();
        this.entityUuid = nova.getUUID();
    }

    private NovaEntity getNova() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null
                || !NovaRodControl.holdingRod(minecraft.player)) {
            return null;
        }
        if (minecraft.level.getEntity(entityId) instanceof NovaEntity nova
                && nova.getUUID().equals(entityUuid) && nova.isAlive()
                && nova.isOwnedBy(minecraft.player)
                && minecraft.player.distanceToSqr(nova) <= 64.0) {
            return nova;
        }
        return null;
    }

    private void sendAction(int action) {
        if (getNova() != null) {
            ClientPlayNetworking.send(new NovaControlPayload(entityId, entityUuid, action));
        }
    }

    @Override
    protected void init() {
        NovaEntity nova = getNova();
        if (nova == null) {
            onClose();
            return;
        }

        int btnH = 20;
        int y = height - 36;
        int startX = (width - 337) / 2;

        // 1. Nút "◄ Trước" (nhỏ gọn)
        addRenderableWidget(Button.builder(Component.literal("◄ Trước"), b -> {
            sendAction(NovaControlPayload.FACE_PREV);
        }).bounds(startX, y, 60, btnH).build());

        // 2. Nút "Tiếp ►" (nhỏ gọn)
        addRenderableWidget(Button.builder(Component.literal("Tiếp ►"), b -> {
            sendAction(NovaControlPayload.FACE_NEXT);
        }).bounds(startX + 66, y, 60, btnH).build());

        // 3. Nút "Đặt lại" (kiểu nút reset như mẫu)
        addRenderableWidget(Button.builder(Component.literal("Đặt lại"), b -> {
            sendAction(NovaControlPayload.FACE_RESET);
        }).bounds(startX + 136, y, 60, btnH).build());

        // 4. Nút "Quay lại" (trở lại bảng điều khiển chính)
        addRenderableWidget(Button.builder(Component.literal("Quay lại"), b -> {
            NovaEntity n = getNova();
            if (n != null) {
                minecraft.setScreen(new NovaControlScreen(n));
            } else {
                onClose();
            }
        }).bounds(startX + 206, y, 70, btnH).build());

        // 5. Nút "Đóng" (thoát ra để ngắm robot tự do)
        addRenderableWidget(Button.builder(Component.literal("Đóng"), b -> onClose())
                .bounds(startX + 282, y, 55, btnH).build());
    }

    @Override
    public void tick() {
        if (getNova() == null) {
            onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Không làm tối mờ màn hình để người chơi nhìn rõ toàn bộ robot NOVA ở ngoài thế giới 3D!
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Vẽ dải nền tối bán trong suốt nhỏ gọn ở sát đáy màn hình để các nút nổi bật
        graphics.fill(width / 2 - 180, height - 58, width / 2 + 180, height - 10, 0x90000000);

        super.render(graphics, mouseX, mouseY, partialTick);

        NovaEntity nova = getNova();
        if (nova != null) {
            int face = nova.getNovaFace();
            String text = "Biểu cảm: " + face + " / 39" + (face == 0 ? " (Mặc định)" : "");
            graphics.drawCenteredString(font, text, width / 2, height - 51, 0xFF80E5FF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
