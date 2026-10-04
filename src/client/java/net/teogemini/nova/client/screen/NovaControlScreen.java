package net.teogemini.nova.client.screen;

import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.network.NovaControlPayload;

public final class NovaControlScreen extends Screen {
    private final int entityId;
    private final UUID entityUuid;

    public NovaControlScreen(NovaEntity nova) {
        super(Component.literal("NOVA • Điều khiển"));
        entityId = nova.getId();
        entityUuid = nova.getUUID();
    }

    public static void registerInteraction() {
        ClientPlayNetworking.registerGlobalReceiver(net.teogemini.nova.network.NovaRodControl.Open.TYPE,
                (packet, context) -> {
                    var client = context.client();
                    if (client.screen != null || client.level == null || client.player == null
                            || client.player.isSpectator() || !client.player.isAlive()
                            || !net.teogemini.nova.network.NovaRodControl.holdingRod(client.player)
                            || !(client.level.getEntity(packet.entityId()) instanceof NovaEntity nova)
                            || !nova.getUUID().equals(packet.uuid()) || !nova.isAlive()
                            || !nova.isTame() || !nova.isOwnedBy(client.player)
                            || client.player.distanceToSqr(nova) > 64) return;
                    client.setScreen(new NovaControlScreen(nova));
                });
    }

    private NovaEntity getNova() {
        if (minecraft.level == null || minecraft.player == null
                || !net.teogemini.nova.network.NovaRodControl.holdingRod(minecraft.player)) {
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

    @Override
    protected void init() {
        NovaEntity nova = getNova();
        if (nova == null) {
            onClose();
            return;
        }
        int x = width / 2 - 100;
        int y = height / 2 - 70;
        addCommand("Đi theo", NovaEntity.MODE_FOLLOW, x, y);
        addCommand("Tự do", NovaEntity.MODE_FREE, x, y + 25);
        addCommand("Đứng chờ", NovaEntity.MODE_WAIT, x, y + 50);
        addCommand(nova.isNovaSitRequested() ? "Đứng dậy" : "Ngồi xuống",
                nova.isNovaSitRequested() ? NovaControlPayload.STAND : NovaControlPayload.SIT,
                x, y + 75);
        addRenderableWidget(Button.builder(Component.literal("Tính cách & Kỷ niệm"), button -> {
            if (getNova()!=null) ClientPlayNetworking.send(
                    new net.teogemini.nova.network.NovaCompanionCommands.Request(entityId,entityUuid,0));
        }).bounds(x,y+100,200,20).build());
        addRenderableWidget(Button.builder(Component.literal("Đóng"), button -> onClose())
                .bounds(x, y + 125, 200, 20).build());
    }

    private void addCommand(String label, int action, int x, int y) {
        addRenderableWidget(Button.builder(Component.literal(label), button -> {
            if (getNova() != null) {
                ClientPlayNetworking.send(new NovaControlPayload(entityId, entityUuid, action));
            }
            onClose();
        }).bounds(x, y, 200, 20).build());
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
            graphics.drawCenteredString(font,
                    mode + (nova.isNovaSitRequested() ? " • Đang ngồi" : ""),
                    width / 2, height / 2 - 88, 0xFF80E5FF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
