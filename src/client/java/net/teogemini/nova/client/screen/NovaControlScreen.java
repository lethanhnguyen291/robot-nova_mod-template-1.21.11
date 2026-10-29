package net.teogemini.nova.client.screen;

import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.network.NovaCompanionCommands;
import net.teogemini.nova.network.NovaControlPayload;
import net.teogemini.nova.network.NovaRodControl;

public final class NovaControlScreen extends Screen {
    private enum SubMenu {
        NONE,
        COMMANDS,
        ACTIONS
    }

    private final int entityId;
    private final UUID entityUuid;
    private SubMenu currentSubMenu = SubMenu.NONE;

    public NovaControlScreen(NovaEntity nova) {
        super(Component.literal("NOVA • Bảng Điều Khiển"));
        entityId = nova.getId();
        entityUuid = nova.getUUID();
    }

    public static void registerInteraction() {
        ClientPlayNetworking.registerGlobalReceiver(NovaRodControl.Open.TYPE,
                (packet, context) -> {
                    var client = context.client();
                    if (client.screen != null || client.level == null || client.player == null
                            || client.player.isSpectator() || !client.player.isAlive()
                            || !NovaRodControl.holdingRod(client.player)
                            || !(client.level.getEntity(packet.entityId()) instanceof NovaEntity nova)
                            || !nova.getUUID().equals(packet.uuid()) || !nova.isAlive()
                            || !nova.isTame() || !nova.isOwnedBy(client.player)
                            || client.player.distanceToSqr(nova) > 64) return;
                    client.setScreen(new NovaControlScreen(nova));
                });
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

    private void switchSubMenu(SubMenu menu) {
        if (this.currentSubMenu == menu) {
            this.currentSubMenu = SubMenu.NONE;
        } else {
            this.currentSubMenu = menu;
        }
        clearWidgets();
        init();
    }

    private void sendAction(int action, boolean closeScreen) {
        if (getNova() != null) {
            ClientPlayNetworking.send(new NovaControlPayload(entityId, entityUuid, action));
        }
        if (closeScreen) {
            onClose();
        }
    }

    @Override
    protected void init() {
        NovaEntity nova = getNova();
        if (nova == null) {
            onClose();
            return;
        }

        // Kích thước các nút chính bằng kích thước cũ (200x20) và nằm chính giữa màn hình
        int mainX = width / 2 - 100;
        int mainW = 200;
        int btnH = 20;
        int startY = height / 2 - 76;
        int gap = 24;

        // Kích thước các nút lựa chọn nhỏ nằm bên phải nút chính (kiểu nút nhỏ như mẫu hình ảnh)
        int subX = width / 2 + 105;
        int subW = 68;

        // 1. Nút "Ra lệnh" (ở vị trí nút Đi theo cũ)
        String cmdArrow = currentSubMenu == SubMenu.COMMANDS ? " ◀" : "";
        addRenderableWidget(Button.builder(Component.literal("Ra lệnh" + cmdArrow),
                b -> switchSubMenu(SubMenu.COMMANDS))
                .bounds(mainX, startY, mainW, btnH).build());

        // 2. Nút "Giao việc" (Thay ngọc lục bảo)
        boolean isWorking = nova.isNovaWorkEnabled();
        String workLabel = isWorking ? "Giao việc: [Bật]" : "Giao việc: [Tắt]";
        addRenderableWidget(Button.builder(Component.literal(workLabel), b -> {
            sendAction(NovaControlPayload.TOGGLE_WORK, true);
        }).bounds(mainX, startY + gap, mainW, btnH).build());

        // 3. Nút "Biểu cảm" (Mở màn hình xem trực tiếp không bị che robot)
        addRenderableWidget(Button.builder(Component.literal("Biểu cảm "), b -> {
            NovaEntity n = getNova();
            if (n != null) {
                minecraft.setScreen(new NovaExpressionScreen(n));
            }
        }).bounds(mainX, startY + gap * 2, mainW, btnH).build());

        // 4. Nút "Hành động" (Gộp chào, ăn mừng, xin lỗi, gật đầu, tự hào)
        String actArrow = currentSubMenu == SubMenu.ACTIONS ? " ◀" : " ";
        addRenderableWidget(Button.builder(Component.literal("Hành động" + actArrow),
                b -> switchSubMenu(SubMenu.ACTIONS))
                .bounds(mainX, startY + gap * 3, mainW, btnH).build());

        // 5. Nút "Trò chuyện" (Liên kết AI giao tiếp)
        addRenderableWidget(Button.builder(Component.literal("Trò chuyện"), b -> {
            sendAction(NovaControlPayload.CHAT, true);
        }).bounds(mainX, startY + gap * 4, mainW, btnH).build());

        // 6. Nút "Tính cách & Kỷ niệm"
        addRenderableWidget(Button.builder(Component.literal("Tính cách & Kỷ niệm"), b -> {
            if (getNova() != null) {
                ClientPlayNetworking.send(new NovaCompanionCommands.Request(entityId, entityUuid, 0));
            }
        }).bounds(mainX, startY + gap * 5, mainW, btnH).build());

        // 7. Nút "Đóng"
        addRenderableWidget(Button.builder(Component.literal("Đóng"), b -> onClose())
                .bounds(mainX, startY + gap * 6, mainW, btnH).build());

        // Các nút lựa chọn nhỏ bên cạnh cột nút chính
        if (currentSubMenu == SubMenu.COMMANDS) {
            int currentMode = nova.getNovaMoveMode();
            addRenderableWidget(Button.builder(
                    Component.literal((currentMode == NovaEntity.MODE_FOLLOW ? "✔ " : "") + "Đi theo"),
                    b -> sendAction(NovaEntity.MODE_FOLLOW, true))
                    .bounds(subX, startY, subW, btnH).build());

            addRenderableWidget(Button.builder(
                    Component.literal((currentMode == NovaEntity.MODE_FREE ? "✔ " : "") + "Tự do"),
                    b -> sendAction(NovaEntity.MODE_FREE, true))
                    .bounds(subX, startY + gap, subW, btnH).build());

            addRenderableWidget(Button.builder(
                    Component.literal((currentMode == NovaEntity.MODE_WAIT ? "✔ " : "") + "Đứng chờ"),
                    b -> sendAction(NovaEntity.MODE_WAIT, true))
                    .bounds(subX, startY + gap * 2, subW, btnH).build());

            boolean isSitting = nova.isNovaSitRequested();
            addRenderableWidget(Button.builder(
                    Component.literal(isSitting ? "Đứng dậy" : "Ngồi"),
                    b -> sendAction(isSitting ? NovaControlPayload.STAND : NovaControlPayload.SIT, true))
                    .bounds(subX, startY + gap * 3, subW, btnH).build());
        } else if (currentSubMenu == SubMenu.ACTIONS) {
            addRenderableWidget(Button.builder(Component.literal("Chào"),
                    b -> sendAction(NovaControlPayload.ACTION_WAVE, true))
                    .bounds(subX, startY + gap * 2, subW, btnH).build());

            addRenderableWidget(Button.builder(Component.literal("Ăn mừng"),
                    b -> sendAction(NovaControlPayload.ACTION_CELEBRATE, true))
                    .bounds(subX, startY + gap * 3, subW, btnH).build());

            addRenderableWidget(Button.builder(Component.literal("Xin lỗi"),
                    b -> sendAction(NovaControlPayload.ACTION_APOLOGIZE, true))
                    .bounds(subX, startY + gap * 4, subW, btnH).build());

            addRenderableWidget(Button.builder(Component.literal("Gật đầu"),
                    b -> sendAction(NovaControlPayload.ACTION_NOD, true))
                    .bounds(subX, startY + gap * 5, subW, btnH).build());

            addRenderableWidget(Button.builder(Component.literal("Tự hào"),
                    b -> sendAction(NovaControlPayload.ACTION_PROUD, true))
                    .bounds(subX, startY + gap * 6, subW, btnH).build());
        }
    }

    @Override
    public void tick() {
        if (getNova() == null) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 105, 0xFFFFFFFF);
        NovaEntity nova = getNova();
        if (nova != null) {
            String mode = switch (nova.getNovaMoveMode()) {
                case NovaEntity.MODE_FREE -> "Tự do";
                case NovaEntity.MODE_WAIT -> "Đứng chờ";
                default -> "Đi theo";
            };
            String status = mode + (nova.isNovaSitRequested() ? " • Đang ngồi" : "")
                    + (nova.isNovaWorkEnabled() ? " • Đang làm việc" : "");
            graphics.drawCenteredString(font, status, width / 2, height / 2 - 90, 0xFF80E5FF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
