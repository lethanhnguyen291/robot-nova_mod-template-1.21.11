package net.teogemini.nova.ai;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.teogemini.nova.entity.NovaEntity;

/**
 * Hook AI giao tiếp cho NOVA.
 * Điểm nối để tích hợp các mô hình ngôn ngữ / AI đàm thoại trong tương lai.
 */
public final class NovaChatHandler {
    private NovaChatHandler() {}

    /**
     * Xử lý khi người chơi bấm nút "Trò chuyện" trên bảng điều khiển NOVA.
     * Người dùng có thể cắm file/kết nối AI (OpenAI API, Claude, Ollama, Gemini...) vào đây.
     *
     * @param nova Thực thể robot NOVA đang tương tác
     * @param player Người chơi là chủ sở hữu đang trò chuyện với NOVA
     */
    public static void handleChat(NovaEntity nova, ServerPlayer player) {
        // Biểu cảm thân thiện và cử chỉ gật đầu
        nova.showNovaFace(0, 60);
        nova.companion().gesture("nod");

        // TODO: Kết nối với mô hình AI giao tiếp tại đây trong tương lai.
        // Phản hồi mẫu hiện tại thông báo kết nối sẵn sàng:
        String message = "Xin chào " + player.getName().getString() + "! Mình là NOVA. Mô-đun AI trò chuyện đã sẵn sàng để liên kết.";
        player.displayClientMessage(Component.literal("NOVA: " + message), false);
    }
}

