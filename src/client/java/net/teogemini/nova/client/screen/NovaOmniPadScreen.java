package net.teogemini.nova.client.screen;

import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.network.NovaOmniPadNetwork;
import net.teogemini.nova.network.NovaOmniPadNetwork.ActionPayload;
import net.teogemini.nova.network.NovaOmniPadNetwork.RobotInfo;
import net.teogemini.nova.network.NovaOmniPadNetwork.SyncPayload;
import org.lwjgl.glfw.GLFW;

public final class NovaOmniPadScreen extends Screen {
    private static final Identifier FRAME_TEXTURE = ROBOTNOVA_MOD.id("textures/gui/nova_omni_pad_frame.png");
    private static final int CARDS_PER_PAGE = 2;
    private static final int SQUAD_ROWS_VISIBLE = 5;

    private enum Tab {
        COMMAND_CENTER,
        SQUAD_CONTROL,
        GUIDE_WIKI,
        ABOUT_MOD
    }

    public enum BtnTheme {
        TAB_ACTIVE(0xEE0B3E63, 0xFF0E4D78, 0xFF00F2FF, 0xFF00FFFF),
        TAB_IDLE(0xBB071D33, 0xDD0D3559, 0xFF4F5BD5, 0xFFB8E2FF),
        CYAN(0xCC07243E, 0xEE0D3E66, 0xFF00D9FF, 0xFFE2F9FF),
        PURPLE(0xCC17184B, 0xEE282A75, 0xFF8B5CF6, 0xFFEDE9FE),
        EMERALD(0xCC062E2C, 0xEE0B4F4A, 0xFF00F5D4, 0xFFD8F3DC),
        AMBER(0xCC331520, 0xEE521E30, 0xFFFF6B6B, 0xFFFFE5EC);

        final int bgNormal;
        final int bgHover;
        final int border;
        final int textColor;

        BtnTheme(int bgNormal, int bgHover, int border, int textColor) {
            this.bgNormal = bgNormal;
            this.bgHover = bgHover;
            this.border = border;
            this.textColor = textColor;
        }
    }

    private final class HoloButton extends Button {
        private final BtnTheme theme;

        HoloButton(int x, int y, int width, int height, Component message, OnPress onPress, BtnTheme theme) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.theme = theme;
        }

        @Override
        protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean hov = isHoveredOrFocused() && active;
            int bg = !active && theme != BtnTheme.TAB_ACTIVE ? 0x77051322 : (hov ? theme.bgHover : theme.bgNormal);
            int brd = !active && theme != BtnTheme.TAB_ACTIVE ? 0xFF234560 : (hov ? 0xFF00FFFF : theme.border);
            int txtCol = !active && theme != BtnTheme.TAB_ACTIVE ? 0xFF5C829E : (hov ? 0xFFFFFFFF : theme.textColor);

            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            g.fill(x, y, x + w, y + h, bg);
            g.renderOutline(x, y, w, h, brd);

            // Tech corner highlights
            int accent = hov ? 0xFFFFFFFF : 0xFF00F2FF;
            g.fill(x, y, x + 4, y + 1, accent);
            g.fill(x, y, x + 1, y + 4, accent);
            g.fill(x + w - 4, y + h - 1, x + w, y + h, accent);
            g.fill(x + w - 1, y + h - 4, x + w, y + h, accent);

            if (theme == BtnTheme.TAB_ACTIVE) {
                g.fill(x + 2, y + h - 2, x + w - 2, y + h, 0xFF00F2FF);
            }

            // Auto-scale text so it never truncates or overflows
            String text = getMessage().getString();
            int textW = font.width(text);
            float maxW = Math.max(12, w - 6);
            float scale = textW > maxW ? (maxW / textW) : 1.0f;

            g.pose().pushMatrix();
            float cx = x + w / 2.0f;
            float cy = y + (h - 8 * scale) / 2.0f;
            g.pose().translate(cx, cy);
            g.pose().scale(scale, scale);
            g.drawString(font, text, Math.round(-textW / 2.0f), 0, txtCol, false);
            g.pose().popMatrix();
        }
    }

    private Tab currentTab = Tab.COMMAND_CENTER;
    private List<RobotInfo> robots = new ArrayList<>();
    private final Set<UUID> selectedUuids = new LinkedHashSet<>();
    private int reserveEnergyPercent = 200;
    private int inventoryRedstonePercent = 0;
    private boolean lockdownActive = false;
    private String feedbackBanner = "";
    private int feedbackTimer = 0;

    // Tab 1 state
    private int cardPage = 0;
    private UUID renamingUuid = null;
    private String renamingCurrentName = "";
    private EditBox renameInput = null;

    // Tab 2 state
    private int squadScroll = 0;

    // Tab 3 & 4 state
    private int guideSubTab = 0;
    private int recipeIndex = 0;
    private final List<NovaOmniPadGuideRenderer.HoloScrollBox> guideScrollBoxes = List.of(
            new NovaOmniPadGuideRenderer.HoloScrollBox(),
            new NovaOmniPadGuideRenderer.HoloScrollBox(),
            new NovaOmniPadGuideRenderer.HoloScrollBox(),
            new NovaOmniPadGuideRenderer.HoloScrollBox(),
            new NovaOmniPadGuideRenderer.HoloScrollBox(),
            new NovaOmniPadGuideRenderer.HoloScrollBox(),
            new NovaOmniPadGuideRenderer.HoloScrollBox()
    );

    // Panel geometry
    private int panelX, panelY, panelW, panelH;
    private int contentX, contentY, contentW, contentH;
    private int autoRefreshTicker = 0;

    public NovaOmniPadScreen(SyncPayload initial) {
        super(Component.literal("NOVA OMNI-PAD"));
        applySync(initial);
        for (RobotInfo r : robots) {
            if (!r.dormant()) selectedUuids.add(r.uuid());
        }
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, context) -> {
            var client = context.client();
            if (client.level == null || client.player == null) return;
            if (client.screen instanceof NovaOmniPadScreen padScreen) {
                padScreen.applySync(payload);
                padScreen.rebuildWidgets();
            } else if (payload.openScreen()) {
                client.setScreen(new NovaOmniPadScreen(payload));
            }
        });
    }

    private void applySync(SyncPayload payload) {
        this.robots = new ArrayList<>(payload.robots());
        this.reserveEnergyPercent = payload.reserveEnergyPercent();
        this.inventoryRedstonePercent = payload.inventoryRedstonePercent();
        this.lockdownActive = payload.lockdownActive();
        if (payload.feedback() != null && !payload.feedback().isBlank()) {
            this.feedbackBanner = payload.feedback();
            this.feedbackTimer = 110;
        }
        Set<UUID> valid = new HashSet<>();
        for (RobotInfo r : robots) valid.add(r.uuid());
        selectedUuids.retainAll(valid);
        int maxPage = Math.max(0, (robots.size() - 1) / CARDS_PER_PAGE);
        cardPage = Mth.clamp(cardPage, 0, maxPage);
    }

    private void sendAction(int action, List<UUID> targets, String text) {
        ClientPlayNetworking.send(new ActionPayload(action, targets, text == null ? "" : text));
    }

    private void sendSingleAction(int action, UUID target) {
        sendAction(action, List.of(target), "");
    }

    private void sendMassAction(int action) {
        List<UUID> targets = selectedUuids.isEmpty()
                ? robots.stream().map(RobotInfo::uuid).toList()
                : new ArrayList<>(selectedUuids);
        sendAction(action, targets, "");
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        this.renamingUuid = null;
        rebuildWidgets();
    }

    private HoloButton addHoloBtn(int x, int y, int w, int h, String label, BtnTheme theme,
                                  Button.OnPress onPress, String tooltipText) {
        HoloButton btn = addRenderableWidget(new HoloButton(x, y, w, h, Component.literal(label), onPress, theme));
        if (tooltipText != null && !tooltipText.isEmpty()) {
            btn.setTooltip(Tooltip.create(Component.literal(tooltipText)));
        }
        return btn;
    }

    @Override
    protected void init() {
        // Fill virtually the entire screen so the frame is large, clear, and spacious
        panelW = width - 2;
        panelH = height - 2;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        // Safe interior bounds strictly inside the frame (above the bottom V-wing peak)
        int navY = panelY + Math.round(panelH * 0.102f);
        contentX = panelX + Math.round(panelW * 0.044f);
        contentW = panelW - Math.round(panelW * 0.088f);
        contentY = navY + 19;
        int bottomSafeY = panelY + Math.round(panelH * 0.742f);
        contentH = Math.max(100, bottomSafeY - contentY);

        // Top Navigation Bar (4 Sci-Fi HoloButton Tabs + Refresh & Close)
        int navTotalW = contentW - 42;
        int tabW = (navTotalW - 9) / 4;

        addTabBtn("TRUNG TÂM CHỈ HUY", Tab.COMMAND_CENTER, contentX, navY, tabW);
        addTabBtn("ĐIỀU KHIỂN ĐỘI HÌNH", Tab.SQUAD_CONTROL, contentX + (tabW + 3), navY, tabW);
        addTabBtn("HƯỚNG DẪN SỬ DỤNG", Tab.GUIDE_WIKI, contentX + (tabW + 3) * 2, navY, tabW);
        addTabBtn("GIỚI THIỆU MOD", Tab.ABOUT_MOD, contentX + (tabW + 3) * 3, navY, tabW);

        addHoloBtn(contentX + contentW - 38, navY, 18, 16, "⟳", BtnTheme.CYAN,
                b -> sendAction(NovaOmniPadNetwork.ACTION_REFRESH, List.of(), ""),
                "Làm mới dữ liệu mạng lưới NOVA");

        addHoloBtn(contentX + contentW - 18, navY, 18, 16, "✕", BtnTheme.AMBER,
                b -> onClose(),
                "Đóng Bảng Điều Khiển (ESC)");

        switch (currentTab) {
            case COMMAND_CENTER -> initCommandCenterTab(contentX, contentY, contentW, contentH);
            case SQUAD_CONTROL -> initSquadControlTab(contentX, contentY, contentW, contentH);
            case GUIDE_WIKI -> initGuideWikiTab(contentX, contentY, contentW, contentH);
            case ABOUT_MOD -> {}
        }
    }

    private void addTabBtn(String label, Tab tab, int x, int y, int w) {
        boolean active = currentTab == tab;
        HoloButton btn = addHoloBtn(x, y, w, 16, (active ? "◈ " : "") + label,
                active ? BtnTheme.TAB_ACTIVE : BtnTheme.TAB_IDLE,
                b -> switchTab(tab), null);
        btn.active = !active;
    }

    private void initCommandCenterTab(int x, int y, int w, int h) {
        if (renamingUuid != null) {
            String draft = renameInput != null ? renameInput.getValue() : renamingCurrentName;
            renameInput = new EditBox(font, x + 76, y + 1, w - 178, 16, Component.literal("Tên NOVA"));
            renameInput.setMaxLength(32);
            renameInput.setHint(Component.literal("Nhập tên mới (VD: Nova Tiều Phu, Nova Vệ Sĩ)..."));
            renameInput.setValue(draft);
            addRenderableWidget(renameInput);
            setInitialFocus(renameInput);

            UUID targetId = renamingUuid;
            addHoloBtn(x + w - 98, y + 1, 52, 16, "✔ Lưu Tên", BtnTheme.EMERALD, b -> {
                sendAction(NovaOmniPadNetwork.ACTION_RENAME, List.of(targetId), renameInput.getValue());
                renamingUuid = null;
                renameInput = null;
            }, "Lưu tên mới lên đầu Robot NOVA");

            addHoloBtn(x + w - 43, y + 1, 43, 16, "Hủy", BtnTheme.AMBER, b -> {
                renamingUuid = null;
                renameInput = null;
                rebuildWidgets();
            }, null);
        } else {
            int utilW = (w - 6) / 3;
            addHoloBtn(x, y, utilW, 16,
                    "⚡ Nạp Pin Pad (" + reserveEnergyPercent + "%)",
                    BtnTheme.EMERALD,
                    b -> sendAction(NovaOmniPadNetwork.ACTION_LOAD_PAD_ENERGY, List.of(), ""),
                    "Nạp Bụi Đá Đỏ (+20%) hoặc Khối Đá Đỏ (+100%) từ túi đồ vào khe dự phòng của Omni-Pad.\n"
                    + "Đá đỏ trong túi bạn hiện có: +" + inventoryRedstonePercent + "%");

            addHoloBtn(x + utilW + 3, y, utilW, 16,
                    "✦ Tự Động Phân Công",
                    BtnTheme.PURPLE,
                    b -> sendAction(NovaOmniPadNetwork.MASS_SMART_ASSIGN, List.of(), ""),
                    "Tự động kiểm tra công cụ trong bụng từng NOVA để giao việc:\n"
                    + "Rìu → Chặt cây | Cúp → Đào mỏ | Kiếm → Vệ sĩ | Cuốc → Làm ruộng | Xẻng → Đào đất");

            String lockLabel = lockdownActive ? "🔓 Mở Khóa An Toàn" : "🔒 Khóa An Toàn Toàn Cục";
            addHoloBtn(x + (utilW + 3) * 2, y, w - (utilW + 3) * 2, 16,
                    lockLabel,
                    lockdownActive ? BtnTheme.EMERALD : BtnTheme.AMBER,
                    b -> sendAction(NovaOmniPadNetwork.MASS_LOCKDOWN_TOGGLE, List.of(), ""),
                    "Chế Độ Ngủ Đông Toàn Cục (Emergency Lockdown):\n"
                    + "Toàn bộ NOVA ngừng việc ngay lập tức và bật giáp phòng thủ cấp III!");
        }

        // 2 Spacious NOVA Cards strictly above the bottom V-wing peak
        int startIdx = cardPage * CARDS_PER_PAGE;
        int cardYStart = y + 19;
        int cardH = (h - 21) / 2;

        for (int i = 0; i < CARDS_PER_PAGE; i++) {
            int idx = startIdx + i;
            if (idx >= robots.size()) break;
            RobotInfo r = robots.get(idx);
            int cy = cardYStart + i * (cardH + 3);

            boolean unnamed = r.customName() == null || r.customName().isBlank();
            String renameLabel = unnamed ? "✎ Đặt Tên" : "✎ Đổi Tên";

            int btnW = 62;
            int col2 = x + w - btnW - 4;
            int col1 = col2 - btnW - 3;
            int col0 = col1 - 54 - 3;

            addHoloBtn(col0, cy + 4, 54, 15, renameLabel, BtnTheme.PURPLE, b -> {
                this.renamingUuid = r.uuid();
                this.renamingCurrentName = unnamed ? "" : r.customName();
                this.renameInput = null;
                rebuildWidgets();
            }, "Đặt tên hoặc đổi tên trực tiếp không cần Thẻ Tên hay Đe Sắt!");

            addHoloBtn(col1, cy + 4, btnW, 15, "✦ Dịch Chuyển", BtnTheme.CYAN,
                    b -> sendSingleAction(NovaOmniPadNetwork.ACTION_TELEPORT_ONE, r.uuid()),
                    "Dịch Chuyển Lượng Tử: Triệu hồi tức thời NOVA này về đứng trước mặt bạn!");

            addHoloBtn(col2, cy + 4, btnW, 15, "◎ Định Vị", BtnTheme.PURPLE,
                    b -> sendSingleAction(NovaOmniPadNetwork.ACTION_PING_ONE, r.uuid()),
                    "Radar Ping: Phát sáng viền xanh xuyên tường trong 15 giây!");

            addHoloBtn(col1, cy + cardH - 19, btnW, 15, "▤ Mở Kho Đồ", BtnTheme.CYAN,
                    b -> sendSingleAction(NovaOmniPadNetwork.ACTION_OPEN_INVENTORY, r.uuid()),
                    "Mở kho đồ không dây từ xa để lấy gỗ/quặng hoặc đổi công cụ!");

            addHoloBtn(col2, cy + cardH - 19, btnW, 15, "⚡ Truyền Điện", BtnTheme.EMERALD,
                    b -> sendSingleAction(NovaOmniPadNetwork.ACTION_CHARGE_ONE, r.uuid()),
                    "Truyền điện không dây từ Omni-Pad / Đá đỏ để sạc đầy pin & hồi máu!");
        }

        // Pagination buttons placed neatly in the Left & Right Lower Bays (beside the center V-wing!)
        int totalPages = Math.max(1, (robots.size() + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE);
        int bayY = panelY + Math.round(panelH * 0.755f);
        int bayLeftX = panelX + Math.round(panelW * 0.105f);
        int bayRightX = panelX + panelW - Math.round(panelW * 0.105f) - 76;

        HoloButton prevBtn = addHoloBtn(bayLeftX, bayY, 76, 15, "◀ Trang trước", BtnTheme.CYAN, b -> {
            if (cardPage > 0) {
                cardPage--;
                rebuildWidgets();
            }
        }, null);
        prevBtn.active = cardPage > 0;

        HoloButton nextBtn = addHoloBtn(bayRightX, bayY, 76, 15, "Trang tiếp ▶", BtnTheme.CYAN, b -> {
            if (cardPage + 1 < totalPages) {
                cardPage++;
                rebuildWidgets();
            }
        }, null);
        nextBtn.active = cardPage + 1 < totalPages;
    }

    private void initSquadControlTab(int x, int y, int w, int h) {
        int leftW = (w * 43) / 100;
        int rightX = x + leftW + 8;
        int rightW = w - leftW - 8;

        int selBtnW = (leftW - 6) / 4;
        addHoloBtn(x, y, selBtnW, 15, "Tất Cả", BtnTheme.CYAN, b -> {
            for (RobotInfo r : robots) selectedUuids.add(r.uuid());
        }, "Chọn tất cả NOVA");

        addHoloBtn(x + selBtnW + 2, y, selBtnW, 15, "Rảnh", BtnTheme.CYAN, b -> {
            selectedUuids.clear();
            for (RobotInfo r : robots) {
                if (!r.working() && !r.dormant()) selectedUuids.add(r.uuid());
            }
        }, "Chỉ chọn những NOVA đang rảnh rỗi");

        addHoloBtn(x + (selBtnW + 2) * 2, y, selBtnW + 2, 15, "Pin Yếu", BtnTheme.AMBER, b -> {
            selectedUuids.clear();
            for (RobotInfo r : robots) {
                if (r.lowBattery() || r.dormant()) selectedUuids.add(r.uuid());
            }
        }, "Chọn những NOVA đang yếu pin (<15%)");

        addHoloBtn(x + (selBtnW + 2) * 3 + 2, y, leftW - ((selBtnW + 2) * 3 + 2), 15, "Bỏ Chọn", BtnTheme.PURPLE,
                b -> selectedUuids.clear(), "Bỏ chọn toàn bộ");

        int bw = (rightW - 4) / 2;
        int btnH = 14;
        int stepY = 16;

        // Work Commands (Header at y + 0, buttons at y + 10, 26, 42)
        int ry = y + 10;
        addHoloBtn(rightX, ry, bw, btnH, "🪓 Đi Chặt Cây", BtnTheme.CYAN,
                b -> sendMassAction(NovaOmniPadNetwork.MASS_WORK_CHOP),
                "Ghi đè lệnh & giao việc Chặt Cây đồng loạt");
        addHoloBtn(rightX + bw + 4, ry, bw, btnH, "⛏ Đi Đào Mỏ", BtnTheme.CYAN,
                b -> sendMassAction(NovaOmniPadNetwork.MASS_WORK_MINE),
                "Ghi đè lệnh & giao việc Đào Mỏ đồng loạt");

        ry += stepY;
        addHoloBtn(rightX, ry, bw, btnH, "🌾 Đi Làm Ruộng", BtnTheme.CYAN,
                b -> sendMassAction(NovaOmniPadNetwork.MASS_WORK_FARM),
                "Ghi đè lệnh & giao việc Làm Ruộng đồng loạt");
        addHoloBtn(rightX + bw + 4, ry, bw, btnH, "🛠 Dọn Đường / Đào", BtnTheme.CYAN,
                b -> sendMassAction(NovaOmniPadNetwork.MASS_WORK_DIG),
                "Ghi đè lệnh & giao việc Đào Đất / Dọn Đường đồng loạt");

        ry += stepY;
        addHoloBtn(rightX, ry, bw, btnH, "▶ Bật Giao Việc", BtnTheme.EMERALD,
                b -> sendMassAction(NovaOmniPadNetwork.MASS_WORK_ON),
                "Bật làm việc theo công cụ đang cầm của từng NOVA");
        addHoloBtn(rightX + bw + 4, ry, bw, btnH, "■ Tắt Làm Việc", BtnTheme.AMBER,
                b -> sendMassAction(NovaOmniPadNetwork.MASS_WORK_OFF),
                "Dừng công việc hiện tại của nhóm đã chọn");

        // Movement & Formation Commands (Header at y + 60, buttons at y + 70, 86)
        ry = y + 70;
        addHoloBtn(rightX, ry, bw, btnH, "⚑ Tập Hợp (Rally)", BtnTheme.PURPLE,
                b -> sendMassAction(NovaOmniPadNetwork.MASS_RALLY),
                "Tập hợp toàn bộ đội hình đã chọn xếp hàng quanh chủ nhân");
        addHoloBtn(rightX + bw + 4, ry, bw, btnH, "🛡 Đi Theo Bảo Vệ", BtnTheme.PURPLE,
                b -> sendMassAction(NovaOmniPadNetwork.MASS_FOLLOW),
                "Ra lệnh cho cả nhóm đi theo hộ tống chủ nhân");

        ry += stepY;
        addHoloBtn(rightX, ry, bw, btnH, "⏸ Đứng Chờ (Wait)", BtnTheme.CYAN,
                b -> sendMassAction(NovaOmniPadNetwork.MASS_WAIT),
                "Ra lệnh cho cả nhóm đứng yên tại vị trí hiện tại");
        addHoloBtn(rightX + bw + 4, ry, bw, btnH, "📦 Về Rương Nghỉ", BtnTheme.CYAN,
                b -> sendMassAction(NovaOmniPadNetwork.MASS_RETURN_CHEST),
                "Ra lệnh cho nhóm quay về Rương NOVA liên kết và nghỉ ngơi");

        // Super Squad Commands (Header at y + 104, buttons shifted down to y + 115 so they never cover the title!)
        ry = y + 115;
        addHoloBtn(rightX, ry, bw, 15, "✦ Triệu Hồi Đội Hình", BtnTheme.EMERALD,
                b -> sendMassAction(NovaOmniPadNetwork.MASS_TELEPORT),
                "Dịch chuyển lượng tử toàn bộ nhóm đã chọn về ngay trước mặt bạn!");
        addHoloBtn(rightX + bw + 4, ry, bw, 15, "⚡ Sạc Pin Đội Hình", BtnTheme.EMERALD,
                b -> sendMassAction(NovaOmniPadNetwork.MASS_WIRELESS_CHARGE),
                "Sạc pin không dây từ xa cho toàn bộ nhóm đã chọn!");
    }

    private void initGuideWikiTab(int x, int y, int w, int h) {
        int subW = (w - 9) / 4;
        String[] labels = {"1. Khoáng Sản", "2. Công Thức (14)", "3. Lắp Ráp 3 Khối", "4. Cẩm Nang"};
        for (int i = 0; i < 4; i++) {
            int subIdx = i;
            boolean active = guideSubTab == i;
            HoloButton b = addHoloBtn(x + i * (subW + 3), y, subW, 15,
                    (active ? "▸ " : "") + labels[i],
                    active ? BtnTheme.TAB_ACTIVE : BtnTheme.TAB_IDLE,
                    btn -> {
                        guideSubTab = subIdx;
                        for (var box : guideScrollBoxes) box.reset();
                        rebuildWidgets();
                    }, null);
            b.active = !active;
        }

        if (guideSubTab == 1) {
            int total = NovaOmniPadGuideRenderer.recipes().size();
            int bayY = panelY + Math.round(panelH * 0.755f);
            int bayLeftX = panelX + Math.round(panelW * 0.105f);
            int bayRightX = panelX + panelW - Math.round(panelW * 0.105f) - 82;

            addHoloBtn(bayLeftX, bayY, 82, 15, "◀ Công thức trước", BtnTheme.CYAN, b -> {
                recipeIndex = (recipeIndex - 1 + total) % total;
                guideScrollBoxes.get(2).reset();
            }, null);

            addHoloBtn(bayRightX, bayY, 82, 15, "Công thức tiếp ▶", BtnTheme.CYAN, b -> {
                recipeIndex = (recipeIndex + 1) % total;
                guideScrollBoxes.get(2).reset();
            }, null);
        }
    }

    @Override
    public void tick() {
        if (feedbackTimer > 0) feedbackTimer--;
        if (++autoRefreshTicker >= 40) {
            autoRefreshTicker = 0;
            sendAction(NovaOmniPadNetwork.ACTION_REFRESH, List.of(), "");
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Subtle dark backdrop outside the tablet frame
        g.fill(0, 0, width, height, 0x77020710);

        // 1. Render the user's new HD Sci-Fi Hologram Frame texture (sampling active region 11,15 -> 1002x550 of 1024x576)
        g.blit(RenderPipelines.GUI_TEXTURED, FRAME_TEXTURE,
                panelX, panelY, 11, 15, panelW, panelH, 1002, 550, 1024, 576);

        // 2. Gentle Hologram Scanline inside the safe screen area
        int scanTop = contentY;
        int scanBottom = contentY + contentH;
        int scanRange = Math.max(1, scanBottom - scanTop);
        int scanY = scanTop + (int) ((System.currentTimeMillis() / 22L) % scanRange);
        g.hLine(contentX, contentX + contentW, scanY, 0x2800F2FF);

        // 3. Top Trapezoid Crown Title (sits cleanly inside the top trapezoid of the frame!)
        int titleY = panelY + Math.round(panelH * 0.026f);
        g.drawCenteredString(font, "◈ BẢNG ĐIỀU KHIỂN TỐI CAO • NOVA OMNI-PAD ◈",
                width / 2, titleY, 0xFF00F2FF);

        // 4. Render Active Tab Content
        switch (currentTab) {
            case COMMAND_CENTER -> renderCommandCenterContent(g, contentX, contentY, contentW, contentH, mouseX, mouseY);
            case SQUAD_CONTROL -> renderSquadControlContent(g, contentX, contentY, contentW, contentH, mouseX, mouseY);
            case GUIDE_WIKI -> NovaOmniPadGuideRenderer.renderGuideTab(
                    g, font, contentX, contentY + 18, contentW, contentH - 18, guideSubTab, recipeIndex, guideScrollBoxes, mouseX, mouseY);
            case ABOUT_MOD -> NovaOmniPadGuideRenderer.renderAboutTab(
                    g, font, contentX, contentY, contentW, contentH, guideScrollBoxes.get(6), mouseX, mouseY);
        }

        // 5. Render Clean Footer inside the Bottom V-Wing Console (no overlap with V-wing lines!)
        renderFooterInWing(g);

        // 6. Render Widgets (HoloButtons, EditBox)
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderCommandCenterContent(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        if (renamingUuid != null) {
            g.fill(x, y, x + w, y + 18, 0xCC06233B);
            g.renderOutline(x, y, w, 18, 0xFF00F2FF);
            g.drawString(font, "Đặt tên mới:", x + 6, y + 5, 0xFF00F2FF, false);
        }

        if (robots.isEmpty()) {
            NovaOmniPadGuideRenderer.drawHologramBox(g, x, y + 20, w, h - 22, 0xAA061B2E, 0xFF00D4FF);
            g.drawCenteredString(font, "CHƯA CÓ ROBOT NOVA NÀO TRONG MẠNG LƯỚI TOÀN CẦU", width / 2, y + 48, 0xFFFFD166);
            g.drawCenteredString(font, "Xếp cột Vỏ khung → Thân → Đầu hoặc đặt NOVA Chưa Kích Hoạt, rồi dùng Khối Đá Đỏ thức tỉnh!",
                    width / 2, y + 64, 0xFFB8E8FF);
            return;
        }

        int startIdx = cardPage * CARDS_PER_PAGE;
        int cardYStart = y + 19;
        int cardH = (h - 21) / 2;

        for (int i = 0; i < CARDS_PER_PAGE; i++) {
            int idx = startIdx + i;
            if (idx >= robots.size()) break;
            RobotInfo r = robots.get(idx);
            int cy = cardYStart + i * (cardH + 3);

            int borderCol = r.alert() ? 0xFFFF5555 : (r.working() ? 0xFF00F2FF : 0xFF3E68B8);
            NovaOmniPadGuideRenderer.drawHologramBox(g, x, cy, w, cardH, 0xAA051A2E, borderCol);

            // Left: 3D Avatar Box + overlaid Tool Icon in bottom-right corner
            int avX = x + 4;
            int avY = cy + 4;
            int avW = 42;
            int avH = cardH - 8;
            g.fill(avX, avY, avX + avW, avY + avH, 0xCC03101C);
            g.renderOutline(avX, avY, avW, avH, 0xFF00D4FF);

            NovaEntity liveEntity = findClientEntity(r.entityId(), r.uuid());
            if (liveEntity != null) {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        g, avX + 2, avY + 1, avX + avW - 2, avY + avH - 2,
                        17, 0.0F, mx, my, liveEntity);
            } else {
                drawHologramFaceIcon(g, avX + 9, avY + 5, r.dormant(), r.lowBattery());
                g.drawCenteredString(font, r.distanceMeters() + "m", avX + avW / 2, avY + avH - 10, 0xFF7DD3FC);
            }

            // Small tool badge at bottom-right of avatar
            ItemStack toolStack = resolveToolStack(r);
            if (!toolStack.isEmpty()) {
                g.pose().pushMatrix();
                g.pose().translate(avX + avW - 13, avY + avH - 13);
                g.pose().scale(0.75f, 0.75f);
                g.renderItem(toolStack, 0, 0);
                g.pose().popMatrix();
                if (mx >= avX + avW - 14 && mx < avX + avW && my >= avY + avH - 14 && my < avY + avH) {
                    g.setTooltipForNextFrame(font, toolStack, mx, my);
                }
            }

            // Middle: Clean 3-line layout (Name + Lv | Status + Coords | Inline Pin & HP Bars)
            int infoX = avX + avW + 6;
            int maxInfoW = w - (infoX - x) - 190;

            boolean unnamed = r.customName() == null || r.customName().isBlank();
            String nameStr = unnamed
                    ? ("◈ NOVA #" + (idx + 1) + " (Chưa đặt tên)")
                    : ("★ " + r.customName());
            String lvBadge = "  [Lv." + r.skillLevel() + "]";
            NovaOmniPadGuideRenderer.drawFitted(g, font, nameStr + lvBadge, infoX, cy + 5, maxInfoW,
                    unnamed ? 0xFFFFE066 : 0xFF00F2FF);

            String statusStr = r.statusLabel() + " • (" + r.x() + ", " + r.y() + ", " + r.z() + ") • " + r.distanceMeters() + "m";
            NovaOmniPadGuideRenderer.drawFitted(g, font, statusStr, infoX, cy + 16, maxInfoW + 52, 0xFFD0F0FF);

            // Inline HUD Bars at bottom of card (text inside the bar -> super clean!)
            int barY = cy + cardH - 15;
            int barW = Math.min(108, (maxInfoW + 48) / 2);

            int batCol = r.energyPercent() < 15 ? 0xFFD90429 : (r.energyPercent() < 40 ? 0xFFF77F00 : 0xFF00B4D8);
            drawInlineBar(g, infoX, barY, barW, 11, r.energyPercent() / 100f, batCol,
                    "⚡ Pin: " + r.energyPercent() + "%");

            float hpRatio = (float) r.health() / Math.max(1, r.maxHealth());
            int hpCol = hpRatio < 0.35f ? 0xFFD90429 : 0xFF10B981;
            drawInlineBar(g, infoX + barW + 6, barY, barW, 11, hpRatio, hpCol,
                    "♥ HP: " + r.health() + "/" + r.maxHealth());
        }
    }

    private void renderSquadControlContent(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        int leftW = (w * 43) / 100;
        int rightX = x + leftW + 8;

        int listY = y + 17;
        int listH = h - 18;
        NovaOmniPadGuideRenderer.drawHologramBox(g, x, listY, leftW, listH, 0xAA051A2E, 0xFF00D4FF);
        g.drawString(font, "ĐÃ CHỌN: " + selectedUuids.size() + "/" + robots.size() + " NOVA",
                x + 6, listY + 4, 0xFF00F2FF, false);

        int rowStartY = listY + 15;
        int rowH = 18;
        int maxScroll = Math.max(0, robots.size() - SQUAD_ROWS_VISIBLE);
        squadScroll = Mth.clamp(squadScroll, 0, maxScroll);

        for (int i = 0; i < SQUAD_ROWS_VISIBLE; i++) {
            int idx = squadScroll + i;
            if (idx >= robots.size()) break;
            RobotInfo r = robots.get(idx);
            int ry = rowStartY + i * rowH;
            boolean checked = selectedUuids.contains(r.uuid());
            boolean hovered = mx >= x + 4 && mx < x + leftW - 4 && my >= ry && my < ry + rowH - 2;

            int rowBg = checked ? 0xBB0C3B5E : (hovered ? 0x88092A44 : 0x55041422);
            g.fill(x + 4, ry, x + leftW - 4, ry + rowH - 2, rowBg);
            g.renderOutline(x + 4, ry, leftW - 8, rowH - 2, checked ? 0xFF00F2FF : 0xFF1E4E6E);

            String box = checked ? "✔ " : "▫ ";
            String name = (r.customName() == null || r.customName().isBlank()) ? ("NOVA #" + (idx + 1)) : r.customName();
            NovaOmniPadGuideRenderer.drawFitted(g, font, box + name, x + 8, ry + 4, leftW - 50,
                    checked ? 0xFF00FFFF : 0xFFD8F6FF);

            String batStr = r.energyPercent() + "%";
            int batCol = r.energyPercent() < 15 ? 0xFFFF5555 : 0xFF80FFDB;
            g.drawString(font, batStr, x + leftW - 7 - font.width(batStr), ry + 4, batCol, false);
        }

        g.drawString(font, "◈ LỆNH CÔNG VIỆC ĐỒNG LOẠT:", rightX, y + 0, 0xFF00F2FF, false);
        g.drawString(font, "◈ LỆNH DI CHUYỂN & ĐỘI HÌNH:", rightX, y + 60, 0xFF80E5FF, false);
        g.drawString(font, "◈ TUYỆT KỸ ĐỘI HÌNH SIÊU CẤP:", rightX, y + 104, 0xFFA99BFF, false);
    }

    private void renderFooterInWing(GuiGraphics g) {
        int total = robots.size();
        int working = 0, idle = 0, alerts = 0;
        for (RobotInfo r : robots) {
            if (r.working()) working++;
            else if (!r.dormant()) idle++;
            if (r.alert()) alerts++;
        }

        // Upper V-wing diamond (around 78% of panelH): Page indicator
        int totalPages = Math.max(1, (robots.size() + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE);
        int diamondY = panelY + Math.round(panelH * 0.775f);
        if (currentTab == Tab.COMMAND_CENTER) {
            g.drawCenteredString(font, "Trang " + (cardPage + 1) + "/" + totalPages + " (" + total + " NOVA)",
                    width / 2, diamondY, 0xFF80E5FF);
        }

        // Lower V-wing console body (shifted down to 88.8% of panelH so it sits cleanly below the inner cyan V-line!)
        int wingY = panelY + Math.round(panelH * 0.888f);
        String line;
        int color;
        if (feedbackTimer > 0 && !feedbackBanner.isBlank()) {
            line = "✦ " + feedbackBanner;
            color = 0xFF00FFCC;
        } else if (lockdownActive) {
            line = "🔒 KHÓA AN TOÀN TOÀN CỤC ĐANG BẬT (" + total + " NOVA)";
            color = 0xFFFFB703;
        } else if (alerts > 0) {
            boolean blink = (System.currentTimeMillis() / 400L) % 2 == 0;
            line = "⚠ CẢNH BÁO: " + alerts + " NOVA pin yếu (<15%)!  •  Làm việc: " + working + "  •  Rảnh: " + idle;
            color = blink ? 0xFFFF4D6D : 0xFFFFB703;
        } else {
            line = "● Mạng lưới ổn định  •  Tổng: " + total + "  •  Làm việc: " + working + "  •  Rảnh: " + idle;
            color = 0xFF00F5D4;
        }

        // Auto-scale footer line so it fits strictly inside the bottom V-wing console
        int maxWingW = Math.round(panelW * 0.42f);
        int textW = font.width(line);
        float scale = textW > maxWingW ? ((float) maxWingW / textW) : 0.9f;
        g.pose().pushMatrix();
        g.pose().translate(width / 2.0f, wingY);
        g.pose().scale(scale, scale);
        g.drawString(font, line, Math.round(-textW / 2.0f), 0, color, false);
        g.pose().popMatrix();
    }

    private void drawInlineBar(GuiGraphics g, int x, int y, int w, int h, float ratio, int fillColor, String label) {
        ratio = Mth.clamp(ratio, 0f, 1f);
        g.fill(x, y, x + w, y + h, 0xDD03101C);
        int fillW = Math.round((w - 2) * ratio);
        if (fillW > 0) {
            g.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, fillColor);
        }
        g.renderOutline(x, y, w, h, 0xFF1D638C);

        int tw = font.width(label);
        float scale = 0.8f;
        g.pose().pushMatrix();
        g.pose().translate(x + (w - tw * scale) / 2.0f, y + 2.0f);
        g.pose().scale(scale, scale);
        g.drawString(font, label, 0, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
    }

    private void drawHologramFaceIcon(GuiGraphics g, int x, int y, boolean dormant, boolean lowBat) {
        int col = dormant ? 0xFF64748B : (lowBat ? 0xFFFF5555 : 0xFF00F2FF);
        g.fill(x, y, x + 24, y + 18, 0xFF062338);
        g.renderOutline(x, y, 24, 18, col);
        if (dormant) {
            g.hLine(x + 5, x + 9, y + 7, col);
            g.hLine(x + 14, x + 18, y + 7, col);
        } else {
            g.fill(x + 5, y + 4, x + 9, y + 10, col);
            g.fill(x + 15, y + 4, x + 19, y + 10, col);
        }
        g.hLine(x + 7, x + 16, y + 13, col);
    }

    private NovaEntity findClientEntity(int entityId, UUID uuid) {
        if (minecraft == null || minecraft.level == null) return null;
        if (minecraft.level.getEntity(entityId) instanceof NovaEntity nova && nova.getUUID().equals(uuid)) {
            return nova;
        }
        return null;
    }

    private ItemStack resolveToolStack(RobotInfo r) {
        if (r.toolItemId() == null || r.toolItemId().isBlank()) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(r.toolItemId());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
        if (r.toolMaxDamage() > 0 && r.toolDamage() > 0) {
            stack.setDamageValue(r.toolDamage());
        }
        return stack;
    }

    private List<NovaOmniPadGuideRenderer.HoloScrollBox> activeScrollBoxes() {
        if (currentTab == Tab.GUIDE_WIKI) {
            return switch (guideSubTab) {
                case 0 -> guideScrollBoxes.subList(0, 2);
                case 1 -> guideScrollBoxes.subList(2, 3);
                case 2 -> guideScrollBoxes.subList(3, 5);
                default -> guideScrollBoxes.subList(5, 6);
            };
        }
        if (currentTab == Tab.ABOUT_MOD) {
            return guideScrollBoxes.subList(6, 7);
        }
        return List.of();
    }

    private boolean isShiftKeyHeld() {
        if (minecraft == null || minecraft.getWindow() == null) return false;
        long win = minecraft.getWindow().handle();
        return GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (var box : activeScrollBoxes()) {
            if (box.mouseClicked(event.x(), event.y(), event.button())) {
                return true;
            }
        }
        if (currentTab == Tab.SQUAD_CONTROL && event.button() == 0) {
            int leftW = (contentW * 43) / 100;
            int rowStartY = contentY + 32;
            int rowH = 18;

            for (int i = 0; i < SQUAD_ROWS_VISIBLE; i++) {
                int idx = squadScroll + i;
                if (idx >= robots.size()) break;
                int ry = rowStartY + i * rowH;
                if (event.x() >= contentX + 4 && event.x() < contentX + leftW - 4
                        && event.y() >= ry && event.y() < ry + rowH - 2) {
                    UUID uuid = robots.get(idx).uuid();
                    if (!selectedUuids.remove(uuid)) {
                        selectedUuids.add(uuid);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        for (var box : activeScrollBoxes()) {
            if (box.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY)) {
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (var box : activeScrollBoxes()) {
            if (box.mouseReleased(event.button())) {
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        boolean shift = isShiftKeyHeld();
        for (var box : activeScrollBoxes()) {
            if (box.mouseScrolled(x, y, horizontal, vertical, shift)) {
                return true;
            }
        }
        if (vertical != 0) {
            int delta = vertical < 0 ? 1 : -1;
            if (currentTab == Tab.COMMAND_CENTER) {
                int totalPages = Math.max(1, (robots.size() + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE);
                int next = Mth.clamp(cardPage + delta, 0, totalPages - 1);
                if (next != cardPage) {
                    cardPage = next;
                    rebuildWidgets();
                }
                return true;
            } else if (currentTab == Tab.SQUAD_CONTROL) {
                int maxScroll = Math.max(0, robots.size() - SQUAD_ROWS_VISIBLE);
                squadScroll = Mth.clamp(squadScroll + delta, 0, maxScroll);
                return true;
            } else if (currentTab == Tab.GUIDE_WIKI && guideSubTab == 1) {
                int total = NovaOmniPadGuideRenderer.recipes().size();
                recipeIndex = (recipeIndex + delta + total) % total;
                guideScrollBoxes.get(2).reset();
                return true;
            }
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (renamingUuid != null && renameInput != null) {
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                sendAction(NovaOmniPadNetwork.ACTION_RENAME, List.of(renamingUuid), renameInput.getValue());
                renamingUuid = null;
                renameInput = null;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                renamingUuid = null;
                renameInput = null;
                rebuildWidgets();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
