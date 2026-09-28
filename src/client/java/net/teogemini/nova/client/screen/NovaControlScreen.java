package net.teogemini.nova.client.screen;

import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
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
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide() && hand == InteractionHand.MAIN_HAND
                    && !player.isSpectator() && !player.isShiftKeyDown()
                    && player.getMainHandItem().isEmpty()
                    && entity instanceof NovaEntity nova && nova.isAlive()
                    && nova.isTame() && nova.isOwnedBy(player)) {
                Minecraft.getInstance().setScreen(new NovaControlScreen(nova));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    private NovaEntity getNova() {
        if (minecraft.level == null || minecraft.player == null) {
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
        int y = height / 2 - 50;
        addCommand("Đi theo", NovaEntity.MODE_FOLLOW, x, y);
        addCommand("Tự do", NovaEntity.MODE_FREE, x, y + 25);
        addCommand("Đứng chờ", NovaEntity.MODE_WAIT, x, y + 50);
        addCommand(nova.isNovaSitRequested() ? "Đứng dậy" : "Ngồi xuống",
                nova.isNovaSitRequested() ? NovaControlPayload.STAND : NovaControlPayload.SIT,
                x, y + 75);
        addRenderableWidget(Button.builder(Component.literal("Đóng"), button -> onClose())
                .bounds(x, y + 105, 200, 20).build());
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
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 85, 0xFFFFFFFF);
        NovaEntity nova = getNova();
        if (nova != null) {
            String mode = switch (nova.getNovaMoveMode()) {
                case NovaEntity.MODE_FREE -> "Tự do";
                case NovaEntity.MODE_WAIT -> "Đứng chờ";
                default -> "Đi theo";
            };
            graphics.drawCenteredString(font,
                    mode + (nova.isNovaSitRequested() ? " • Đang ngồi" : ""),
                    width / 2, height / 2 - 68, 0xFF80E5FF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
