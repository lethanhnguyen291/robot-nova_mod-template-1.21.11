# Rương thưởng khởi đầu NOVA

Mỗi thế giới mới có **một rương thưởng NOVA chung**, xuất hiện quanh điểm spawn của Overworld, ở cả **Sinh tồn và Sáng tạo**. Rương được tạo ngay trong bước khởi tạo điểm spawn của Minecraft, trước khi nhân vật vào; không còn chờ 5 giây hay chạy theo vị trí người chơi.

Cách tìm mặt đất dùng bản đồ độ cao `MOTION_BLOCKING_NO_LEAVES`, như rương thưởng gốc. Hệ thống tránh đặt lên lá cây/ngọn gỗ, tìm đất trống quanh đó và đặt tối đa bốn đuốc ở các phía có chỗ. Không cần bật tùy chọn Bonus Chest của Minecraft; tùy chọn rương vanilla vẫn giữ hành vi riêng của nó.

## Đồ trong rương

| Vật phẩm | Số lượng |
|---|---:|
| Bảng điều khiển NOVA Omni-Pad hiện có | 1 |
| Gậy điều khiển NOVA | 1 |
| NOVA chưa kích hoạt | 1 |
| Khối đá đỏ, dùng để thức tỉnh | 1 |
| Rìu NOVA | 1 |
| Cuốc chim NOVA | 1 |
| Xẻng NOVA | 1 |
| Cuốc làm ruộng NOVA | 1 |
| Kiếm NOVA | 1 |
| Bụi đá đỏ, dùng để sạc | 20 |

Rương chứa chính là khối rương NOVA đặt trong thế giới. Không có thêm một rương rỗng nằm bên trong. Rương mang tên “Rương thưởng khởi đầu NOVA”; người vào đầu tiên được thông báo tọa độ và cách đánh thức NOVA.

## Quy tắc xuất hiện

- Chỉ một rương NOVA cho cả thế giới, dùng chung cho mọi người chơi.
- Đổi chế độ, thoát/vào lại, chết/hồi sinh hoặc phá rương không tạo thêm rương.
- Nội dung thưởng giữ nguyên; chỉ cơ chế xuất hiện thay đổi.
- Tìm từ chunk chứa điểm spawn; chỉ thử thêm tám chunk liền kề khi cần. Không quét thế giới và không có vòng dò mỗi tick.
- Cây cao không làm rương bị giới hạn theo độ cao nhân vật. Rương được đặt trên mặt đất hợp lệ dưới tán lá hoặc ở chỗ trống cạnh cây.
- Không đặt xuống nước/lava, trên lá/ngọn gỗ, hoặc nhập vào rương có sẵn. Chỉ thay ô trống hay cây cỏ có thể thay thế.
- Nếu chưa có nền hợp lệ, giữ trạng thái chờ để thử lại khi mở server hoặc vào game; vẫn tìm quanh điểm spawn đã lưu.
- Tương thích dữ liệu cũ: biên nhận chờ hoặc rỗng từ bản trước sẽ được thử sửa; thế giới đã ghi nhận nhận quà sẽ không được phát thêm. Không xóa hay di chuyển rương đã phát từ trước.

## Thử

Build bằng `./gradlew.bat build`, lấy JAR thường trong `build/libs`. Tạo thế giới mới ở Sinh tồn hoặc Sáng tạo; rương đã có gần điểm spawn ngay khi vào. Xem tọa độ trong chat nếu nhân vật xuất hiện trên cây. Tìm rương trên mặt đất và kiểm tra đuốc xung quanh. Mở lại thế giới hoặc đổi chế độ để kiểm tra không sinh rương thứ hai.

## File

- `server/NovaStarterKit.java`: hàm `onInitialSpawn()` tạo một phần thưởng cho thế giới; `contents()` là danh sách vật phẩm.
- `server/NovaStarterChestPlacement.java`: tìm mặt đất bằng bản đồ độ cao, đặt rương, điền đồ và đặt đuốc.
- `server/NovaStarterRewardsData.java`: lưu vị trí và trạng thái phát một lần, đọc được biên nhận cũ.
- `mixin/NovaStarterSpawnMixin.java`: nối với bước `MinecraftServer.setInitialSpawn()`; khai báo trong `robot-nova_mod.mixins.json`.
- `assets/robot-nova_mod/lang/vi_vn.json` và `en_us.json`: tên rương và thông báo tọa độ.

Dữ liệu vẫn lưu trong `data/robot_nova_starter_rewards.dat` của thế giới. Không cần xóa khi cập nhật mod.
