package net.teogemini.nova.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.teogemini.nova.registry.ModBlocks;
import net.teogemini.nova.registry.ModItems;
import net.teogemini.nova.registry.NovaMaterials;

public final class NovaOmniPadGuideRenderer {
    public record RecipeEntry(
            String title,
            String typeLabel,
            ItemStack[] grid,
            ItemStack result,
            List<String> detailLines
    ) {}

    public static final class HoloScrollBox {
        private float scrollX;
        private float scrollY;
        private int maxScrollX;
        private int maxScrollY;
        private int x, y, w, h;
        private boolean draggingV;
        private boolean draggingH;

        public void reset() {
            scrollX = 0;
            scrollY = 0;
            draggingV = false;
            draggingH = false;
        }

        public void begin(GuiGraphics g, int bx, int by, int bw, int bh, int contentW, int contentH, int bgColor, int borderColor) {
            this.x = bx;
            this.y = by;
            this.w = bw;
            this.h = bh;
            drawHologramBox(g, bx, by, bw, bh, bgColor, borderColor);

            int viewW = Math.max(10, bw - 12);
            int viewH = Math.max(10, bh - 12);
            this.maxScrollX = Math.max(0, contentW - viewW);
            this.maxScrollY = Math.max(0, contentH - viewH);
            this.scrollX = Mth.clamp(scrollX, 0, maxScrollX);
            this.scrollY = Mth.clamp(scrollY, 0, maxScrollY);

            g.enableScissor(bx + 3, by + 3, bx + 3 + viewW, by + 3 + viewH);
        }

        public void end(GuiGraphics g, int mx, int my) {
            g.disableScissor();

            // Vertical scrollbar (right edge)
            int vx = x + w - 7;
            int vy = y + 3;
            int vw = 5;
            int vh = Math.max(12, h - 11);
            g.fill(vx, vy, vx + vw, vy + vh, 0xAA03101C);
            g.renderOutline(vx, vy, vw, vh, 0xFF1B5278);

            int vThumbH = maxScrollY > 0
                    ? Math.max(10, Math.round(vh * (float) (h - 12) / (h - 12 + maxScrollY)))
                    : vh;
            int vThumbY = vy + (maxScrollY > 0
                    ? Math.round((vh - vThumbH) * (scrollY / maxScrollY))
                    : 0);
            boolean hovV = draggingV || (mx >= vx - 1 && mx <= vx + vw + 1 && my >= vy && my <= vy + vh);
            g.fill(vx + 1, vThumbY, vx + vw - 1, vThumbY + vThumbH, hovV ? 0xFF00FFFF : 0xFF00C8F0);

            // Horizontal scrollbar (bottom edge)
            int hx = x + 3;
            int hy = y + h - 7;
            int hw = Math.max(12, w - 11);
            int hh = 5;
            g.fill(hx, hy, hx + hw, hy + hh, 0xAA03101C);
            g.renderOutline(hx, hy, hw, hh, 0xFF1B5278);

            int hThumbW = maxScrollX > 0
                    ? Math.max(14, Math.round(hw * (float) (w - 12) / (w - 12 + maxScrollX)))
                    : hw;
            int hThumbX = hx + (maxScrollX > 0
                    ? Math.round((hw - hThumbW) * (scrollX / maxScrollX))
                    : 0);
            boolean hovH = draggingH || (mx >= hx && mx <= hx + hw && my >= hy - 1 && my <= hy + hh + 1);
            g.fill(hThumbX, hy + 1, hThumbX + hThumbW, hy + hh - 1, hovH ? 0xFF00FFFF : 0xFF00C8F0);
        }

        public int ox() {
            return x + 5 - Math.round(scrollX);
        }

        public int oy() {
            return y + 5 - Math.round(scrollY);
        }

        public boolean isMouseOver(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0 || !isMouseOver(mx, my)) return false;
            int vx = x + w - 8;
            int vy = y + 3;
            int vh = Math.max(12, h - 11);
            int hx = x + 3;
            int hy = y + h - 8;
            int hw = Math.max(12, w - 11);

            if (mx >= vx && mx <= x + w && my >= vy && my <= vy + vh) {
                draggingV = true;
                updateVerticalFromMouse(my);
                return true;
            }
            if (mx >= hx && mx <= hx + hw && my >= hy && my <= y + h) {
                draggingH = true;
                updateHorizontalFromMouse(mx);
                return true;
            }
            return false;
        }

        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (button != 0) return false;
            if (draggingV) {
                updateVerticalFromMouse(my);
                return true;
            }
            if (draggingH) {
                updateHorizontalFromMouse(mx);
                return true;
            }
            if (isMouseOver(mx, my)) {
                scrollX = Mth.clamp(scrollX - (float) dx, 0, maxScrollX);
                scrollY = Mth.clamp(scrollY - (float) dy, 0, maxScrollY);
                return true;
            }
            return false;
        }

        public boolean mouseReleased(int button) {
            if (button == 0 && (draggingV || draggingH)) {
                draggingV = false;
                draggingH = false;
                return true;
            }
            return false;
        }

        public boolean mouseScrolled(double mx, double my, double horizontal, double vertical, boolean shiftDown) {
            if (!isMouseOver(mx, my)) return false;
            boolean onBottomBar = my >= y + h - 12;
            if (horizontal != 0) {
                scrollX = Mth.clamp(scrollX - (float) (horizontal * 22), 0, maxScrollX);
                return true;
            }
            if (shiftDown || onBottomBar || (maxScrollY == 0 && maxScrollX > 0)) {
                scrollX = Mth.clamp(scrollX - (float) (vertical * 24), 0, maxScrollX);
                return true;
            }
            if (maxScrollY > 0) {
                scrollY = Mth.clamp(scrollY - (float) (vertical * 18), 0, maxScrollY);
                return true;
            }
            return false;
        }

        private void updateVerticalFromMouse(double my) {
            int vy = y + 3;
            int vh = Math.max(12, h - 11);
            float ratio = Mth.clamp((float) (my - vy) / vh, 0f, 1f);
            scrollY = ratio * maxScrollY;
        }

        private void updateHorizontalFromMouse(double mx) {
            int hx = x + 3;
            int hw = Math.max(12, w - 11);
            float ratio = Mth.clamp((float) (mx - hx) / hw, 0f, 1f);
            scrollX = ratio * maxScrollX;
        }
    }

    private static List<RecipeEntry> cachedRecipes;

    private NovaOmniPadGuideRenderer() {}

    public static List<RecipeEntry> recipes() {
        if (cachedRecipes != null) return cachedRecipes;
        List<RecipeEntry> list = new ArrayList<>();

        ItemStack e = ItemStack.EMPTY;
        ItemStack rawSi = new ItemStack(NovaMaterials.RAW_SILICON);
        ItemStack wafer = new ItemStack(NovaMaterials.SILICON_WAFER);
        ItemStack crystal = new ItemStack(NovaMaterials.ENERGY_CRYSTAL);
        ItemStack circuit = new ItemStack(NovaMaterials.NOVA_CIRCUIT);
        ItemStack chip = new ItemStack(NovaMaterials.CONTROL_CHIP);
        ItemStack aiProc = new ItemStack(NovaMaterials.NOVA_AI_PROCESSOR);
        ItemStack screen = new ItemStack(NovaMaterials.NOVA_SCREEN);
        ItemStack core = new ItemStack(NovaMaterials.NOVA_ENERGY_CORE);
        ItemStack battery = new ItemStack(NovaMaterials.NOVA_BATTERY);
        ItemStack motor = new ItemStack(NovaMaterials.SMALL_MOTOR);
        ItemStack casing = new ItemStack(NovaMaterials.NOVA_FRAME_CASING);
        ItemStack head = new ItemStack(NovaMaterials.NOVA_HEAD_UNIT);
        ItemStack body = new ItemStack(NovaMaterials.NOVA_BODY_UNIT);
        ItemStack dormant = new ItemStack(ModItems.NOVA_DORMANT);
        ItemStack omniPad = new ItemStack(NovaMaterials.NOVA_OMNI_PAD);

        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack copper = new ItemStack(Items.COPPER_INGOT);
        ItemStack redstone = new ItemStack(Items.REDSTONE);
        ItemStack glassPane = new ItemStack(Items.GLASS_PANE);
        ItemStack glowstone = new ItemStack(Items.GLOWSTONE_DUST);
        ItemStack lantern = new ItemStack(Items.LANTERN);
        ItemStack chest = new ItemStack(Items.CHEST);

        list.add(new RecipeEntry(
                "Bảng Điều Khiển NOVA (Omni-Pad)",
                "Bàn chế tạo 3×3",
                new ItemStack[]{
                        wafer, aiProc, wafer,
                        circuit, screen, circuit,
                        e, battery, e
                },
                omniPad,
                List.of(
                        "• Nguyên liệu: 2 Phiến silic + 1 Bộ xử lý AI + 2 Mạch điện + 1 Màn hình + 1 Pin NOVA.",
                        "• Công dụng: Thiết bị chỉ huy tối thượng giúp giám sát toàn bộ mạng lưới NOVA toàn cầu.",
                        "• Tính năng: Đặt tên trực tiếp, Dịch chuyển lượng tử, Định vị xuyên tường, Mở kho đồ & Sạc từ xa."
                )
        ));

        list.add(new RecipeEntry(
                "Phiến Silic (Silicon Wafer)",
                "Lò nung / Lò luyện kim",
                new ItemStack[]{
                        e, rawSi, e,
                        e, new ItemStack(Items.COAL), e,
                        e, e, e
                },
                wafer,
                List.of(
                        "• Nguyên liệu: 1 Silic thô (Raw Silicon) + Nhiên liệu đốt bất kỳ.",
                        "• Thời gian: Lò nung thường (10 giây) hoặc Lò luyện kim Blast Furnace (5 giây).",
                        "• Công dụng: Vật liệu bán dẫn nền tảng để chế tạo Mạch điện, Chip và Vỏ khung NOVA."
                )
        ));

        list.add(new RecipeEntry(
                "Mạch Điện NOVA (×2)",
                "Ghép tự do (2×2 / 3×3)",
                new ItemStack[]{
                        wafer, copper, e,
                        redstone, e, e,
                        e, e, e
                },
                new ItemStack(NovaMaterials.NOVA_CIRCUIT, 2),
                List.of(
                        "• Nguyên liệu: 1 Phiến silic + 1 Phôi đồng + 1 Bụi đá đỏ (Ghép tự do không cần thứ tự).",
                        "• Sản lượng: Nhận ngay 2 Mạch điện NOVA mỗi lần ghép.",
                        "• Công dụng: Thành phần bắt buộc trong hầu hết các linh kiện điện tử của NOVA."
                )
        ));

        list.add(new RecipeEntry(
                "Chip Điều Khiển",
                "Ghép tự do (2×2 / 3×3)",
                new ItemStack[]{
                        circuit, wafer, e,
                        redstone, e, e,
                        e, e, e
                },
                chip,
                List.of(
                        "• Nguyên liệu: 1 Mạch điện NOVA + 1 Phiến silic + 1 Bụi đá đỏ.",
                        "• Công dụng: Vi mạch xử lý tín hiệu điều khiển trung tâm cho Đầu NOVA và Bộ xử lý AI."
                )
        ));

        list.add(new RecipeEntry(
                "Bộ Xử Lý AI NOVA",
                "Ghép tự do (2×2 / 3×3)",
                new ItemStack[]{
                        chip, circuit, e,
                        crystal, iron, e,
                        e, e, e
                },
                aiProc,
                List.of(
                        "• Nguyên liệu: 1 Chip điều khiển + 1 Mạch điện NOVA + 1 Tinh thạch năng lượng + 1 Phôi sắt.",
                        "• Công dụng: Bộ não trí tuệ nhân tạo giúp NOVA suy nghĩ, trò chuyện và làm việc tự động."
                )
        ));

        list.add(new RecipeEntry(
                "Màn Hình NOVA",
                "Ghép tự do (2×2 / 3×3)",
                new ItemStack[]{
                        glassPane, circuit, e,
                        glowstone, iron, e,
                        e, e, e
                },
                screen,
                List.of(
                        "• Nguyên liệu: 1 Tấm kính + 1 Mạch điện NOVA + 1 Bụi đá phát sáng + 1 Phôi sắt.",
                        "• Công dụng: Màn hình hiển thị 40 biểu cảm khuôn mặt LED của NOVA và màn hình cảm ứng Omni-Pad."
                )
        ));

        list.add(new RecipeEntry(
                "Lõi Năng Lượng NOVA",
                "Ghép tự do (2×2 / 3×3)",
                new ItemStack[]{
                        crystal, circuit, e,
                        copper, redstone, e,
                        e, e, e
                },
                core,
                List.of(
                        "• Nguyên liệu: 1 Tinh thạch năng lượng + 1 Mạch điện NOVA + 1 Phôi đồng + 1 Bụi đá đỏ.",
                        "• Công dụng: Trái tim chuyển hóa năng lượng cung cấp nguồn điện ổn định cho Thân NOVA."
                )
        ));

        list.add(new RecipeEntry(
                "Pin NOVA",
                "Ghép tự do (2×2 / 3×3)",
                new ItemStack[]{
                        core, copper, e,
                        iron, redstone, e,
                        e, e, e
                },
                battery,
                List.of(
                        "• Nguyên liệu: 1 Lõi năng lượng NOVA + 1 Phôi đồng + 1 Phôi sắt + 1 Bụi đá đỏ.",
                        "• Công dụng: Bộ lưu trữ điện năng dung lượng cao cho Thân NOVA và Bảng điều khiển Omni-Pad."
                )
        ));

        list.add(new RecipeEntry(
                "Động Cơ Nhỏ (×2)",
                "Ghép tự do (2×2 / 3×3)",
                new ItemStack[]{
                        copper, iron, e,
                        redstone, e, e,
                        e, e, e
                },
                new ItemStack(NovaMaterials.SMALL_MOTOR, 2),
                List.of(
                        "• Nguyên liệu: 1 Phôi đồng + 1 Phôi sắt + 1 Bụi đá đỏ (Nhận 2 Động cơ nhỏ mỗi lần ghép).",
                        "• Công dụng: Động cơ trợ lực khớp tay chân giúp NOVA di chuyển, leo giàn giáo và vung công cụ."
                )
        ));

        list.add(new RecipeEntry(
                "Vỏ Khung NOVA (×3)",
                "Ghép tự do (2×2 / 3×3)",
                new ItemStack[]{
                        iron, copper, e,
                        wafer, e, e,
                        e, e, e
                },
                new ItemStack(NovaMaterials.NOVA_FRAME_CASING, 3),
                List.of(
                        "• Nguyên liệu: 1 Phôi sắt + 1 Phôi đồng + 1 Phiến silic (Nhận 3 Khối vỏ khung mỗi lần ghép).",
                        "• Công dụng: Dùng để chế tạo Thân NOVA và làm khối đế dưới cùng khi lắp ráp cột 3 khối ngoài thế giới."
                )
        ));

        list.add(new RecipeEntry(
                "Đầu NOVA (Khối Lắp Ráp)",
                "Bàn chế tạo 3×3",
                new ItemStack[]{
                        iron, lantern, iron,
                        chip, screen, aiProc,
                        e, iron, e
                },
                head,
                List.of(
                        "• Hàng 1: Phôi sắt – Đèn lồng (Lantern) – Phôi sắt",
                        "• Hàng 2: Chip điều khiển – Màn hình NOVA – Bộ xử lý AI NOVA",
                        "• Hàng 3: (Trống) – Phôi sắt – (Trống)",
                        "• Công dụng: Đặt lên trên cùng của cột 3 khối (Vỏ Khung → Thân → Đầu) để triệu hồi Tượng NOVA kèm Rương NOVA."
                )
        ));

        list.add(new RecipeEntry(
                "Thân NOVA (Khối Lắp Ráp)",
                "Bàn chế tạo 3×3",
                new ItemStack[]{
                        casing, core, casing,
                        motor, battery, motor,
                        iron, chest, iron
                },
                body,
                List.of(
                        "• Hàng 1: Vỏ khung NOVA – Lõi năng lượng NOVA – Vỏ khung NOVA",
                        "• Hàng 2: Động cơ nhỏ – Pin NOVA – Động cơ nhỏ",
                        "• Hàng 3: Phôi sắt – Rương gỗ (Chest) – Phôi sắt",
                        "• Công dụng: Khối trung tâm chứa nguồn điện và khoang hành lý 27 ô của NOVA."
                )
        ));

        list.add(new RecipeEntry(
                "NOVA Chưa Kích Hoạt (Tượng)",
                "Bàn chế tạo 3×3",
                new ItemStack[]{
                        e, head, e,
                        casing, body, casing,
                        e, casing, e
                },
                dormant,
                List.of(
                        "• Nguyên liệu: 1 Đầu NOVA (trên) + 1 Thân NOVA (giữa) + 3 Vỏ khung NOVA (trái, phải, dưới).",
                        "• Công dụng: Đặt Tượng NOVA ra mặt đất rồi dùng 1 Khối Đá Đỏ nhấp phải để thức tỉnh 100% pin!"
                )
        ));

        list.add(new RecipeEntry(
                "Rương NOVA & Gậy Điều Khiển",
                "Bàn chế tạo 3×3",
                new ItemStack[]{
                        e, copper, e,
                        copper, chest, copper,
                        e, redstone, e
                },
                new ItemStack(ModBlocks.NOVA_CHEST_ITEM),
                List.of(
                        "• Rương NOVA: 3 Phôi đồng + 1 Rương gỗ + 1 Bụi đá đỏ (Kho trung tâm để NOVA cất đồ thu hoạch).",
                        "• Gậy Điều Khiển: 1 Cột thu lôi (trên) + 1 Đá đỏ (giữa) + 1 Phôi sắt (dưới)."
                )
        ));

        cachedRecipes = list;
        return list;
    }

    public static void renderGuideTab(GuiGraphics g, Font font, int x, int y, int w, int h,
                                      int subTab, int recipeIndex, List<HoloScrollBox> scrollBoxes, int mx, int my) {
        if (subTab == 0) {
            renderOresGuide(g, font, x, y, w, h, scrollBoxes.get(0), scrollBoxes.get(1), mx, my);
        } else if (subTab == 1) {
            renderRecipesGuide(g, font, x, y, w, h, recipeIndex, scrollBoxes.get(2), mx, my);
        } else if (subTab == 2) {
            renderAssemblyGuide(g, font, x, y, w, h, scrollBoxes.get(3), scrollBoxes.get(4), mx, my);
        } else {
            renderManualGuide(g, font, x, y, w, h, scrollBoxes.get(5), mx, my);
        }
    }

    private static void renderOresGuide(GuiGraphics g, Font font, int x, int y, int w, int h,
                                        HoloScrollBox boxLeft, HoloScrollBox boxRight, int mx, int my) {
        int cardW = (w - 8) / 2;

        String[] siLines = {
                "• Độ cao xuất hiện: Y = -16 đến 112 (Dày nhất quanh Y = 48)",
                "• Mạch lộ vách hang (Cave): Y = -24 đến 96",
                "• Mạch nối dài (Vein): Y = -32 đến 80",
                "• Công cụ khai thác: Cúp Đá (Stone Pickaxe) trở lên",
                "• Vật phẩm rơi ra: 1–2 Silic thô (Hỗ trợ phù phép Gia tài & Chạm lụa)",
                "• Chế biến: Nung Silic thô trong Lò nung (10s) hoặc Lò luyện kim (5s) → Phiến Silic",
                "• Kinh nghiệm AI: NOVA đào quặng này nhận +2 EXP kỹ năng/khối"
        };
        int leftMaxW = measureMaxWidth(font, siLines) + 14;
        int leftMaxH = 44 + siLines.length * 13;

        boxLeft.begin(g, x, y, cardW, h, leftMaxW, leftMaxH, 0xAA061B2E, 0xFF00D4FF);
        int ox = boxLeft.ox(), oy = boxLeft.oy();
        g.drawString(font, "1. QUẶNG SILIC (SILICON ORE)", ox, oy, 0xFF00F2FF, false);
        drawItemSlot(g, font, new ItemStack(NovaMaterials.SILICON_ORE), ox, oy + 13, mx, my);
        drawItemSlot(g, font, new ItemStack(NovaMaterials.DEEPSLATE_SILICON_ORE), ox + 20, oy + 13, mx, my);
        g.drawString(font, "→", ox + 42, oy + 18, 0xFF80E5FF, false);
        drawItemSlot(g, font, new ItemStack(NovaMaterials.RAW_SILICON), ox + 52, oy + 13, mx, my);
        g.drawString(font, "→", ox + 74, oy + 18, 0xFF80E5FF, false);
        drawItemSlot(g, font, new ItemStack(NovaMaterials.SILICON_WAFER), ox + 84, oy + 13, mx, my);

        int ty = oy + 36;
        for (String line : siLines) {
            g.drawString(font, line, ox, ty, 0xFFD8F6FF, false);
            ty += 13;
        }
        boxLeft.end(g, mx, my);

        int x2 = x + cardW + 8;
        String[] ecLines = {
                "• Độ cao xuất hiện: Y = -32 đến 64 (Dày nhất quanh Y = 8)",
                "• Mạch lộ vách hang (Cave): Y = -40 đến 56",
                "• Mạch lớn đá sâu (Vein): Y = -48 đến 48",
                "• Đặc tính: Tự phát sáng nhẹ (Mức sáng 5) dễ nhận biết trong hang tối",
                "• Công cụ khai thác: Cúp Đá (Stone Pickaxe) trở lên",
                "• Vật phẩm rơi ra: 1–2 Tinh thạch năng lượng + 1–3 điểm EXP tự nhiên",
                "• Công dụng: Chế tạo Lõi Năng Lượng NOVA và Bộ Xử Lý AI NOVA"
        };
        int rightMaxW = measureMaxWidth(font, ecLines) + 14;
        int rightMaxH = 44 + ecLines.length * 13;

        boxRight.begin(g, x2, y, cardW, h, rightMaxW, rightMaxH, 0xAA061B2E, 0xFF7B68EE);
        int ox2 = boxRight.ox(), oy2 = boxRight.oy();
        g.drawString(font, "2. QUẶNG TINH THẠCH NĂNG LƯỢNG", ox2, oy2, 0xFFA99BFF, false);
        drawItemSlot(g, font, new ItemStack(NovaMaterials.ENERGY_CRYSTAL_ORE), ox2, oy2 + 13, mx, my);
        drawItemSlot(g, font, new ItemStack(NovaMaterials.DEEPSLATE_ENERGY_CRYSTAL_ORE), ox2 + 20, oy2 + 13, mx, my);
        g.drawString(font, "→", ox2 + 42, oy2 + 18, 0xFFA99BFF, false);
        drawItemSlot(g, font, new ItemStack(NovaMaterials.ENERGY_CRYSTAL), ox2 + 52, oy2 + 13, mx, my);
        drawItemSlot(g, font, new ItemStack(NovaMaterials.NOVA_ENERGY_CORE), ox2 + 76, oy2 + 13, mx, my);

        ty = oy2 + 36;
        for (String line : ecLines) {
            g.drawString(font, line, ox2, ty, 0xFFE6E0FF, false);
            ty += 13;
        }
        boxRight.end(g, mx, my);
    }

    private static void renderRecipesGuide(GuiGraphics g, Font font, int x, int y, int w, int h,
                                           int recipeIndex, HoloScrollBox descBox, int mx, int my) {
        List<RecipeEntry> list = recipes();
        if (list.isEmpty()) return;
        int idx = ((recipeIndex % list.size()) + list.size()) % list.size();
        RecipeEntry entry = list.get(idx);

        drawHologramBox(g, x, y, w, h, 0xAA061B2E, 0xFF00D4FF);

        String title = "[" + (idx + 1) + "/" + list.size() + "] " + entry.title() + "  (" + entry.typeLabel() + ")";
        g.drawString(font, title, x + 10, y + 6, 0xFF00F2FF, false);

        int gridX = x + 12;
        int gridY = y + 21;
        g.fill(gridX - 3, gridY - 3, gridX + 59, gridY + 59, 0xCC041220);
        g.renderOutline(gridX - 3, gridY - 3, 62, 62, 0xFF00D9F5);

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int sx = gridX + c * 19;
                int sy = gridY + r * 19;
                int slotIdx = r * 3 + c;
                ItemStack stack = slotIdx < entry.grid().length ? entry.grid()[slotIdx] : ItemStack.EMPTY;
                drawItemSlot(g, font, stack, sx, sy, mx, my);
            }
        }

        g.drawString(font, "==>", gridX + 63, gridY + 23, 0xFF00F2FF, false);
        int resX = gridX + 83;
        int resY = gridY + 18;
        g.fill(resX - 3, resY - 3, resX + 21, resY + 21, 0xDD0A2E46);
        g.renderOutline(resX - 3, resY - 3, 24, 24, 0xFF00FFCC);
        drawItemSlot(g, font, entry.result(), resX, resY, mx, my);

        // Scrollable detail box on the right
        int descX = resX + 28;
        int descY = y + 18;
        int descW = (x + w) - descX - 6;
        int descH = h - 24;

        List<String> lines = new ArrayList<>();
        lines.add("CHI TIẾT CÔNG THỨC & CÔNG DỤNG:");
        lines.addAll(entry.detailLines());
        lines.add("• Mẹo: Rê chuột vào ô vật phẩm để xem tên, bấm ◀ / ▶ ở dưới để chuyển công thức.");

        int maxLineW = measureMaxWidth(font, lines.toArray(String[]::new)) + 14;
        int maxLineH = lines.size() * 14 + 8;

        descBox.begin(g, descX, descY, descW, descH, maxLineW, maxLineH, 0x88041424, 0xFF1D638C);
        int ox = descBox.ox(), oy = descBox.oy();
        for (int i = 0; i < lines.size(); i++) {
            int col = i == 0 ? 0xFF80E5FF : (i == lines.size() - 1 ? 0xFF7DD3FC : 0xFFE2F8FF);
            g.drawString(font, lines.get(i), ox, oy + i * 14, col, false);
        }
        descBox.end(g, mx, my);
    }

    private static void renderAssemblyGuide(GuiGraphics g, Font font, int x, int y, int w, int h,
                                            HoloScrollBox boxLeft, HoloScrollBox boxRight, int mx, int my) {
        int leftW = (w - 8) / 2;

        String[] leftLines = {
                "• Bước 1: Đặt 1 Vỏ Khung NOVA ở dưới cùng làm chân đế.",
                "• Bước 2: Đặt 1 Khối Thân NOVA chồng lên chính giữa.",
                "• Bước 3: Đặt 1 Khối Đầu NOVA lên trên cùng (đặt sau cùng).",
                "• Kết quả: Cột 3 khối tự biến thành 1 Rương NOVA ở dưới (đã liên kết sẵn) và Tượng NOVA đứng trên nóc rương!",
                "• Bước 4: Cầm 1 Khối Đá Đỏ nhấp chuột phải vào Tượng để thức tỉnh Robot NOVA (100% Pin)!"
        };
        int leftMaxW = Math.max(160, measureMaxWidth(font, leftLines) + 14);
        int leftMaxH = 82 + leftLines.length * 13;

        boxLeft.begin(g, x, y, leftW, h, leftMaxW, leftMaxH, 0xAA061B2E, 0xFF00D4FF);
        int ox = boxLeft.ox(), oy = boxLeft.oy();
        g.drawString(font, "LẮP RÁP CỘT 3 KHỐI NGOÀI THẾ GIỚI", ox, oy, 0xFF00F2FF, false);

        ItemStack head = new ItemStack(NovaMaterials.NOVA_HEAD_UNIT);
        ItemStack body = new ItemStack(NovaMaterials.NOVA_BODY_UNIT);
        ItemStack casing = new ItemStack(NovaMaterials.NOVA_FRAME_CASING);
        ItemStack novaChest = new ItemStack(ModBlocks.NOVA_CHEST_ITEM);
        ItemStack rsBlock = new ItemStack(Items.REDSTONE_BLOCK);
        ItemStack dormant = new ItemStack(ModItems.NOVA_DORMANT);

        // 3-block vertical column (Head -> Body -> Frame Casing)
        int cx = ox + 10;
        int cy = oy + 13;
        drawItemSlot(g, font, head, cx, cy, mx, my);
        drawItemSlot(g, font, body, cx, cy + 19, mx, my);
        drawItemSlot(g, font, casing, cx, cy + 38, mx, my);

        // Transforms into: Dormant NOVA on top + Linked NOVA Chest underneath
        g.drawString(font, "→", cx + 24, cy + 24, 0xFF00F2FF, false);
        drawItemSlot(g, font, dormant, cx + 38, cy + 19, mx, my);
        drawItemSlot(g, font, novaChest, cx + 38, cy + 38, mx, my);

        // Awaken with Redstone Block (aligned horizontally with the NOVA slot)
        g.drawString(font, "+", cx + 62, cy + 24, 0xFFFF6B6B, false);
        drawItemSlot(g, font, rsBlock, cx + 73, cy + 19, mx, my);

        int ly = oy + 74;
        for (String s : leftLines) {
            g.drawString(font, s, ox, ly, 0xFFD8F6FF, false);
            ly += 13;
        }
        boxLeft.end(g, mx, my);

        int x2 = x + leftW + 8;
        String[] rightInfo = {
                "• Đổi 5 tư thế trưng bày: Nhấp chuột phải tay không vào tượng NOVA Chưa Kích Hoạt:",
                "  1) Tắt nguồn ngủ đông   2) Đứng nghiêm canh gác   3) Ngồi gập gối chờ lệnh",
                "  4) Giơ tay vẫy chào     5) Chiến binh thủ thế",
                "• Thu hồi tượng nhanh: Shift + Chuột phải (hoặc đập bằng chuột trái) để cất vào túi đồ.",
                "• Bảo toàn toàn bộ dữ liệu: Khi thu hồi và đặt lại, NOVA giữ nguyên Tên định danh,",
                "  cấp độ kỹ năng EXP, tính cách, kỷ niệm, công cụ đang cầm và toàn bộ đồ trong ba lô!",
                "• Hết pin tự động: Khi pin về 0%, NOVA tự chuyển về dạng Tượng an toàn chờ sạc lại."
        };
        int rightMaxW = measureMaxWidth(font, rightInfo) + 14;
        int rightMaxH = 22 + rightInfo.length * 13;

        boxRight.begin(g, x2, y, leftW, h, rightMaxW, rightMaxH, 0xAA061B2E, 0xFF7B68EE);
        int ox2 = boxRight.ox(), oy2 = boxRight.oy();
        g.drawString(font, "CƠ CHẾ TƯỢNG & BẢO TOÀN DỮ LIỆU", ox2, oy2, 0xFFA99BFF, false);
        int ry = oy2 + 15;
        for (String line : rightInfo) {
            g.drawString(font, line, ox2, ry, 0xFFE6E0FF, false);
            ry += 13;
        }
        boxRight.end(g, mx, my);
    }

    private static void renderManualGuide(GuiGraphics g, Font font, int x, int y, int w, int h,
                                          HoloScrollBox box, int mx, int my) {
        String[] lines = {
                "1. PHÂN CÔNG NGHỀ TỰ ĐỘNG THEO CÔNG CỤ TAY PHẢI:",
                "   • Rìu (Axe) → Tiều phu: Tự chặt cây gỗ, thu gom gỗ/mầm cây & tự trồng lại rừng khi tay trái có mầm cây.",
                "   • Cúp (Pickaxe) → Thợ mỏ: Tự dò tìm & khai thác mạch quặng xung quanh, tự động cắm đuốc khi vào hang tối.",
                "   • Cuốc (Hoe) → Nông dân: Tự thu hoạch nông sản đã chín (lúa, cà rốt, khoai tây, củ dền) & gieo lại hạt giống.",
                "   • Xẻng (Shovel) → Công binh & Kiến trúc sư: San phẳng đất, dọn đường, đào hầm và xây dựng công trình 3D bằng giàn giáo.",
                "   • Kiếm (Sword) → Vệ sĩ chiến đấu: Hỗ trợ cầm song kiếm 2 tay, tuần tra tiêu diệt quái vật và bảo vệ chủ nhân.",
                "2. LIÊN KẾT RƯƠNG & QUẢN LÝ KHO ĐỒ:",
                "   • Cầm Gậy Điều Khiển chọn NOVA → Nhấp phải vào Rương NOVA để đặt làm kho chính (bán kính làm việc quanh rương).",
                "   • Shift + Nhấp phải Gậy Điều Khiển vào rương thường hoặc nút bấm để cấp quyền sử dụng phụ (tối đa 9 vị trí).",
                "3. SẠC PIN, SỬA VỎ & LỆNH TRÒ CHUYỆN ĐỒNG LOẠT:",
                "   • Sạc Pin trực tiếp hoặc qua Omni-Pad: Bụi Đá Đỏ (+20% Pin & +2 HP), Khối Đá Đỏ (100% Pin & +6 HP).",
                "   • Sửa chữa thân vỏ hợp kim: Nhấp phải bằng Phôi Sắt (+5 HP) hoặc Phôi Đồng (+3 HP).",
                "   • Lệnh Chat Toàn Cục: Gõ /NOVA:[câu lệnh] (VD: /NOVA:làm việc, /NOVA:tập hợp, /NOVA:ngồi xuống) để ra lệnh đồng loạt!"
        };
        int maxW = measureMaxWidth(font, lines) + 16;
        int maxH = 22 + lines.length * 13;

        box.begin(g, x, y, w, h, maxW, maxH, 0xAA061B2E, 0xFF00D4FF);
        int ox = box.ox(), oy = box.oy();
        g.drawString(font, "CẨM NANG ĐIỀU KHIỂN & SỬ DỤNG ROBOT NOVA (CUỘN DỌC / NGANG ĐỂ XEM HẾT)", ox, oy, 0xFF00F2FF, false);

        int ly = oy + 15;
        for (String line : lines) {
            int col = (line.startsWith("1.") || line.startsWith("2.") || line.startsWith("3.")) ? 0xFF80E5FF : 0xFFE2F8FF;
            g.drawString(font, line, ox, ly, col, false);
            ly += 13;
        }
        box.end(g, mx, my);
    }

    public static void renderAboutTab(GuiGraphics g, Font font, int x, int y, int w, int h,
                                      HoloScrollBox box, int mx, int my) {
        String[] loreAndFeatures = {
                "✦ CỐT TRUYỆN & NGUỒN GỐC THẾ HỆ NOVA:",
                "  Được chế tạo từ hợp kim Silic bán dẫn và Tinh Thạch Năng Lượng kết tinh sâu trong lòng đất,",
                "  NOVA (Neural Operational Versatile Android) là thế hệ Robot đồng hành mang trí tuệ nhân tạo",
                "  và cảm xúc chân thật, luôn sát cánh cùng chủ nhân trong sinh tồn, khai khoáng, xây dựng và chiến đấu.",
                "✦ CÁC TÍNH NĂNG ĐỘC ĐÁO CỦA ROBOT NOVA MOD:",
                "  • Trí Tuệ Nhân Tạo Đa Nhiệm: Tự động chặt cây, trồng rừng, đào quặng, làm ruộng và tuần tra song kiếm.",
                "  • Kiến Trúc Sư Công Trình 3D: Quét công trình, xem trước mô hình 3D, tự bắc giàn giáo xây nhà trên cao & thu hồi giàn giáo.",
                "  • Biểu Cảm & Giọng Nói Sống Động: 40 khuôn mặt biểu cảm LED, 180 hiệu ứng âm thanh phân nhóm & AI trò chuyện 10 ngôn ngữ.",
                "  • Hệ Sinh Thái Công Nghiệp: Quặng Silic, Quặng Tinh Thạch Năng Lượng, 10 linh kiện bán dẫn & lắp ráp cột 3 khối kèm Rương NOVA.",
                "  • Bảng Điều Khiển Tối Cao (NOVA Omni-Pad): Giám sát toàn cầu, đặt tên trực tiếp, dịch chuyển lượng tử & sạc không dây!"
        };
        int maxW = Math.max(360, measureMaxWidth(font, loreAndFeatures) + 16);
        int maxH = 38 + loreAndFeatures.length * 13;

        box.begin(g, x, y, w, h, maxW, maxH, 0xAA061B2E, 0xFF00D4FF);
        int ox = box.ox(), oy = box.oy();

        drawItemSlot(g, font, new ItemStack(NovaMaterials.NOVA_OMNI_PAD), ox, oy, mx, my);
        drawItemSlot(g, font, new ItemStack(ModItems.NOVA_DORMANT), ox + 20, oy, mx, my);
        drawItemSlot(g, font, new ItemStack(NovaMaterials.NOVA_AI_PROCESSOR), ox + 40, oy, mx, my);

        g.drawString(font, "HỒ SƠ DỰ ÁN: ROBOT NOVA MOD (SCI-FI COMPANION)", ox + 64, oy + 1, 0xFF00F2FF, false);
        g.drawString(font, "Tác giả: Lê Thành Nguyễn  •  Phiên bản: 1.21.11 (Fabric)  •  Cấp quyền: Master Admin", ox + 64, oy + 11, 0xFFA5B4FC, false);

        int ly = oy + 26;
        for (String line : loreAndFeatures) {
            int col = line.startsWith("✦") ? 0xFF00F2FF : 0xFFE2F8FF;
            g.drawString(font, line, ox, ly, col, false);
            ly += 13;
        }
        box.end(g, mx, my);
    }

    private static int measureMaxWidth(Font font, String[] lines) {
        int max = 0;
        for (String s : lines) {
            max = Math.max(max, font.width(s));
        }
        return max;
    }

    public static void drawItemSlot(GuiGraphics g, Font font, ItemStack stack, int x, int y, int mx, int my) {
        g.fill(x, y, x + 18, y + 18, 0xBB051829);
        g.renderOutline(x, y, 18, 18, 0xFF2274A5);
        if (stack != null && !stack.isEmpty()) {
            g.renderItem(stack, x + 1, y + 1);
            if (stack.getCount() > 1) {
                String cnt = Integer.toString(stack.getCount());
                g.drawString(font, cnt, x + 17 - font.width(cnt), y + 9, 0xFFFFFFFF, true);
            }
            if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
                g.setTooltipForNextFrame(font, stack, mx, my);
            }
        }
    }

    public static void drawHologramBox(GuiGraphics g, int x, int y, int w, int h, int bgColor, int borderColor) {
        g.fill(x, y, x + w, y + h, bgColor);
        g.renderOutline(x, y, w, h, borderColor);
        g.fill(x, y, x + 5, y + 2, 0xFF00F2FF);
        g.fill(x + w - 5, y, x + w, y + 2, 0xFF00F2FF);
    }

    public static void drawFitted(GuiGraphics g, Font font, String text, int x, int y, int maxWidth, int color) {
        if (font.width(text) > maxWidth) {
            text = font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("…"))) + "…";
        }
        g.drawString(font, text, x, y, color, false);
    }
}
