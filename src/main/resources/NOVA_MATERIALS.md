# Quặng, linh kiện và lắp ráp NOVA — Minecraft Java 1.21.11

## Chế tạo trong sinh tồn

Khai thác quặng bằng cuốc đá trở lên. Quặng silic rơi 1–2 silic thô, nung để lấy phiến silic. Quặng tinh thạch rơi 1–2 tinh thạch, phát sáng mức 5. Cả hai hỗ trợ Gia Tài và Chạm Mềm Mại.

Các công thức dưới đây không cần đúng vị trí, dùng được trong ô chế tạo 2×2. Mỗi thành phần liệt kê là một vật phẩm, trừ khi ghi số lượng khác.

| Thành phẩm | Nguyên liệu | Sản lượng |
|---|---|---|
| Mạch điện | Phôi đồng + bụi đá đỏ + giấy | 2 |
| Chip điều khiển | Mạch điện + phiến silic + bụi đá đỏ | 1 |
| Bộ xử lý AI | Chip điều khiển + phiến silic + phôi đồng + tinh thạch | 1 |
| Màn hình | Kính hoặc kính tấm + phôi sắt + lapis + bụi đá đỏ | 1 |
| Lõi năng lượng | Tinh thạch + phôi đồng + bụi đá đỏ | 1 |
| Pin NOVA | Lõi năng lượng + phôi sắt + phôi đồng + bụi đá đỏ | 1 |
| Động cơ nhỏ | Phôi sắt + phôi đồng + bụi đá đỏ | 2 |
| Vỏ khung | 3 phôi sắt (3 ô riêng) + phôi đồng | 3 |

Nung silic thô: lò thường 10 giây, lò luyện kim 5 giây.

Ba công thức sau dùng bàn chế tạo 3×3. Dấu chấm là ô trống.

**Đầu NOVA:** S = phôi sắt, Đ = đèn lồng, C = chip, M = màn hình, A = bộ xử lý AI.
```text
S Đ S
C M A
. S .
```

**Thân NOVA:** Đ = động cơ, S = phôi sắt, M = mạch điện, P = pin, R = rương thường.
```text
Đ S Đ
M P R
Đ S Đ
```

**NOVA chưa kích hoạt:** Đ = đầu, T = thân, V = vỏ khung.
```text
. Đ .
V T V
. V .
```

## Lắp ráp bằng khối trong thế giới

Xếp thẳng đứng **3 khối**, từ dưới lên trên. Đặt **đầu cuối cùng**:

```text
[ Đầu NOVA    ]  ← đặt cuối
[ Thân NOVA   ]
[ Vỏ khung    ]
```

Sau khi lắp ráp, vỏ khung dưới cùng trở thành **một rương NOVA rỗng**; thân và đầu biến mất. **NOVA chưa kích hoạt đứng ngay trên mặt rương**, cùng hướng với khối đầu. Robot được liên kết sẵn với rương này để làm điểm nhà và kho đồ ban đầu. Liên kết giữ lại khi lưu/tải thế giới hoặc thu hồi robot.

Không cần vỏ khung hai bên hoặc phía sau. Các khối đặt ngoài cột không bị tiêu thụ. Công thức ghép trực tiếp trên bàn chế tạo ở trên vẫn dùng 3 vỏ khung và chỉ cho ra vật phẩm NOVA chưa kích hoạt; cách tạo thêm rương này dành riêng cho việc xếp cột trong thế giới.

Dùng **một khối đá đỏ**, nhấp chuột phải vào robot để thức tỉnh, đầy pin và nhận người đánh thức làm chủ. Rương đã được liên kết, không cần dùng gậy chọn lại. Khi chuyển sang làm việc, robot dùng rương này như các rương được liên kết thủ công.

Thu hồi rồi đặt lại giữ chủ nhân, ba lô, trang bị, kinh nghiệm và trí nhớ. NOVA của người khác không thể bị chiếm bằng cách đánh thức lại.

## Tìm quặng và thử nhanh

- Silic: Y −16..112; dày nhất quanh Y 48, có thêm cụm lộ vách hang và mạch nối dài.
- Tinh thạch: Y −32..64; dày nhất quanh Y 16, tăng thêm ở tầng sâu; mạch lớn có thể xuống Y −48.
- Chỉ sinh ở Overworld, trong **chunk mới tạo**. Vùng đã khám phá sẽ không tự có quặng mới.
- Creative: mở nhóm **NOVA**, có đủ linh kiện, khối lắp ráp và trứng triệu hồi.
- Lệnh thử: `/give @s robot-nova_mod:nova_dormant` hoặc `/give @s robot-nova_mod:nova_head_unit`.

Build: mở Terminal tại thư mục dự án và chạy `./gradlew.bat build`. JAR dùng chơi game nằm trong `build/libs`; không lấy bản `sources`.
