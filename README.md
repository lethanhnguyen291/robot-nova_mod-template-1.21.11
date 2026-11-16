# 🤖 Robot NOVA Mod — Minecraft 1.21.11 (Fabric)

**Robot NOVA Mod** (`robot-nova_mod`) là bản mod thêm người bạn đồng hành robot thông minh **NOVA** vào thế giới Minecraft **1.21.11** (sử dụng **Fabric API** & **GeckoLib**). NOVA không chỉ là một trợ lý tự động hóa công việc (Đào mỏ, Đốn củi & Trồng rừng, Làm nông, San lấp đất & Xây nhà theo bản vẽ, Chiến đấu Song Kiếm) mà còn sở hữu hệ thống cảm xúc sống động (40 biểu cảm khuôn mặt), tính cách riêng, sổ tay kỷ niệm, tự sửa chữa bằng năng lượng, dọn đường thông minh, tượng ngủ đông như tượng khối sắt, và giao tiếp/nhận lệnh trực tiếp từ người chơi bằng tiếng Việt.

---

## 📑 Mục lục

1. [💻 Yêu cầu hệ thống & Cài đặt](#1-yêu-cầu-hệ-thống--cài-đặt)
2. [⚡ Cách triệu hồi & Thuần hóa Robot NOVA](#2-cách-triệu-hồi--thuần-hóa-robot-nova)
3. [🗿 NOVA Chưa Kích Hoạt (Tượng Kim Loại Ngủ Đông)](#3-nova-chưa-kích-hoạt-tượng-kim-loại-ngủ-đông)
4. [🧰 Danh sách Vật phẩm, Khối & Công cụ trong Mod](#4-danh-sách-vật-phẩm-khối--công-cụ-trong-mod)
5. [🔨 Công thức chế tạo (Crafting Recipes)](#5-công-thức-chế-tạo-crafting-recipes)
6. [🔋 Hệ thống Chỉ số, Năng lượng, Cấp độ & Tự sửa chữa](#6-hệ-thống-chỉ-số-năng-lượng-cấp-độ--tự-sửa-chữa)
7. [🎒 Giao diện Ba lô & Đai Công cụ của NOVA](#7-giao-diện-ba-lô--đai-công-cụ-của-nova)
8. [🔧 Chi tiết 6 Nghề nghiệp và Cơ chế Công cụ](#8-chi-tiết-6-nghề-nghiệp-và-cơ-chế-công-cụ)
   - [⚔️ Chiến binh NOVA (Cầm Kiếm / Song Kiếm)](#81-chiến-binh-nova-cầm-kiếm--song-kiếm)
   - [⛏️ Thợ mỏ NOVA (Cầm Cúp / Cuốc chim)](#82-thợ-mỏ-nova-cầm-cúp--cuốc-chim)
   - [🪓 Tiều phu NOVA (Cầm Rìu)](#83-tiều-phu-nova-cầm-rìu)
   - [🌾 Nông dân NOVA (Cầm Cuốc)](#84-nông-dân-nova-cầm-cuốc)
   - [🪏 Công nhân Đào đất NOVA (Cầm Xẻng thường)](#85-công-nhân-đào-đất-nova-cầm-xẻng-thường)
   - [🏗️ Kỹ sư Xây dựng NOVA (Cầm Xẻng kết hợp Bản vẽ AI)](#86-kỹ-sư-xây-dựng-nova-cầm-xẻng-kết-hợp-bản-vẽ-ai)
9. [📡 Hướng dẫn sử dụng Gậy Điều Khiển NOVA](#9-hướng-dẫn-sử-dụng-gậy-điều-khiển-nova)
10. [📦 Hệ thống Rương NOVA, Rương phụ & Vận chuyển Rương](#10-hệ-thống-rương-nova-rương-phụ--vận-chuyển-rương)
11. [🎮 Bảng Điều Khiển, Biểu cảm, Tính cách & Trò chuyện](#11-bảng-điều-khiển-biểu-cảm-tính-cách--trò-chuyện)
12. [📢 Lệnh Đồng Loạt Toàn Bộ NOVA (`/NOVA:[CÂU RA LỆNH]`)](#12-lệnh-đồng-loạt-toàn-bộ-nova-novacâu-ra-lệnh)
13. [⚡ Bảng Tóm tắt Thao tác Nhanh (Quick Reference)](#13-bảng-tóm-tắt-thao-tác-nhanh-quick-reference)

---

## 1. Yêu cầu hệ thống & Cài đặt

- **Phiên bản Minecraft:** `1.21.11`
- **Mod Loader:** Fabric Loader (`>= 0.18.4`)
- **Thư viện bắt buộc:**
  - **Fabric API** (`0.141.3+1.21.11`)
  - **GeckoLib 5** (`5.4.5` cho Minecraft 1.21.11)
- **Ngôn ngữ hỗ trợ:** Tiếng Việt (`vi_vn`) và Tiếng Anh (`en_us`). Mọi thông báo trạng thái của NOVA đều hiển thị gọn gàng trên **Thanh hành động (Action Bar)** phía trên 9 ô công cụ để không làm trôi khung chat của người chơi.

---

## 2. Cách triệu hồi, Lắp ráp & Kích hoạt Robot NOVA

### 2.1. Lắp ghép cấu trúc 5 khối trong thế giới

Bạn có thể triệu hồi NOVA bằng **Trứng triệu hồi NOVA** (`nova_spawn_egg`), đặt trực tiếp **NOVA Chưa Kích Hoạt** (`nova_dormant`), hoặc **lắp ráp 5 khối linh kiện** trong thế giới (tốn **1 Đầu NOVA**, **1 Thân NOVA** và **3 Vỏ khung NOVA**, đặt **Đầu NOVA cuối cùng**):

- **Kiểu A (Có đế dưới chân):** Đặt 1 **Vỏ khung NOVA** (`nova_frame_casing`) làm đế, đặt **Thân NOVA** (`nova_body_unit`) lên trên, gắn 2 **Vỏ khung NOVA** vào hai bên trái & phải của Thân, cuối cùng đặt **Đầu NOVA** (`nova_head_unit`) lên đỉnh Thân.
- **Kiểu B (Có vỏ sau lưng):** Đặt **Thân NOVA** ở giữa, gắn 2 **Vỏ khung NOVA** vào hai bên trái & phải và 1 **Vỏ khung NOVA** vào mặt sau lưng của Thân, cuối cùng đặt **Đầu NOVA** lên đỉnh Thân.

```text
      [ Đầu NOVA ]            <-- Đặt cuối cùng ở trên đỉnh Thân NOVA
[Vỏ khung] [Thân NOVA] [Vỏ khung]  <-- Hai tay Vỏ khung hai bên (thêm 1 Vỏ khung ở dưới chân hoặc sau lưng)
```

> **Lưu ý:** Ngay khi đặt Đầu NOVA hoàn tất cấu trúc (nếu có đủ khoảng trống hợp lệ), 5 khối sẽ hợp nhất cùng hiệu ứng tia lửa điện tạo thành **NOVA Chưa Kích Hoạt**!

### 2.2. Kích hoạt & Thuần hóa (Activation & Taming)

- **Thức tỉnh từ trạng thái Chưa Kích Hoạt:** Cầm **1 Khối Đá Đỏ (`Redstone Block`)** và **Nhấp chuột phải** vào NOVA Chưa Kích Hoạt. NOVA sẽ thức tỉnh ngay lập tức với **100% Pin**, đầy máu và tự động nhận người đánh thức làm chủ nhân! (Cầm Bột Đá Đỏ nhấp vào tượng chưa kích hoạt sẽ hiện hướng dẫn cần dùng Khối Đá Đỏ).
- **Thuần hóa NOVA triệu hồi từ Trứng:** Với NOVA đang hoạt động nhưng chưa có chủ (sinh ra từ `nova_spawn_egg`), bạn có thể dùng **Bột Đá Đỏ (`Redstone Dust`)** hoặc **Khối Đá Đỏ (`Redstone Block`)** nhấp chuột phải để kết nối làm chủ nhân.

---

## 3. 🗿 NOVA Chưa Kích Hoạt (Tượng Kim Loại Ngủ Đông)

Vật phẩm **NOVA Chưa Kích Hoạt (`nova_dormant`)** hoạt động theo cơ chế **Tượng khối kim loại vô tri** (tương tự như Tượng Copper Golem trong Minecraft):

- **Vị trí Tab Sáng Tạo:** Nằm trong tab riêng **NOVA** (`itemGroup.robot-nova_mod.nova`) và tab **Khối chức năng (`Functional Blocks`)**.
- **Đặt tượng xuống đất:**
  - Phát âm thanh đặt tượng và tiếng **khối sắt (`SoundEvents.IRON_PLACE`)**.
  - Tự động thay thế các khối có thể đè lên (như cỏ, dương xỉ) tại vị trí nhấp chuột và kiểm tra đủ không gian trống.
  - Tự động xoay mặt đối diện người chơi theo 4 hướng chuẩn (Bắc, Nam, Đông, Tây).
  - Giữ nguyên toàn bộ dữ liệu của robot (chủ nhân, ba lô, trang bị, cấp độ kinh nghiệm, trí nhớ và tư thế tượng) khi thu hồi rồi đặt lại.
- **Đổi 5 tư thế tượng:**
  - **Nhấp chuột phải** (tay không hoặc vật phẩm thường) vào tượng để xoay vòng qua 5 tư thế đặc trưng:
    1. *Tắt nguồn ngủ đông (1/5)*
    2. *Đứng nghiêm canh gác (2/5)*
    3. *Ngồi gập gối chờ lệnh (3/5)*
    4. *Giơ tay chào (4/5)*
    5. *Chiến binh thủ thế (5/5)*
- **Đặc tính Tượng vô tri:**
  - Đứng yên cố định góc xoay, miễn nhiễm lực đẩy lùi (`knockback`), không bị quái vật tấn công, không rên đau hay nảy hạt tim máu.
  - Nhấp chuột giữa (`Pick Block`) lấy ra đúng vật phẩm `nova_dormant`.
- **Thu hồi tượng & Bảo vệ quyền sở hữu:**
  - Chủ sở hữu (hoặc người chơi ở chế độ Sáng tạo / tượng chưa có chủ) có thể **đập vào tượng** hoặc **Shift + Chuột phải tay không** để thu hồi ngay vào túi đồ.
  - NOVA đã có chủ nhân sẽ được bảo vệ: người chơi khác không thể thu hồi cũng không thể dùng Khối Đá Đỏ để chiếm quyền sở hữu.

---

## 4. Danh sách Quặng, Linh kiện, Khối & Công cụ trong Mod

Toàn bộ vật phẩm, quặng, linh kiện và công cụ của mod đều được tập hợp đầy đủ trong tab Sáng tạo riêng mang tên **NOVA** (biểu tượng Bộ xử lý AI NOVA):

### 4.1. Khoáng sản & Quặng Thế giới (`Worldgen Ores`)

Chỉ sinh ra tại **Overworld** (trong các chunk mới tạo), khai thác bằng **Cúp Đá (`Stone Pickaxe`) trở lên**, hỗ trợ phù phép **Gia Tài (`Fortune`)** và **Chạm Mềm Mại (`Silk Touch`)**. Thợ mỏ NOVA tự động nhận diện cả 4 khối quặng này và nhận **`+2 EXP`** mỗi khối đào được:

| Tên tiếng Việt | ID khối / vật phẩm | Độ cao phân bố (Y) | Đặc điểm & Vật phẩm rơi ra |
| :--- | :--- | :--- | :--- |
| **Quặng silic** | `robot-nova_mod:silicon_ore` | `Y -16` đến `112` (dày nhất quanh `Y 48`) | Rơi `1–2` **Silic thô** (`raw_silicon`). Có thêm cụm lộ vách hang và mạch nối dài (`Y 0..50`). |
| **Quặng silic đá sâu** | `robot-nova_mod:deepslate_silicon_ore` | Tầng đá sâu Deepslate (`Y -16..0`) | Biến thể đá sâu của Quặng silic, rơi `1–2` **Silic thô**. |
| **Quặng tinh thạch năng lượng** | `robot-nova_mod:energy_crystal_ore` | `Y -32` đến `64` (dày nhất quanh `Y 16`) | Phát sáng mức `5`. Rơi `1–2` **Tinh thạch năng lượng** (`energy_crystal`) và `1–3` EXP. |
| **Quặng tinh thạch đá sâu** | `robot-nova_mod:deepslate_energy_crystal_ore` | Tầng sâu (`Y -48..0`) | Phát sáng mức `5`, tăng mật độ ở tầng sâu và có mạch lớn xuống tới `Y -48`. |

### 4.2. Linh kiện Điện tử & Khối Lắp ráp NOVA

| Tên tiếng Việt | ID vật phẩm / khối | Loại | Mô tả & Công dụng |
| :--- | :--- | :--- | :--- |
| **Silic thô** | `robot-nova_mod:raw_silicon` | Nguyên liệu | Khai thác từ Quặng silic; đem nung chảy để tạo thành Phiến silic. |
| **Phiến silic** | `robot-nova_mod:silicon_wafer` | Linh kiện | Nung từ Silic thô trong Lò nung hoặc Lò luyện kim; dùng chế tạo Chip và Bộ xử lý AI. |
| **Tinh thạch năng lượng** | `robot-nova_mod:energy_crystal` | Nguyên liệu | Tinh thể phát năng lượng khai thác từ Quặng tinh thạch; dùng làm Lõi năng lượng & Bộ xử lý AI. |
| **Mạch điện NOVA** | `robot-nova_mod:nova_circuit` | Linh kiện | Bảng mạch cơ bản để chế tạo Chip điều khiển và Thân NOVA. |
| **Chip điều khiển** | `robot-nova_mod:control_chip` | Linh kiện | Vi mạch điều khiển trung gian cho Bộ xử lý AI và Đầu NOVA. |
| **Bộ xử lý AI NOVA** | `robot-nova_mod:nova_ai_processor` | Linh kiện cao cấp | Trái tim trí tuệ nhân tạo của Đầu NOVA. |
| **Màn hình NOVA** | `robot-nova_mod:nova_screen` | Linh kiện | Màn hình hiển thị 40 biểu cảm LED cho Đầu NOVA. |
| **Lõi năng lượng NOVA** | `robot-nova_mod:nova_energy_core` | Linh kiện | Lõi tích trữ năng lượng dùng để chế tạo Pin NOVA. |
| **Pin NOVA** | `robot-nova_mod:nova_battery` | Linh kiện | Nguồn cấp điện chính đặt bên trong Thân NOVA. |
| **Động cơ nhỏ** | `robot-nova_mod:small_motor` | Linh kiện | Động cơ truyền động khớp máy cho Thân NOVA. |
| **Vỏ khung NOVA** | `robot-nova_mod:nova_frame_casing` | Khối lắp ráp | Khối vỏ hợp kim sắt - đồng dùng để ghép tay, chân/lưng khi lắp ráp NOVA. |
| **Đầu NOVA** | `robot-nova_mod:nova_head_unit` | Khối lắp ráp | Khối đầu robot có hướng mặt; đặt lên trên Thân NOVA để kích hoạt lắp ráp. |
| **Thân NOVA** | `robot-nova_mod:nova_body_unit` | Khối lắp ráp | Khối thân trung tâm chứa pin, động cơ và khoang chứa đồ của NOVA. |

### 4.3. Thiết bị Chỉ huy & Bộ Công cụ NOVA

| Tên tiếng Việt | ID vật phẩm / khối | Loại | Mô tả & Công dụng chính |
| :--- | :--- | :--- | :--- |
| **Gậy điều khiển NOVA** | `robot-nova_mod:nova_control_rod` | Vật phẩm & Khối | Trung tâm chỉ huy NOVA: Mở bảng điều khiển, chọn robot, liên kết rương, thu hồi công cụ, cấp quyền rương phụ/nút bấm, làm cột mốc cho NOVA Chiến binh và triệu tập đội hình (Rally). |
| **Rương NOVA** | `robot-nova_mod:nova_chest` | Khối chứa đồ | Rương trung tâm (27 ô) dùng để liên kết 1-1 với một Robot NOVA. Là tâm điểm bán kính làm việc (16 khối) và nơi NOVA tự cất đồ, lấy công cụ mới hoặc được bê đi theo chủ. |
| **NOVA Chưa Kích Hoạt** | `robot-nova_mod:nova_dormant` | Khối / Tượng | Bức tượng robot NOVA ở trạng thái ngủ đông (5 tư thế đổi bằng chuột phải, thức tỉnh bằng Khối Đá Đỏ). |
| **Kiếm NOVA** | `robot-nova_mod:nova_sword` | Vũ khí | Kiếm chuyên dụng của NOVA (chất liệu Đồng, sát thương +3, tốc độ đánh -2.4). Có thể cầm **Song Kiếm 2 tay**. |
| **Cuốc chim NOVA** | `robot-nova_mod:nova_pickaxe` | Công cụ | Cúp chuyên dụng cho nghề Thợ mỏ NOVA (sát thương +1, tốc độ -2.8). |
| **Rìu NOVA** | `robot-nova_mod:nova_axe` | Công cụ | Rìu chuyên dụng cho nghề Tiều phu NOVA (sát thương +7, tốc độ -3.2). |
| **Cuốc NOVA** | `robot-nova_mod:nova_hoe` | Công cụ | Cuốc chuyên dụng cho nghề Nông dân NOVA (tốc độ -2.0). |
| **Xẻng NOVA** | `robot-nova_mod:nova_shovel` | Công cụ | Xẻng chuyên dụng cho nghề Đào đất và Xây dựng bản vẽ AI (sát thương +1.5, tốc độ -3.0). |
| **Trứng triệu hồi NOVA** | `robot-nova_mod:nova_spawn_egg` | Vật phẩm | Sinh ra ngay một Robot NOVA đang hoạt động tại vị trí chỉ định. |

> **💡 Mẹo:** Ngoài bộ công cụ NOVA chuyên dụng, Robot NOVA **tương thích hoàn toàn với mọi công cụ và vũ khí chuẩn của Minecraft** (Gỗ, Đá, Đồng, Sắt, Vàng, Kim cương, Netherite) thuộc nhóm Kiếm, Cúp, Rìu, Cuốc, Xẻng!

---

## 5. Công thức chế tạo (Crafting Recipes)

### 5.1. Luyện kim & Chế tạo Linh kiện (Ghép tự do 2×2)

- **Nung Phiến silic (`silicon_wafer`):** Nung **Silic thô** (`raw_silicon`) trong Lò nung thường (`10 giây`) hoặc Lò luyện kim (`5 giây`, nhận `0.3 EXP`).
- **8 Công thức Linh kiện & Vỏ khung (Không cần đúng vị trí, chế tạo được ngay trong ô 2×2):**

| Thành phẩm | Nguyên liệu yêu cầu | Số lượng nhận |
| :--- | :--- | :---: |
| **Mạch điện NOVA** (`nova_circuit`) | `1 Phôi đồng` + `1 Bột đá đỏ` + `1 Giấy` | **2** |
| **Chip điều khiển** (`control_chip`) | `1 Mạch điện NOVA` + `1 Phiến silic` + `1 Bột đá đỏ` | **1** |
| **Bộ xử lý AI NOVA** (`nova_ai_processor`) | `1 Chip điều khiển` + `1 Phiến silic` + `1 Phôi đồng` + `1 Tinh thạch năng lượng` | **1** |
| **Màn hình NOVA** (`nova_screen`) | `1 Kính (hoặc Kính tấm)` + `1 Phôi sắt` + `1 Ngọc lưu ly` + `1 Bột đá đỏ` | **1** |
| **Lõi năng lượng NOVA** (`nova_energy_core`) | `1 Tinh thạch năng lượng` + `1 Phôi đồng` + `1 Bột đá đỏ` | **1** |
| **Pin NOVA** (`nova_battery`) | `1 Lõi năng lượng NOVA` + `1 Phôi sắt` + `1 Phôi đồng` + `1 Bột đá đỏ` | **1** |
| **Động cơ nhỏ** (`small_motor`) | `1 Phôi sắt` + `1 Phôi đồng` + `1 Bột đá đỏ` | **2** |
| **Vỏ khung NOVA** (`nova_frame_casing`) | `3 Phôi sắt` (đặt ở 3 ô riêng) + `1 Phôi đồng` | **3** |

### 5.2. Chế tạo Đầu NOVA, Thân NOVA & NOVA Chưa Kích Hoạt (Bàn chế tạo 3×3)

- **Đầu NOVA (`nova_head_unit`):**
  - Hàng trên: `Phôi Sắt` — `Đèn Lồng (Lantern)` — `Phôi Sắt`
  - Hàng giữa: `Chip điều khiển` — `Màn hình NOVA` — `Bộ xử lý AI NOVA`
  - Hàng dưới: *(Trống)* — `Phôi Sắt` — *(Trống)*
- **Thân NOVA (`nova_body_unit`):**
  - Hàng trên: `Động cơ nhỏ` — `Phôi Sắt` — `Động cơ nhỏ`
  - Hàng giữa: `Mạch điện NOVA` — `Pin NOVA` — `Rương gỗ thường (Chest)`
  - Hàng dưới: `Động cơ nhỏ` — `Phôi Sắt` — `Động cơ nhỏ`
- **NOVA Chưa Kích Hoạt (`nova_dormant`):**
  - Hàng trên: *(Trống)* — `Đầu NOVA` — *(Trống)*
  - Hàng giữa: `Vỏ khung NOVA` — `Thân NOVA` — `Vỏ khung NOVA`
  - Hàng dưới: *(Trống)* — `Vỏ khung NOVA` — *(Trống)*

### 5.3. Gậy điều khiển NOVA (`nova_control_rod`) & Rương NOVA (`nova_chest`)

- **Gậy điều khiển NOVA (3 hàng dọc):** `1 Ngọc Lưu Ly` (trên) + `1 Bột Đá Đỏ` (giữa) + `1 Cột Thu Lôi` (dưới).
- **Rương NOVA (3×3):** `4 Phôi Sắt` (4 góc) + `2 Phôi Đồng` (giữa trên & giữa dưới) + `2 Bột Đá Đỏ` (trái & phải giữa) + `1 Rương gỗ thường` (chính giữa).

### 5.4. Bộ 5 Công cụ Chuyên dụng NOVA (Đồng - Copper Tier)

Chế tạo trên Bàn chế tạo 3×3 theo khuôn công cụ tiêu chuẩn, sử dụng **Phôi Đồng (`Copper Ingot`)** và **Que Gỗ (`Stick`)**:
- **Kiếm NOVA (`nova_sword`):** 2 Phôi Đồng + 1 Que Gỗ.
- **Cuốc chim NOVA (`nova_pickaxe`):** 3 Phôi Đồng + 2 Que Gỗ.
- **Rìu NOVA (`nova_axe`):** 3 Phôi Đồng + 2 Que Gỗ.
- **Xẻng NOVA (`nova_shovel`):** 1 Phôi Đồng + 2 Que Gỗ.
- **Cuốc NOVA (`nova_hoe`):** 2 Phôi Đồng + 2 Que Gỗ.

---

## 6. Hệ thống Chỉ số, Năng lượng, Cấp độ & Tự sửa chữa

- **Máu tối đa:** `20.0 HP` (10 tim). Giáp cơ bản: `4.0`.
- **Dung lượng Pin:** `1000` đơn vị (`100%`).
  - Tiêu hao `1 năng lượng/giây` khi thao tác công việc (không tốn khi đi theo hoặc nghỉ).
  - Khi pin cạn sạch (`0%`), NOVA tự động chuyển sang trạng thái **Tượng Chưa Kích Hoạt (Ngủ đông)** để bảo vệ vi mạch. Dùng Khối Đá Đỏ để nạp đầy và kích hoạt lại!
  - Cầm Bột Đá Đỏ nhấp chuột phải sạc `+20% Pin` (`+200 năng lượng`).
- **Tự sửa chữa (Self-Repair):** Sau 10 giây không bị đánh, nếu máu chưa đầy và pin `>= 11%`, cứ 5 giây NOVA tự tiêu hao 1% pin để hồi phục `+1.0 HP`.
- **Cấp độ Kỹ năng (Lv. 1 → 5):**
  - **Lv. 1:** Tốc độ chuẩn (`1.0x`).
  - **Lv. 2:** Tốc độ làm việc `+15%`, sát thương `+0.75`.
  - **Lv. 3:** Tốc độ `+30%`, sát thương `+1.5`, **Tự cắm đuốc khi đào mỏ**.
  - **Lv. 4:** Tốc độ `+45%`, sát thương `+2.25`.
  - **Lv. 5:** Tốc độ `+60%`, sát thương `+3.0`, **Đốn hạ toàn bộ cây cùng lúc (Tree Feller)**!

---

## 7. Giao diện Ba lô & Đai Công cụ của NOVA

- **Cách mở:** Nhấp chuột phải tay không hoặc giữ `Shift + Chuột phải` vào NOVA.
- **Khu vực hiển thị:**
  1. Mô hình 3D trực tiếp, thanh Máu, thanh Pin, thanh Cấp độ & tiến độ EXP.
  2. **Kho chứa đồ 27 ô (Ba lô chính ở giữa):** Đựng tài nguyên thu hoạch và vật tư dự phòng.
  3. **Cột 6 ô Công cụ & Trang bị (Bên phải):**
     - **Ô số 1 (Tay chính):** Quyết định nghề nghiệp của NOVA.
     - **Ô số 2, 3, 4, 5 (Đai công cụ dự phòng):** Chứa công cụ dự phòng cùng loại hoặc khác loại để NOVA tự mượn dọn đường.
     - **Ô số 6 (Tay phụ - `OFFHAND`):** Đặt thanh kiếm thứ 2 (chế độ Song Kiếm) hoặc chứa Đuốc, Cây non, Hạt giống, Khối xây dựng.

---

## 8. Chi tiết 6 Nghề nghiệp và Cơ chế Công cụ

### 8.1. Chiến binh NOVA (Cầm Kiếm / Song Kiếm)

- **Kích hoạt:** Cầm **Kiếm** ở Ô số 1.
- **Đặc điểm:** Tự động hủy liên kết rương để hoạt động cơ động quanh chủ nhân hoặc cột Gậy điều khiển.
- **Song Kiếm (Dual-Wielding):** Đặt thêm kiếm vào ô Tay phụ hoặc để sẵn trong ba lô, NOVA tự rút sang tay trái: tăng thêm `+50%` sát thương, giảm hồi chiêu từ 16 ticks xuống 10 ticks (chém cực nhanh) và vung luân phiên cả 2 tay.
- **3 Chế độ kết hợp:** Bảo vệ chủ nhân (ưu tiên 1) > Tuần tra tự vệ diệt quái (ưu tiên 2) > Đi săn tài nguyên (ưu tiên 3).
- **Báo hỏng:** Khi toàn bộ kiếm hỏng hết, NOVA chạy về chủ nhân thông báo: *"Mình bị hư hết vũ khí rồi, cấp vũ khí cho mình đi!"*.

### 8.2. Thợ mỏ NOVA (Cầm Cúp / Cuốc chim)

- **Kích hoạt:** Cầm **Cúp** ở Ô 1 + Liên kết Rương NOVA + Bật Giao việc.
- Dò tìm hang động/hầm mỏ tự nhiên trong bán kính 16 khối. Đào sạch từng mạch quặng (Vein Mining) trước khi đào đá thường.
- Từ Cấp 3 trở lên: Tự động rút Đuốc sang tay phụ và cắm đuốc khi độ sáng `< 5`.

### 8.3. Tiều phu NOVA (Cầm Rìu)

- **Kích hoạt:** Cầm **Rìu** ở Ô 1 + Liên kết Rương NOVA + Bật Giao việc.
- Nhận diện cây tự nhiên, đốn gỗ từ gốc lên ngọn (Cấp 5 đốn hạ cả cây cùng lúc).
- Tự động nhặt gỗ, táo, mầm cây và **rút mầm cây sang tay phụ để trồng lại rừng ngay tại gốc cũ**.

### 8.4. Nông dân NOVA (Cầm Cuốc)

- **Kích hoạt:** Cầm **Cuốc** ở Ô 1 + Liên kết Rương NOVA + Bật Giao việc.
- Tự xới đất ruộng quanh nguồn nước, rút hạt giống sang tay phụ gieo hạt, thu hoạch cây chín và trồng lại ngay lập tức.

### 8.5. Công nhân Đào đất NOVA (Cầm Xẻng thường)

- **Kích hoạt:** Cầm **Xẻng** ở Ô 1 + Liên kết Rương NOVA + Bật Giao việc (khi chưa giao bản vẽ xây dựng).
- Đào san lấp đất, cát, sỏi theo khuôn 5x5 khối từ trên xuống dưới một cách gọn gàng.

### 8.6. 🏗️ Kỹ sư Xây dựng NOVA (Cầm Xẻng kết hợp Bản vẽ AI)

- **Kích hoạt:** Cầm **Xẻng** ở Ô 1 + Giao công trình xây dựng qua Bảng Điều Khiển Xây Dựng.
- **Giao diện Bản vẽ 3D (`NovaStructureScreen`):**
  - Xem trước mô hình 3D tương tác: kéo chuột để xoay, lăn chuột để phóng to/thu nhỏ, xem mặt cắt theo từng lớp chiều cao Y.
  - Nền xem trước màu xanh phong cách NoVa sang trọng.
  - Bộ nút điều khiển trạng thái công trình: **Giao việc / Tạm dừng / Tiếp tục / Hủy** và nút **Đóng** nằm cân đối, rõ ràng.
- **Sử dụng Giàn giáo (`Scaffolding`) thông minh:**
  - Khi cần xây dựng ở độ cao vượt tầm với, NOVA tự động dựng cột giàn giáo ở **vòng ngoài công trình** để leo trèo làm việc (không đặt bên trong làm kẹt nóc nhà).
  - Tự động trèo lên, trèo xuống giàn giáo linh hoạt.
  - **Thu hồi toàn bộ giàn giáo:** Ngay khi công trình hoàn thành (hoặc khi bị hủy), NOVA tự động dùng công cụ phá dỡ và thu hồi sạch sẽ toàn bộ các khối giàn giáo đã dựng!
- **Thao tác xây dựng chân thực:**
  - Mỗi khi đặt một khối, NOVA tự động rút khối tương ứng sang **tay trái (`OFFHAND`)** trước rồi mới thực hiện động tác đặt khối vào vị trí.
  - Tự động dùng Rìu/Cúp/Xẻng dọn dẹp các khối đặt lỗi do địa hình hoặc nhầm vị trí.

---

## 9. Hướng dẫn sử dụng Gậy Điều Khiển NOVA

1. **Nhấp chuột trái vào NOVA:** Mở Bảng Điều Khiển NOVA.
2. **Shift + Nhấp chuột trái vào NOVA:** Thu hồi nhanh toàn bộ công cụ của robot về túi đồ.
3. **Nhấp chuột phải vào NOVA:** Chọn robot để chuẩn bị liên kết rương.
4. **Nhấp chuột phải vào Rương NOVA:** Hoàn tất liên kết 1-1 với rương (chống liên kết trùng).
5. **Shift + Nhấp chuột phải vào Rương thường / Nút bấm:** Cấp quyền sử dụng Rương phụ (tối đa 9) hoặc Nút bấm (tối đa 9).
6. **Cắm Gậy xuống đất:** Tạo cột mốc Tập Hợp Đội Hình (`Rally Point`). Toàn bộ NOVA trong bán kính 64 khối sẽ xếp hàng ngay ngắn trước gậy! Nhấp chuột phải vào gậy lần nữa để giải tán.

---

## 10. Hệ thống Rương NOVA, Rương phụ & Vận chuyển Rương

- **Tự động cất đồ:** Khi đầy ba lô, NOVA chạy về Rương NOVA (hoặc Rương phụ) cất đồ và chỉ giữ lại công cụ cùng vật tư cần thiết (đuốc, mầm cây).
- **Bê rương đi theo bạn:** Giữ `Shift + Chuột phải` vào Rương NOVA đã liên kết, NOVA sẽ nhấc bổng rương lên 2 tay và đi theo bạn. Nhấn `Shift + Chuột phải` vào NOVA để đặt rương xuống đất và tự khôi phục liên kết.

---

## 11. Bảng Điều Khiển, Biểu cảm, Tính cách & Trò chuyện

- **4 Chế độ di chuyển:** Đi theo (`Follow`), Tự do (`Free`), Đứng chờ (`Wait`), Ngồi/Đứng dậy (`Sit/Stand`).
- **40 Biểu cảm khuôn mặt LED:** Biểu cảm thay đổi tự nhiên theo hoàn cảnh (bị thương, làm việc, ăn mừng, thiếu pin) hoặc tùy chỉnh thủ công.
- **Hành động cử chỉ:** Chào (`Wave`), Ăn mừng (`Celebrate`), Xin lỗi (`Apologize`), Gật đầu (`Nod`), Tự hào (`Proud`).
- **Trò chuyện trên khung chat (Phím `T`):**
  - Gọi tên riêng theo Thẻ tên (`Chiến Binh đi theo`, `Thợ Mỏ đứng chờ`).
  - Gọi theo số thứ tự (`Nova 1`, `Nova 2`) hoặc gọi chung (`Nova ơi`).
  - Phiên trò chuyện ghi nhớ trong 45 giây giúp nói chuyện tự nhiên mà không cần nhắc lại tên.

---

## 12. 📢 Lệnh Đồng Loạt Toàn Bộ NOVA (`/NOVA:[CÂU RA LỆNH]`)

Khi bạn sở hữu nhiều chú robot NOVA và muốn ra lệnh nhanh cho **toàn bộ đội quân NOVA cùng lúc**, hãy mở khung chat (phím `T`) và nhập theo cú pháp:

```text
/NOVA:[CÂU RA LỆNH]
```

*(Ví dụ: `/NOVA:ngồi xuống`, `/NOVA:đứng dậy`, `/NOVA:đi theo`, `/NOVA:tự do`, `/NOVA:đứng chờ`, `/NOVA:làm việc`, `/NOVA:nghỉ làm`, `/NOVA:tập hợp`, `/NOVA:chào`, `/NOVA:ăn mừng`, `/NOVA:tạm dừng xây`, `/NOVA:tiếp tục xây`, `/NOVA:hủy xây`...)*

### Các đặc điểm nổi bật của lệnh đồng loạt:

1. **Phạm vi tác động:** Toàn bộ NOVA đã thuần hóa của bạn đang hoạt động (không ở trạng thái ngủ đông) trong phạm vi xung quanh sẽ đồng loạt quay nhìn bạn và thi hành lệnh ngay lập tức!
2. **Không làm phiền khung chat:** NOVA **hoàn toàn không trả lời vào khung chat** để tránh làm trôi tin nhắn trò chuyện của người chơi.
3. **Hiển thị trực quan trên Action Bar:** Kết quả thực hiện lệnh được tổng hợp và hiển thị trực tiếp ở **thanh thông báo phía trên 9 ô công cụ ngang (Action Bar)** của bạn với màu sắc sinh động (ví dụ: `[NOVA ×5] Toàn bộ NOVA đã đồng loạt ngồi xuống!`).

---

## 13. ⚡ Bảng Tóm tắt Thao tác Nhanh (Quick Reference)

| Thao tác của Người chơi | Vật phẩm trên tay | Kết quả |
| :--- | :--- | :--- |
| **Nhấp chuột phải** vào NOVA chưa thuần hóa | Bột Đá Đỏ (`Redstone`) / Khối Đá Đỏ | Thuần hóa NOVA. |
| **Nhấp chuột phải** vào NOVA đã thuần hóa | Bột Đá Đỏ (`Redstone`) | Sạc `+200` Năng lượng (`+20%` Pin). |
| **Nhấp chuột phải** vào NOVA chưa kích hoạt | Khối Đá Đỏ (`Redstone Block`) | Nạp 100% pin, hồi đầy máu và khởi động NOVA sống dậy! |
| **Nhấp chuột phải** vào NOVA chưa kích hoạt | Tay không / Khối khác | Đổi vòng tròn qua **5 tư thế tượng kim loại**. |
| **Đập vào tượng** hoặc **Shift + Chuột phải** | Tay không / Công cụ | Thu hồi tượng NOVA vào túi đồ (phát tiếng đập khối sắt). |
| **Shift + Nhấp chuột phải** vào NOVA | Tay không / Bất kỳ (trừ Gậy) | Mở **Ba lô 27 ô & Đai công cụ** (hoặc đặt rương xuống nếu đang bê rương). |
| **Nhấp chuột trái** vào NOVA | Gậy điều khiển NOVA | Mở **Bảng Điều Khiển NOVA** (Ra lệnh, Giao việc, Biểu cảm, Trò chuyện...). |
| **Shift + Nhấp chuột trái** vào NOVA | Gậy điều khiển NOVA | **Thu hồi nhanh** toàn bộ công cụ của NOVA về túi đồ người chơi. |
| **Nhấp chuột phải** vào NOVA → **Nhấp chuột phải** vào Rương NOVA | Gậy điều khiển NOVA | **Liên kết 1-1** NOVA với Rương NOVA chính. |
| **Shift + Nhấp chuột phải** vào Rương NOVA đã liên kết | Tay không / Gậy điều khiển | Ra lệnh cho NOVA **nhấc bổng Rương NOVA** đi theo bạn. |
| **Đặt Gậy xuống đất** / **Nhấp phải** vào Gậy đã đặt | Gậy điều khiển / Tay không | **Tập hợp đội hình (`Rally`)** hoặc **Giải tán** toàn bộ NOVA trong bán kính 64 khối. |
| **Gõ Chat chính (Phím `T`)** | Gọi tên (vd: `Chiến Binh đi theo`, `Nova 1 đứng chờ`) | **Bật trò chuyện & ra lệnh độc lập** cho riêng Robot NOVA đó trên khung chat! |
| **Gõ Chat lệnh đồng loạt (Phím `T`)** | `/NOVA:[CÂU RA LỆNH]` (vd: `/NOVA:ngồi xuống`, `/NOVA:làm việc`) | **Toàn bộ NOVA đồng loạt thực hiện**, thông báo kết quả hiển thị trên **Action Bar** (không làm trôi chatbox)! |

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
