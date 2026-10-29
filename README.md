# 🤖 Robot NOVA Mod — Minecraft 1.21.11 (Fabric)

**Robot NOVA Mod** (`robot-nova_mod`) là bản mod thêm người bạn đồng hành robot thông minh **NOVA** vào thế giới Minecraft **1.21.11** (sử dụng **Fabric API** & **GeckoLib**). NOVA không chỉ là một trợ lý tự động hóa công việc (Đào mỏ, Đốn củi & Trồng rừng, Làm nông, San lấp đất, Chiến đấu Song Kiếm) mà còn sở hữu hệ thống cảm xúc sống động (40 biểu cảm khuôn mặt), tính cách riêng, sổ tay kỷ niệm, tự sửa chữa bằng năng lượng, dọn đường thông minh và giao tiếp trực tiếp với người chơi bằng tiếng Việt.

---

## 📑 Mục lục

1. [💻 Yêu cầu hệ thống & Cài đặt](#1-yêu-cầu-hệ-thống--cài-đặt)
2. [⚡ Cách triệu hồi & Thuần hóa Robot NOVA](#2-cách-triệu-hồi--thuần-hóa-robot-nova)
3. [🧰 Danh sách Vật phẩm, Khối & Công cụ trong Mod](#3-danh-sách-vật-phẩm-khối--công-cụ-trong-mod)
4. [🔨 Công thức chế tạo (Crafting Recipes)](#4-công-thức-chế-tạo-crafting-recipes)
5. [🔋 Hệ thống Chỉ số, Năng lượng, Cấp độ & Tự sửa chữa](#5-hệ-thống-chỉ-số-năng-lượng-cấp-độ--tự-sửa-chữa)
6. [🎒 Giao diện Ba lô & Đai Công cụ của NOVA](#6-giao-diện-ba-lô--đai-công-cụ-của-nova)
7. [🔧 Chi tiết 5 Nghề nghiệp và Cơ chế Công cụ](#7-chi-tiết-5-nghề-nghiệp-và-cơ-chế-công-cụ)
   - [⚔️ Chiến binh NOVA (Cầm Kiếm / Song Kiếm)](#71-chiến-binh-nova-cầm-kiếm--song-kiếm)
   - [⛏️ Thợ mỏ NOVA (Cầm Cúp / Cuốc chim)](#72-thợ-mỏ-nova-cầm-cúp--cuốc-chim)
   - [🪓 Tiều phu NOVA (Cầm Rìu)](#73-tiều-phu-nova-cầm-rìu)
   - [🌾 Nông dân NOVA (Cầm Cuốc)](#74-nông-dân-nova-cầm-cuốc)
   - [🪏 Công nhân Đào đất NOVA (Cầm Xẻng)](#75-công-nhân-đào-đất-nova-cầm-xẻng)
8. [📡 Hướng dẫn sử dụng Gậy Điều Khiển NOVA](#8-hướng-dẫn-sử-dụng-gậy-điều-khiển-nova)
9. [📦 Hệ thống Rương NOVA, Rương phụ & Vận chuyển Rương](#9-hệ-thống-rương-nova-rương-phụ--vận-chuyển-rương)
10. [🎮 Bảng Điều Khiển, Biểu cảm, Tính cách & Trò chuyện](#10-bảng-điều-khiển-biểu-cảm-tính-cách--trò-chuyện)
11. [⚡ Bảng Tóm tắt Thao tác Nhanh (Quick Reference)](#11-bảng-tóm-tắt-thao-tác-nhanh-quick-reference)

---

<a id="1-yêu-cầu-hệ-thống--cài-đặt"></a>
## 1. Yêu cầu hệ thống & Cài đặt

- **Phiên bản Minecraft:** `1.21.11`
- **Mod Loader:** Fabric Loader (`>= 0.18.4`)
- **Thư viện bắt buộc:**
  - **Fabric API** (`0.141.3+1.21.11`)
  - **GeckoLib 5** (`5.4.5` cho Minecraft 1.21.11)
- **Ngôn ngữ hỗ trợ:** Tiếng Việt (`vi_vn`) và Tiếng Anh (`en_us`). Mọi thông báo trạng thái của NOVA đều hiển thị gọn gàng trên **Thanh hành động (Action Bar)** phía trên thanh công cụ để không gây trôi khung chat.

---

<a id="2-cách-triệu-hồi--thuần-hóa-robot-nova"></a>
## 2. Cách triệu hồi & Thuần hóa Robot NOVA

### 2.1. Xây dựng cấu trúc triệu hồi (Giống cách gọi Golem)

Bạn có thể triệu hồi NOVA bằng **Trứng triệu hồi NOVA** (`nova_spawn_egg`) ở chế độ Sáng tạo, hoặc **lắp ghép cấu trúc 5 khối** trong chế độ Sinh tồn theo đúng thứ tự sau:

1. **Thân giữa (Dưới cùng):** Đặt **1 Khối Sắt** (`Iron Block`).
2. **Hai cánh tay (Hai bên trái & phải của Khối Sắt):** Đặt **2 Khối Đá Đỏ** (`Redstone Block`) ở hai mặt ngang đối diện nhau của Khối Sắt.
3. **Ba lô năng lượng (Phía sau lưng Khối Sắt):** Đặt **1 Khối Lưu Ly** (`Lapis Lazuli Block`) gắn vào mặt sau của Khối Sắt.
4. **Đầu Robot (Đặt cuối cùng):** Đặt **1 Bí Ngô Khắc** (`Carved Pumpkin`) hoặc **Đèn Bí Ngô** (`Jack o'Lantern`) **lên đỉnh Khối Sắt** (hướng mặt bí ngô quay về phía trước — ngược hướng với Khối Lưu Ly sau lưng).

```text
      [Bí Ngô Khắc]          <-- Đặt cuối cùng ở trên đỉnh Khối Sắt
[Redstone] [Khối Sắt] [Redstone]  <-- Hai tay Redstone hai bên
         [Khối Lưu Ly]       <-- Đặt ở mặt sau của Khối Sắt (làm Ba lô)
```

> **Lưu ý:** Ngay khi đặt Bí Ngô Khắc đúng hướng, 5 khối sẽ biến mất cùng hiệu ứng tia lửa điện và **Robot NOVA** sẽ khởi động!

### 2.2. Thuần hóa (Taming)

- Khi mới được triệu hồi, NOVA chưa có chủ nhân.
- **Cách thuần hóa:** Cầm **Bột Đá Đỏ (`Redstone Dust`)** trên tay và **Nhấp chuột phải** vào NOVA (không giữ `Shift`).
- Tỷ lệ thuần hóa thành công là **1/3 (33%)** mỗi viên Redstone. Khi thành công, NOVA sẽ phát âm thanh vui mừng, thả tim và ghi nhớ tên bạn vào nhật ký kỷ niệm.

---

<a id="3-danh-sách-vật-phẩm-khối--công-cụ-trong-mod"></a>
## 3. Danh sách Vật phẩm, Khối & Công cụ trong Mod

Tất cả vật phẩm của mod nằm trong tab **Công cụ & Tiện ích (Tools & Utilities)** và **Trứng sinh ra (Spawn Eggs)**:

| Tên tiếng Việt | ID vật phẩm / khối | Loại | Mô tả & Công dụng chính |
| :--- | :--- | :--- | :--- |
| **Gậy điều khiển NOVA** | `robot-nova_mod:nova_control_rod` | Vật phẩm & Khối | Trung tâm chỉ huy NOVA: Mở bảng điều khiển, chọn robot, liên kết rương, thu hồi công cụ, cấp quyền rương phụ/nút bấm, làm cột mốc cho NOVA Chiến binh và triệu tập đội hình (Rally). |
| **Rương NOVA** | `robot-nova_mod:nova_chest` | Khối chứa đồ | Rương trung tâm (27 ô) dùng để liên kết 1-1 với một Robot NOVA. Là tâm điểm bán kính làm việc (16 khối) và nơi NOVA tự cất đồ, lấy công cụ mới hoặc được bê đi theo chủ. |
| **Kiếm NOVA** | `robot-nova_mod:nova_sword` | Vũ khí | Kiếm chuyên dụng của NOVA (chất liệu Đồng tinh luyện, sát thương +3, tốc độ đánh -2.4). Có thể cầm **Song Kiếm 2 tay**. |
| **Cuốc chim NOVA** | `robot-nova_mod:nova_pickaxe` | Công cụ | Cúp chuyên dụng cho nghề Thợ mỏ NOVA (sát thương +1, tốc độ -2.8). |
| **Rìu NOVA** | `robot-nova_mod:nova_axe` | Công cụ | Rìu chuyên dụng cho nghề Tiều phu NOVA (sát thương +7, tốc độ -3.2). |
| **Cuốc NOVA** | `robot-nova_mod:nova_hoe` | Công cụ | Cuốc chuyên dụng cho nghề Nông dân NOVA (tốc độ -2.0). |
| **Xẻng NOVA** | `robot-nova_mod:nova_shovel` | Công cụ | Xẻng chuyên dụng cho nghề Đào đất NOVA (sát thương +1.5, tốc độ -3.0). |
| **Trứng triệu hồi NOVA** | `robot-nova_mod:nova_spawn_egg` | Vật phẩm | Sinh ra ngay một Robot NOVA tại vị trí chỉ định. |

> **💡 Mẹo:** Ngoài bộ công cụ NOVA chuyên dụng, Robot NOVA **tương thích hoàn toàn với mọi công cụ và vũ khí chuẩn của Minecraft** (Gỗ, Đá, Đồng, Sắt, Vàng, Kim cương, Netherite) thuộc nhóm Kiếm, Cúp, Rìu, Cuốc, Xẻng!

---

<a id="4-công-thức-chế-tạo-crafting-recipes"></a>
## 4. Công thức chế tạo (Crafting Recipes)

### 4.1. Gậy điều khiển NOVA (`nova_control_rod`)

Chế tạo trên Bàn chế tạo (3 hàng dọc):

- **Hàng trên:** 1 Ngọc Lưu Ly (`Lapis Lazuli`)
- **Hàng giữa:** 1 Bột Đá Đỏ (`Redstone Dust`)
- **Hàng dưới:** 1 Cột Thu Lôi (`Lightning Rod`)

```text
[   ] [ Lưu Ly ] [   ]
[   ] [Redstone] [   ]
[   ] [Thu Lôi ] [   ]
```

### 4.2. Rương NOVA (`nova_chest`)

Chế tạo trên Bàn chế tạo (3x3):

- **`I`**: Phôi Sắt (`Iron Ingot`) — 4 góc
- **`C`**: Phôi Đồng (`Copper Ingot`) — Giữa hàng trên & Giữa hàng dưới
- **`R`**: Bột Đá Đỏ (`Redstone Dust`) — Trái & Phải hàng giữa
- **`H`**: Rương gỗ thường (`Chest`) — Chính giữa

```text
[ Phôi Sắt ] [Phôi Đồng ] [ Phôi Sắt ]
[ Redstone ] [Rương Gỗ  ] [ Redstone ]
[ Phôi Sắt ] [Phôi Đồng ] [ Phôi Sắt ]
```

### 4.3. Bộ 5 Công cụ Chuyên dụng NOVA (Đồng - Copper Tier)

Chế tạo trên Bàn chế tạo theo khuôn công cụ tiêu chuẩn, sử dụng **Phôi Đồng (`Copper Ingot`)** và **Que Gỗ (`Stick`)**:

- **Kiếm NOVA (`nova_sword`):** 2 Phôi Đồng + 1 Que Gỗ (hàng dọc).
- **Cuốc chim NOVA (`nova_pickaxe`):** 3 Phôi Đồng (`Copper Ingot`) hoặc 3 Hạt Đồng (`Copper Nugget`) (hàng ngang trên) + 2 Que Gỗ (cột giữa). Ngoài ra có thể nâng cấp trực tiếp: 1 Cuốc chim đồng (`Copper Pickaxe`) + 1 Bột Đá Đỏ (`Redstone`).
- **Rìu NOVA (`nova_axe`):** 3 Phôi Đồng + 2 Que Gỗ (khuôn rìu chuẩn).
- **Xẻng NOVA (`nova_shovel`):** 1 Phôi Đồng + 2 Que Gỗ (hàng dọc).
- **Cuốc NOVA (`nova_hoe`):** 2 Phôi Đồng + 2 Que Gỗ (khuôn cuốc chuẩn).

### 4.4. NOVA Chưa Kích Hoạt (`nova_dormant`)

Chế tạo trên Bàn chế tạo (thu nhỏ cấu trúc triệu hồi 5 khối vào ô chế tạo 3x3):

- **Hàng trên (Giữa):** 1 Bí Ngô Khắc (`Carved Pumpkin`) hoặc Đèn Bí Ngô (`Jack o'Lantern`).
- **Hàng giữa:** 1 Khối Đá Đỏ (`Redstone Block`) — 1 Khối Sắt (`Iron Block`) — 1 Khối Đá Đỏ (`Redstone Block`).
- **Hàng dưới (Giữa):** 1 Khối Lưu Ly (`Lapis Lazuli Block`).

```text
[        ] [  Bí Ngô Khắc  ] [        ]
[Redstone] [   Khối Sắt    ] [Redstone]
[        ] [ Khối Lưu Ly   ] [        ]
```

Đặt vật phẩm xuống đất sẽ xuất hiện một chú **NOVA Chưa Kích Hoạt** ở chế độ ngủ đông. Cầm **Khối Đá Đỏ** nhấp chuột phải vào tượng để kích hoạt NOVA sống dậy hoạt động!

---

<a id="5-hệ-thống-chỉ-số-năng-lượng-cấp-độ--tự-sửa-chữa"></a>
## 5. Hệ thống Chỉ số, Năng lượng, Cấp độ & Tự sửa chữa

### 5.1. Chỉ số cơ bản của NOVA

- **Máu tối đa (Health):** `20.0 HP` (10 tim)
- **Giáp cơ bản (Armor):** `4.0`
- **Tốc độ di chuyển:** `0.30` (Tự động tăng tốc chạy `1.45x` khi cách xa chủ nhân trên 7 khối hoặc khi truy đuổi mục tiêu)
- **Sức tấn công tay không:** `3.0` (Tăng theo vũ khí và cấp độ kỹ năng khi cầm kiếm)
- **Kháng sát thương:** Miễn nhiễm sát thương từ Xương rồng (`Cactus`) và Bụi quả ngọt (`Sweet Berry Bush`) để làm việc an toàn trong nông trại.

### 5.2. Hệ thống Pin / Năng lượng (Energy)

- **Dung lượng pin tối đa:** `1000` đơn vị (`100%`).
- **Tiêu hao năng lượng:**
  - Chỉ tiêu hao khi NOVA **đang thực hiện thao tác làm việc** (`1 năng lượng / giây`) hoặc khi đốn hạ cây (`1 năng lượng / khối gỗ`).
  - **Không tốn pin** khi đứng nghỉ, ngồi chờ hay đi theo chủ nhân.
  - Khi pin xuống dưới **10% (`<= 100`)**, NOVA sẽ báo hiệu cần sạc pin và tạm nghỉ làm việc (hoặc quay về rương nghỉ ngơi).
- **Cách sạc pin:**
  - Cầm **Bột Đá Đỏ (`Redstone Dust`)** nhấp chuột phải vào NOVA (không giữ `Shift`).
  - Mỗi viên Redstone sạc thêm **`+200 năng lượng` (`+20%` pin)**.

### 5.3. Cơ chế Tự sửa chữa (Self-Repair)

- Sau khi không nhận sát thương trong **10 giây (`200 ticks`)**, nếu máu chưa đầy và pin còn trên **11% (`>= 110 năng lượng`)**, NOVA sẽ tự động kích hoạt mạch tự sửa chữa.
- Cứ mỗi **5 giây (`100 ticks`)**, NOVA tiêu hao **1% pin (`10 năng lượng`)** để hồi phục **`+1.0 HP`**, kèm hiệu ứng âm thanh sửa chữa cơ khí.

### 5.4. Hệ thống Cấp độ Kỹ năng (Skill Level 1 → 5)

NOVA tích lũy kinh nghiệm (EXP) khi làm việc và chiến đấu:

- **Đào quặng:** Than (`+1`), Sắt/Đồng (`+2`), Vàng/Redstone/Lưu ly (`+4`), Kim cương/Ngọc lục bảo (`+8`), Mảnh vỡ cổ đại (`+10`).
- **Đốn gỗ / Trồng cây / Thu hoạch nông sản:** `+1 EXP` mỗi lần.
- **Chiến đấu (Mỗi đòn đánh trúng mục tiêu):** `+3 EXP`.

**Mốc EXP lên cấp & Quyền lợi:**

| Cấp độ | EXP cần để lên cấp tiếp theo | Quyền lợi vượt trội |
| :---: | :---: | :--- |
| **Lv. 1** | `50 EXP` | Tốc độ làm việc tiêu chuẩn (`1.0x`), sát thương chuẩn. |
| **Lv. 2** | `150 EXP` | Tốc độ đào/chặt/xúc nhanh hơn (`+15%`), sát thương chiến đấu `+0.75`. |
| **Lv. 3** | `300 EXP` | Tốc độ làm việc `+30%`, sát thương `+1.5`, **Mở khóa đặt đuốc tự động khi đào mỏ**. |
| **Lv. 4** | `500 EXP` | Tốc độ làm việc `+45%`, sát thương `+2.25`. |
| **Lv. 5 (Tối đa)** | `MAX` | Tốc độ làm việc `+60%` (`1.6x`), sát thương `+3.0`, **Mở khóa kỹ năng Đốn hạ toàn bộ thân cây cùng lúc (Tree Feller)** khi cầm Rìu! |

---

<a id="6-giao-diện-ba-lô--đai-công-cụ-của-nova"></a>
## 6. Giao diện Ba lô & Đai Công cụ của NOVA

### Cách mở giao diện Ba lô

- Đứng gần NOVA (trong phạm vi 8 khối) và nhấn **`Shift + Nhấp chuột phải`** vào NOVA (khi không cầm Gậy điều khiển), **HOẶC** nhấp chuột phải tay không vào NOVA.
- Khi đang mở ba lô, NOVA sẽ tạm dừng di chuyển và nhìn về phía bạn để bạn dễ thao tác.

### Cấu trúc giao diện Ba lô

1. **Khu vực Thông tin (Phía trên):**
   - Hiển thị mô hình 3D trực tiếp của NOVA, thanh **Máu (HP)**, thanh **Pin (Energy)** và thanh **Cấp độ Kỹ năng (Lv. 1–5 + tiến độ EXP)**.
2. **Kho chứa đồ 27 ô (Ba lô chính ở giữa):**
   - Tương đương 1 rương đơn (`3 hàng x 9 cột`). Chứa toàn bộ tài nguyên NOVA thu hoạch được hoặc vật tư dự trữ (hạt giống, cây non, đuốc, công cụ/vũ khí dự phòng).
3. **Cột Trang bị & Đai Công cụ (6 ô bên phải, cuộn bằng con lăn chuột):**
   - **Ô số 1 (Trên cùng - Tay chính `MAINHAND`):** **Ô Quyết định Nghề nghiệp!** Đặt công cụ nào vào ô này, NOVA sẽ nhận nghề nghiệp tương ứng với công cụ đó.
   - **Ô số 2, 3, 4, 5 (4 Ô Đai công cụ dự phòng `Spare Tools`):**
     - Dùng để chứa công cụ dự phòng cùng loại (để NOVA tự động thay thế khi công cụ chính hỏng), **VÀ** chứa công cụ khác loại (Rìu, Cúp, Xẻng) để NOVA **tự động mượn tạm khi cần dọn vật cản trên đường đi** rồi tự trả về chỗ cũ!
     - Bạn có thể bấm trực tiếp vào các ô dự phòng này trên giao diện để hoán đổi nhanh với công cụ đang cầm ở Ô số 1.
   - **Ô số 6 (Dưới cùng - Tay phụ `OFFHAND`):**
     - Đối với **NOVA Chiến binh (Ô 1 là Kiếm):** Cho phép đặt **cây kiếm thứ 2** vào ô này để kích hoạt chế độ **Song Kiếm (Dual-Wield)**!
     - Đối với **các nghề lao động khác:** Chứa vật tư phụ trợ gồm **Đuốc (`Torch`)**, **Cây non (`Sapling`)**, hoặc **Hạt giống (`Wheat Seeds`, `Beetroot Seeds`, `Carrot`, `Potato`)**.

---

<a id="7-chi-tiết-5-nghề-nghiệp-và-cơ-chế-công-cụ"></a>
## 7. Chi tiết 5 Nghề nghiệp và Cơ chế Công cụ

Nghề nghiệp của NOVA được xác định tự động dựa trên **vật phẩm nằm ở Ô công cụ đầu tiên (Tay chính - `MAINHAND`)**. Để NOVA bắt đầu làm việc, hãy mở Bảng điều khiển bằng Gậy điều khiển và bật **`Giao việc: [Bật]`**.

---

<a id="71-chiến-binh-nova-cầm-kiếm--song-kiếm"></a>
### 7.1. Chiến binh NOVA (Cầm Kiếm / Song Kiếm)

Khi bạn đặt **bất kỳ thanh Kiếm nào vào Ô công cụ đầu tiên (`MAINHAND`)**, NOVA lập tức chuyển thành **NOVA Chiến binh (`Sword Nova`)** với cơ chế hoạt động hoàn toàn độc lập:

- **Đặc điểm riêng của NOVA Chiến binh:**
  - **Không liên kết với Rương NOVA**, không nhấc rương, không cất đồ vào rương.
  - Nếu trước đó NOVA đang liên kết với một rương, ngay khi cầm kiếm vào Ô 1, NOVA sẽ **tự động hủy liên kết rương** để nhường rương đó cho các robot lao động khác.
  - **Tâm hoạt động:** Hoạt động xoay quanh **Gậy điều khiển NOVA (`nova_control_rod`) được cắm dưới đất** (trong phạm vi 32 khối), hoặc xoay quanh **Chủ nhân / Vị trí đứng hiện tại** nếu không cắm gậy.
- **Cơ chế Song Kiếm (Dual-Wielding 2 Tay):**
  - NOVA Chiến binh có thể cầm **2 thanh kiếm trên cả 2 tay cùng lúc**!
  - Nếu bạn đặt 1 thanh kiếm ở Ô 1 và trong ba lô/đai dự phòng có thêm kiếm, NOVA sẽ **tự động rút thanh kiếm thứ 2 sang Tay phụ (`OFFHAND`)**. Bạn cũng có thể đặt trực tiếp kiếm thứ 2 vào ô Tay phụ hoặc `Shift + Click` kiếm vào giao diện.
  - **Hiệu quả khi cầm Song Kiếm:**
    - Tăng thêm **`+50%` sát thương** từ thanh kiếm tay phụ cộng dồn vào đòn đánh.
    - Giảm thời gian hồi chiêu giữa các đòn chém từ **`16 ticks` xuống còn `10 ticks`** (tốc độ ra đòn nhanh vượt trội).
    - Vung luân phiên cả tay phải và tay trái khi cận chiến, tự động chia đều hao mòn độ bền giữa 2 thanh kiếm.
- **3 Cơ chế Chiến đấu Kết hợp (Hoạt động đồng thời theo thứ tự ưu tiên):**
  1. **🛡️ Bảo vệ Chủ nhân (Ưu tiên số 1 - Giống cơ chế Chó Sói):**
     - Ngay lập tức lao vào tấn công kẻ địch đang đánh chủ nhân hoặc mục tiêu mà chủ nhân vừa tấn công (chỉ cần bật Giao việc HOẶC đang ở chế độ Đi theo).
  2. **🚨 Tuần tra & Tự vệ (Ưu tiên số 2):**
     - Tự động phát hiện và tiêu diệt mọi quái vật thù địch (`Enemy` / `Monster` như Zombie, Skeleton, Spider; tự động bỏ qua Creeper và Ghast để tránh nổ/bay cao) xâm nhập bán kính **16 khối** quanh Chủ nhân, quanh Cột Gậy điều khiển, hoặc quanh bản thân NOVA.
  3. **🏹 Đi săn (Ưu tiên số 3):**
     - Khi bật Giao việc ở chế độ Tự do, NOVA chủ động tuần tra quanh khu vực Gậy điều khiển (hoặc quanh chủ nhân) để săn quái vật và động vật hoang dã (`Chicken`, `Pig`, `Cow`, `Sheep`, `Rabbit`) nhằm thu thập tài nguyên và tích lũy EXP chiến đấu (tự động bỏ qua thú nuôi và con non).
- **Cơ chế Tự thay kiếm & Báo hỏng vũ khí:**
  - Khi 1 trong 2 thanh kiếm trên tay bị mòn hết độ bền, NOVA tự động lấy kiếm mới từ **4 ô dự phòng** hoặc **27 ô ba lô** để lắp vào tay.
  - Khi **toàn bộ kiếm trên 2 tay và trong ba lô đều hỏng hết**, NOVA sẽ ngừng chiến đấu, **chạy thẳng về phía chủ nhân** và thông báo:
    > *"Mình bị hư hết vũ khí rồi, cấp vũ khí cho mình đi!"*

---

<a id="72-thợ-mỏ-nova-cầm-cúp--cuốc-chim"></a>
### 7.2. Thợ mỏ NOVA (Cầm Cúp / Cuốc chim)

- **Yêu cầu:** Cầm **Cúp (`Pickaxe`)** ở Ô 1 + Đã liên kết với **1 Rương NOVA** + Bật **Giao việc**.
- **Phạm vi làm việc:** Bán kính **16 khối** ngang, từ độ cao `+4` đến `-16` khối tính từ Rương NOVA.
- **Cơ chế Khai thác Thông minh:**
  - **Không đào phá bề mặt ngẫu nhiên:** NOVA sẽ tự động dò tìm **hang động tự nhiên hoặc hầm mỏ** gần rương trước.
  - **Ưu tiên mạch quặng (Vein Mining):** Khi phát hiện bất kỳ khối quặng nào (Than, Đồng, Sắt, Vàng, Redstone, Lưu ly, Kim cương, Ngọc lục bảo, Thạch anh, Mảnh vỡ cổ đại...), NOVA sẽ đào sạch toàn bộ các khối quặng nối liền nhau trong mạch trước khi đào tiếp đá thường.
  - **Bảo vệ công trình:** Không bao giờ đào phá các khối gạch nhân tạo, tường nhà hay khu vực sát rương liên kết (`<= 2 khối`).
- **Tự động cắm Đuốc (Từ Cấp độ 3 trở lên):**
  - Nếu có **Đuốc (`Torch`)** ở ô Tay phụ hoặc trong Ba lô, NOVA tự động rút đuốc sang tay phụ và cắm đuốc chiếu sáng khi độ sáng dưới chân xuống thấp (`Block Light < 5`), đảm bảo các cây đuốc cách nhau tối thiểu 5 khối.

---

<a id="73-tiều-phu-nova-cầm-rìu"></a>
### 7.3. Tiều phu NOVA (Cầm Rìu)

- **Yêu cầu:** Cầm **Rìu (`Axe`)** ở Ô 1 + Đã liên kết với **1 Rương NOVA** + Bật **Giao việc**.
- **Phạm vi làm việc:** Bán kính **16 khối** quanh Rương NOVA.
- **Cơ chế Đốn cây & Trồng rừng bền vững:**
  - Tự động nhận diện cây tự nhiên (có thân gỗ nối với tán lá cây, tránh phá gỗ nhà cửa của người chơi).
  - **Cấp 1 – 4:** Chặt từng khúc gỗ từ gốc lên ngọn.
  - **Cấp 5 (Đốn cây thần tốc - Tree Feller):** Chặt 1 gốc cây sẽ đốn hạ toàn bộ thân cây (tối đa 64 khối gỗ) cùng lúc!
  - **Tự động nhặt đồ & Trồng lại cây non (`Sapling`):**
    - Sau khi đốn cây, NOVA tự động phá lá vướng víu, hút toàn bộ Gỗ, Táo, Que củi và Cây non rơi ra vào ba lô.
    - Tự động chuyển đúng loại **Cây non (`Sapling`)** vừa nhặt được sang ô Tay phụ và **trồng lại ngay tại các vị trí gốc cây vừa chặt**!

---

<a id="74-nông-dân-nova-cầm-cuốc"></a>
### 7.4. Nông dân NOVA (Cầm Cuốc)

- **Yêu cầu:** Cầm **Cuốc (`Hoe`)** ở Ô 1 + Đã liên kết với **1 Rương NOVA** + Bật **Giao việc**.
- **Phạm vi làm việc:** Bán kính **16 khối** quanh Rương NOVA.
- **Cơ chế Làm nông Khép kín:**
  1. **Xới đất (`Till Soil`):** Tự động tìm các khối Đất/Cỏ nằm trong phạm vi 4 khối quanh nguồn nước và xới thành **Đất ruộng (`Farmland`)**.
  2. **Tự động Gieo hạt (`Planting`):** Tự động rút hạt giống (`Hạt lúa mì`, `Hạt củ dền`, `Cà rốt`, `Khoai tây`) từ Ba lô sang ô Tay phụ (`OFFHAND`) và gieo lên các ô đất ruộng trống.
  3. **Thu hoạch & Trồng lại ngay (`Harvest & Replant`):** Khi cây trồng chín tối đa (`Max Age`), NOVA tự động thu hoạch nông sản vào ba lô và gieo lại hạt giống mới ngay lập tức trên ô đất đó.

---

<a id="75-công-nhân-đào-đất-nova-cầm-xẻng"></a>
### 7.5. Công nhân Đào đất NOVA (Cầm Xẻng)

- **Yêu cầu:** Cầm **Xẻng (`Shovel`)** ở Ô 1 + Đã liên kết với **1 Rương NOVA** + Bật **Giao việc**.
- **Phạm vi làm việc:** Bán kính **16 khối** quanh Rương NOVA.
- **Cơ chế San lấp theo Khuôn 5x5:**
  - Khai thác gọn gàng các khối mềm (Đất, Cỏ, Cát, Sỏi, Đất sét, Bùn...) theo từng khu vực **5x5 khối** từ lớp trên xuống lớp dưới.
  - Tự động chừa lại lối đứng quanh Rương NOVA và tự động dịch chuyển tâm khu vực 5x5 sang vùng đất kế tiếp khi đã đào xong khu vực hiện tại.

---

### 🛤️ Cơ chế Dọn đường Tự động (Smart Path Clearing) & Tự thay Công cụ

- **Mượn công cụ dọn đường:** Khi đang đi làm việc mà bị kẹt bởi lá cây, gỗ, đất hoặc đá chắn ngang đường, NOVA sẽ tự động kiểm tra **4 ô công cụ dự phòng** trong đai. Nếu có Rìu/Cúp/Xẻng phù hợp, NOVA sẽ tạm mượn công cụ đó để phá khối cản đường rồi **tự động trả lại công cụ chính về tay phải**.
- **Tự động thay công cụ hỏng:**
  1. Khi công cụ chính gần hỏng, NOVA ưu tiên lấy công cụ cùng loại còn tốt từ **4 ô dự phòng (`Spare Tools`)** hoặc **27 ô Ba lô (`Cargo`)**.
  2. Nếu trong người hết công cụ dự phòng, NOVA sẽ tự chạy về **Rương NOVA liên kết** để tìm và lấy công cụ mới cùng loại!
  3. Nếu trong rương cũng hết công cụ, NOVA sẽ chạy về gần chủ nhân và báo hiệu cụ thể loại công cụ cần cấp thêm.

---

<a id="8-hướng-dẫn-sử-dụng-gậy-điều-khiển-nova"></a>
## 8. Hướng dẫn sử dụng Gậy Điều Khiển NOVA

**Gậy điều khiển NOVA** là công cụ đa năng nhất trong mod với **6 cách tương tác** khác nhau:

1. **Mở Bảng Điều Khiển Robot (`Nhấp chuột trái / Đánh nhẹ vào NOVA`):**
   - Cầm Gậy điều khiển trên tay chính, **nhấp chuột trái** vào NOVA của bạn (không gây sát thương) để mở **Bảng Điều Khiển NOVA**.
2. **Thu hồi nhanh toàn bộ công cụ (`Shift + Nhấp chuột trái vào NOVA`):**
   - Cầm Gậy điều khiển, giữ **`Shift` + Nhấp chuột trái** vào NOVA để thu hồi ngay lập tức công cụ trên tay chính và 4 ô dự phòng về túi đồ của bạn.
3. **Chọn NOVA để Liên kết Rương (`Nhấp chuột phải vào NOVA`):**
   - Cầm Gậy điều khiển, **nhấp chuột phải** vào NOVA để chọn robot đó (có hiệu lực trong 60 giây).
   - Nếu là NOVA Chiến binh (cầm kiếm), NOVA sẽ báo: *"Mình là NOVA chiến đấu nên không liên kết với rương."*
   - Nếu NOVA đã có rương liên kết rồi, NOVA sẽ nhắc: *"Mình đã liên kết với rương rồi."* để tránh bạn liên kết nhầm làm mất rương cũ.
4. **Hoàn tất Liên kết với Rương NOVA (`Nhấp chuột phải vào Rương NOVA`):**
   - Sau khi chọn một NOVA chưa có rương, **nhấp chuột phải** (hoặc `Shift + Nhấp chuột phải`) vào **Rương NOVA** trong vòng 60 giây.
   - **Chống liên kết trùng:** Mỗi Rương NOVA chỉ cho phép **1 Robot NOVA duy nhất** liên kết chính. Nếu rương đó đã có robot khác liên kết, hệ thống sẽ báo rõ tên robot đang sở hữu rương và chặn liên kết đè.
5. **Cấp quyền Rương phụ & Nút bấm (`Shift + Nhấp chuột phải vào Rương thường / Nút bấm`):**
   - Chọn NOVA bằng Gậy điều khiển trước, sau đó giữ **`Shift` + Nhấp chuột phải** vào **Rương gỗ thường (`Chest` / `Trapped Chest`)** hoặc **Nút bấm (`Button`)** trong phạm vi 8 khối.
   - Mỗi NOVA có thể ghi nhớ tối đa **9 Rương phụ** (để cất đồ khi Rương NOVA chính bị đầy) và **9 Nút bấm** được phép tương tác. Làm lại thao tác trên để hủy quyền.
6. **Cắm Gậy xuống đất làm Trạm Tập Hợp Đội Hình (`Rally Point`):**
   - Nhấp chuột phải lên mặt trên của một khối cứng để **đặt Gậy điều khiển NOVA xuống đất**.
   - Ngay khi đặt xuống (hoặc khi nhấp chuột phải vào cây gậy đã đặt), còi tập hợp sẽ vang lên và **tất cả Robot NOVA của bạn trong bán kính 64 khối** sẽ xếp đội hình quân đội ngay ngắn trước mặt cây gậy (có Đội trưởng hiển thị tổng quân số)!
   - Nhấp chuột phải lần nữa vào cây gậy đã đặt (hoặc đập vỡ cây gậy) để **Giải tán đội hình**, đưa các robot trở về chế độ làm việc trước đó.

---

<a id="9-hệ-thống-rương-nova-rương-phụ--vận-chuyển-rương"></a>
## 9. Hệ thống Rương NOVA, Rương phụ & Vận chuyển Rương

### 9.1. Tự động Cất đồ thông minh

- Khi Ba lô 27 ô của NOVA đầy (hoặc khi kết thúc phiên làm việc), NOVA sẽ tự động chạy về **Rương NOVA liên kết chính** (hoặc **9 Rương phụ** đã được cấp quyền nếu rương chính đầy) để cất đồ.
- **Bộ lọc thông minh:** Khi cất đồ vào rương, NOVA sẽ **tự động giữ lại trong ba lô** các công cụ dự phòng, đuốc (cho Thợ mỏ) và số lượng cây non cần thiết (cho Tiều phu đang chờ trồng lại rừng), đồng thời tự nạp đầy vật tư vào ô Tay phụ trước khi cất phần dư thừa.

### 9.2. Ra lệnh cho NOVA Bê Rương đi theo bạn (Carry & Fetch Chest)

Khi bạn muốn di chuyển căn cứ hoặc mang theo kho đồ di động:

1. **Cách 1 (Khi NOVA đang đứng cạnh rương):** Nhấn **`Shift + Nhấp chuột phải`** (tay không hoặc vật phẩm thường) vào **Rương NOVA** đã liên kết. NOVA sẽ nhấc bổng cả chiếc rương (giữ nguyên toàn bộ vật phẩm bên trong) lên hai tay và đi theo bạn!
2. **Cách 2 (Gọi NOVA từ xa chạy lại nhấc rương):** Cầm **Gậy điều khiển**, nhấp chuột phải vào NOVA để chọn, sau đó lại gần Rương NOVA của robot đó và nhấn **`Shift + Nhấp chuột phải`**. Dù NOVA đang ở cách xa tới 48 khối, robot sẽ lập tức chạy tới vị trí rương, tự nhấc rương lên và chạy theo bạn!
3. **Cách đặt rương xuống:** Khi NOVA đang bê rương trên tay, chỉ cần nhấn **`Shift + Nhấp chuột phải`** vào người NOVA, robot sẽ tự tìm vị trí trống ngay cạnh để đặt Rương NOVA xuống đất và **tự động khôi phục liên kết** với chiếc rương đó.

---

<a id="10-bảng-điều-khiển-biểu-cảm-tính-cách--trò-chuyện"></a>
## 10. Bảng Điều Khiển, Biểu cảm, Tính cách & Trò chuyện

Khi cầm **Gậy điều khiển NOVA** và **Nhấp chuột trái** vào NOVA, màn hình **Bảng Điều Khiển (`NOVA • Bảng Điều Khiển`)** sẽ mở ra với các tính năng:

1. **Ra lệnh (`Commands`):**
   - **Đi theo (`Follow`):** Luôn bám sát chủ nhân (tự chạy nhanh khi cách xa).
   - **Tự do (`Free`):** Tự do đi lại quanh khu vực hoặc thực hiện công việc đã giao.
   - **Đứng chờ (`Wait`):** Đứng yên tại vị trí hiện tại.
   - **Ngồi / Đứng dậy (`Sit / Stand`):** Cho phép NOVA ngồi nghỉ tại chỗ.
2. **Giao việc (`[Bật] / [Tắt]`):**
   - Bật/tắt chế độ tự động làm việc theo công cụ ở Ô số 1. Khi bật, hệ thống sẽ kiểm tra ngay công cụ, rương liên kết và báo trạng thái cụ thể của nghề nghiệp.
3. **Biểu cảm (`Expressions` - 40 Khuôn mặt LED):**
   - Mở giao diện xem trước trực tiếp (không làm mờ màn hình) để bạn chuyển qua lại giữa **40 biểu cảm khuôn mặt (`0 -> 39`)** của NOVA hoặc nhấn **Đặt lại** để robot tự đổi biểu cảm theo cảm xúc tự nhiên.
4. **Hành động (`Actions`):**
   - Ra lệnh cho NOVA thực hiện các hoạt ảnh đặc biệt kèm hiệu ứng âm thanh: **Chào (`Wave`)**, **Ăn mừng (`Celebrate`)**, **Xin lỗi (`Apologize`)**, **Gật đầu (`Nod`)**, **Tự hào (`Proud`)**.
5. **Trò chuyện (`Chat`):**
   - **Cách 1 — Qua Giao diện riêng (GUI):** Mở Bảng điều khiển bằng Gậy điều khiển, bấm vào nút **Trò chuyện** để mở cửa sổ nhắn tin riêng với các nút hỏi nhanh (**Pin**, **Công việc**, **Kỷ niệm**, **Gợi ý**).
   - **Cách 2 — Gọi tên & Ra lệnh trực tiếp trên Khung Chat chính của Minecraft (Phím `T`):**
     - **Gọi theo Tên riêng (Thẻ tên - `Name Tag`):** Nếu robot đã được đặt tên riêng (ví dụ: *Chiến Binh*, *Thợ Mỏ*, *Vệ Sĩ*...), bạn chỉ cần gõ tên robot kèm yêu cầu (ví dụ: `Chiến Binh đi theo`, `Thợ Mỏ đứng chờ`, `Chiến Binh bật làm việc`, `Vệ Sĩ ơi`).
     - **Gọi theo Số thứ tự:** Khi có nhiều robot ở gần, bạn có thể gọi `Nova 1 đi theo`, `Nova 2 đứng chờ`, `Robot 1 hỏi pin` (thứ tự tự động sắp xếp theo khoảng cách gần nhất).
     - **Gọi chung:** Gõ `Nova ơi`, `Nova đi theo`, `Robot ơi` để gọi robot gần nhất (hoặc robot đang trò chuyện dở).
     - **Trò chuyện tiếp diễn (Follow-up 45s):** Sau khi được gọi tên, robot sẽ mở một phiên trò chuyện riêng và ghi nhớ trong **45 giây**. Các câu thoại tiếp theo (ví dụ: `đi theo`, `đứng lại`, `ngồi`, `pin`, `kể kỷ niệm đi`) sẽ tự động được gửi tới đúng robot đó mà bạn **không cần gõ lại tên** ở mỗi tin nhắn!
     - **Ra lệnh độc lập từng con:** Khi ra lệnh cho con nào, **chỉ duy nhất con đó** phản hồi và thực thi hành động, các robot khác xung quanh vẫn tiếp tục công việc bình thường. Muốn chuyển sang con khác, chỉ cần gọi tên con đó (ví dụ: `Thợ Mỏ đi theo`).
     - **Kết thúc phiên:** Gõ `tạm biệt` hoặc `bye` để robot chào và đóng phiên trò chuyện.
     - **Hiển thị trực quan:** Lời đáp của robot hiển thị dạng `[Tên Robot] <Nội dung>` màu xanh ngọc nổi bật, robot tự quay đầu nhìn bạn, đổi biểu cảm khuôn mặt LED và phát âm thanh phản hồi. Nếu bạn chat thông thường với người chơi khác, robot sẽ tự động giữ im lặng để không làm phiền.
6. **Tính cách & Kỷ niệm (`Personality & Memories`):**
   - Xem chỉ số **Độ gắn bó (`Bond 0–100`)**, thống kê tổng số **Quặng, Gỗ, Cây trồng, Nông sản, Vật phẩm đã cất** và **Nhật ký kỷ niệm gần đây**.
   - Chuyển đổi giữa **4 Tính cách**: **Tò mò (`Curious`)**, **Cẩn thận (`Careful`)**, **Vui vẻ (`Cheerful`)**, **Điềm tĩnh (`Calm`)**.
   - Bật/tắt tùy chọn **Tự đổi đồ (`Auto Tools`)**.

---

<a id="11-bảng-tóm-tắt-thao-tác-nhanh-quick-reference"></a>
## 11. Bảng Tóm tắt Thao tác Nhanh (Quick Reference)

| Thao tác của Người chơi | Vật phẩm trên tay | Kết quả |
| :--- | :--- | :--- |
| **Nhấp chuột phải** vào NOVA chưa thuần hóa | Bột Đá Đỏ (`Redstone`) | Thuần hóa NOVA (tỷ lệ 33%/lần). |
| **Nhấp chuột phải** vào NOVA đã thuần hóa | Bột Đá Đỏ (`Redstone`) | Sạc `+200` Năng lượng (`+20%` Pin). |
| **Shift + Nhấp chuột phải** vào NOVA | Tay không / Bất kỳ (trừ Gậy) | Mở **Ba lô 27 ô & Đai công cụ** (hoặc đặt rương xuống nếu đang bê rương). |
| **Nhấp chuột trái** vào NOVA | Gậy điều khiển NOVA | Mở **Bảng Điều Khiển NOVA** (Ra lệnh, Giao việc, Biểu cảm, Trò chuyện...). |
| **Shift + Nhấp chuột trái** vào NOVA | Gậy điều khiển NOVA | **Thu hồi nhanh** toàn bộ công cụ của NOVA về túi đồ người chơi. |
| **Nhấp chuột phải** vào NOVA → **Nhấp chuột phải** vào Rương NOVA | Gậy điều khiển NOVA | **Liên kết 1-1** NOVA với Rương NOVA chính (dành cho các nghề lao động). |
| **Nhấp chuột phải** vào NOVA → **Shift + Nhấp phải** vào Rương thường / Nút | Gậy điều khiển NOVA | Cấp hoặc hủy quyền sử dụng **Rương phụ / Nút bấm** (tối đa 9 mỗi loại). |
| **Shift + Nhấp chuột phải** vào Rương NOVA đã liên kết | Tay không / Gậy điều khiển | Ra lệnh cho NOVA **nhấc bổng Rương NOVA** đi theo bạn. |
| **Đặt Gậy xuống đất** / **Nhấp phải** vào Gậy đã đặt | Gậy điều khiển / Tay không | **Tập hợp đội hình (`Rally`)** hoặc **Giải tán** toàn bộ NOVA trong bán kính 64 khối. |
| **Gõ Chat chính (Phím `T`)** | Gọi tên (vd: `Chiến Binh đi theo`, `Nova 1 đứng chờ`) | **Bật trò chuyện & ra lệnh độc lập** cho riêng Robot NOVA đó trực tiếp trên khung chat! |

---

## 🛠️ Hướng dẫn Build từ Mã nguồn (Dành cho Nhà phát triển)

```powershell
# Biên dịch kiểm tra mã nguồn (Server & Client)
.\gradlew.bat compileJava compileClientJava

# Đóng gói file .jar của mod (kết quả nằm tại build/libs/)
.\gradlew.bat build

# Chạy trực tiếp Client Minecraft để test
.\gradlew.bat runClient
```
