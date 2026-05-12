# 🛒 E-Commerce High-Performance REST API (Spring Boot Backend)

Hệ thống REST API E-Commerce cấp doanh nghiệp (Enterprise-grade) được thiết kế cho các kịch bản chịu tải cao và lượng giao dịch đồng thời lớn. Dự án được tối ưu hóa sâu sắc về mặt kiến trúc giao dịch để chống nghẽn luồng và deadlock khi checkout, kết hợp các chiến lược tối ưu hóa index cơ sở dữ liệu, caching hiệu năng cao và thông báo thời gian thực.

---

## 🛠️ Công Nghệ Sử Dụng

- **Framework**: Java 17 & Spring Boot 3.4.1
- **Bảo mật & Phân quyền**: Spring Security, OAuth2 Resource Server, cơ chế xác thực không trạng thái (stateless) qua JWT.
- **Lưu trữ & Truy vấn**: Spring Data JPA, Hibernate, MySQL, dynamic query filters qua thư viện `com.turkraft.springfilter`.
- **Caching**: Spring Boot Starter Data Redis (tối ưu hóa tốc độ load danh mục và sản phẩm).
- **Hàng đợi & Bất đồng bộ**: Spring Boot Starter AMQP (RabbitMQ queues), Spring Task Executor xử lý gửi email và thông báo ngầm.
- **Cổng Thanh Toán**: VNPay API, PayOS SDK, COD (Thanh toán khi nhận hàng).
- **Thông Báo Thời Gian Thực**: Spring WebSockets (STOMP Protocol), Telegram Bot API.
- **Kiểm Thử (Testing)**: JUnit 5, Mockito, H2 In-Memory Database (đảm bảo test chạy cô lập và độc lập với database vật lý).
- **Tài liệu API**: SpringDoc OpenAPI / Swagger UI.
- **Công cụ hỗ trợ**: Lombok, Apache POI (xuất báo cáo Excel), Cloudinary API (quản lý lưu trữ hình ảnh).

---

## 🏗️ Kiến Trúc & Giải Pháp Tối Ưu Hóa Chịu Tải (High-Concurrency)

Hệ thống được tối ưu hóa đặc biệt nhằm vượt qua các điểm nghẽn hiệu năng trong các đợt Flash Sale hoặc sự kiện mua sắm lớn:

### 1. Triệt Tiêu Deadlock Khi Checkout Đồng Thời (Deterministic Lock Ordering)
* **Vấn đề**: Khi nhiều khách hàng cùng checkout các giỏ hàng chứa các sản phẩm trùng nhau tại cùng một thời điểm, việc khóa dòng dữ liệu (row-level lock) không theo thứ tự nhất định sẽ gây ra tranh chấp chéo (Circular Waiting) dẫn đến lỗi deadlock dữ liệu.
* **Giải pháp**: Trước khi thực hiện khóa và cập nhật kho trong DB, `OrderService` tự động sắp xếp danh sách sản phẩm cần mua (`cartItems`) theo ID tăng dần của thực thể bị khóa vật lý (`Product` hoặc `ProductVariant` ID). Quy tắc sắp xếp nhất quán này triệt tiêu hoàn toàn điều kiện xảy ra deadlock chéo giữa các giao dịch đồng thời.

### 2. Tách Biệt Chu Kỳ Giao Giao Dịch DB (Transaction Phase-Separation)
* **Vấn đề**: Việc gọi API HTTP bên ngoài (như tạo link thanh toán VNPay, PayOS) tốn rất nhiều thời gian (vài giây). Nếu thực hiện các cuộc gọi mạng này bên trong khối `@Transactional`, kết nối database (Connection) và các khóa dòng sẽ bị chiếm giữ quá lâu, gây cạn kiệt Connection Pool và làm treo hệ thống.
* **Giải pháp**: Quy trình checkout được bóc tách và phân lớp rõ rệt:
  - **Phase 1: DB Transaction (`createOrderTransaction`)**: Thực hiện các thao tác ghi DB cục bộ nhanh, kiểm tra và giữ hàng (reserve) trong kho, áp dụng mã giảm giá, và khởi tạo bản ghi thanh toán "PENDING". Giao dịch này commit và đóng lại ngay lập tức để giải phóng kết nối database.
  - **Phase 2: Network / HTTP calls (`createOrder` wrapper)**: Thực hiện các cuộc gọi API bên ngoài đến cổng thanh toán ngoài phạm vi transaction của database.
  - **Phase 3: Async Notifications**: Kích hoạt gửi email xác nhận và thông báo WebSocket/Telegram một cách bất đồng bộ để không chặn luồng xử lý chính của người dùng.

### 3. Giữ Chỗ Kho Hàng Nguyên Tử (Atomic Stock Reservation)
* **Vấn đề**: Truy vấn kiểm tra kho (`getCurrentStock`) sau đó mới thực hiện trừ kho bằng một câu lệnh riêng biệt vừa tạo ra thêm 1 vòng truy vấn DB (DB round-trip) thừa thãi, vừa dễ gây ra tình trạng bán quá số lượng (overselling) do tranh chấp dữ liệu (race conditions).
* **Giải pháp**: Tích hợp kiểm tra và giữ kho thành một xử lý nguyên tử duy nhất thông qua hàm `reserveStock()`. Cơ chế này cắt giảm 50% số truy vấn vào DB trong quá trình checkout và bảo vệ kho hàng an toàn trước tình trạng mua quá số lượng hiện có.

### 4. Tối Ưu Chỉ Mục (Database Indexes) & API Danh Mục
* **Database Indexes**: Cấu hình các index quan trọng trên các cột thường xuyên được truy vấn để đạt tốc độ phản hồi tính bằng mili-giây:
  - `idx_product_name` trên cột `name` của bảng `products`.
  - `idx_product_category` trên cột `category_id` của bảng `products`.
  - `idx_product_brand` trên cột `brand_id` của bảng `products`.
- **API Danh Mục Tối Ưu**: Cung cấp endpoint `/api/v1/categories/all` kết hợp lưu bộ nhớ cache để trả về toàn bộ cây danh mục ngay lập tức cho menu điều hướng của khách hàng mà không cần thực hiện truy vấn phân trang nặng nề lặp đi lặp lại.

---

## 📂 Cấu Trúc Thư Mục Chính

```text
Ecommerce/
├── src/
│   ├── main/
│   │   ├── java/com/tuna/ecommerce/
│   │   │   ├── config/             # Cấu hình Security, Async, WebSocket, Redis, v.v.
│   │   │   ├── controller/         # Các REST Controllers cung cấp API endpoints
│   │   │   ├── domain/             # Các Thực thể JPA (Entities), DTOs, Requests/Responses
│   │   │   ├── repository/         # Giao tiếp cơ sở dữ liệu qua Spring Data JPA Repositories
│   │   │   ├── service/            # Nơi thực hiện toàn bộ Logic Nghiệp vụ & Các luồng tối ưu
│   │   │   └── ultil/              # Constants, Custom Exceptions, Helpers chuyên dụng
│   │   └── resources/
│   │       ├── application.properties # File cấu hình chính của Spring Boot
│   │       └── templates/          # Giao diện email Thymeleaf (Email xác nhận, hóa đơn)
│   └── test/                       # Bộ kiểm thử cô lập sử dụng H2 Database và Mockito
├── Dockerfile                      # Cấu hình container hóa ứng dụng
├── docker-compose.yml              # Điều phối chạy MySQL, Redis, RabbitMQ cục bộ qua Docker
├── pom.xml                         # Quản lý thư viện phụ thuộc và build plugin của Maven
└── mvnw / mvnw.cmd                 # Maven Wrapper hỗ trợ build đa nền tảng
```

---

## 🚀 Hướng Dẫn Khởi Chạy

### Yêu Cầu Hệ Thống
- **Java SE Development Kit (JDK) 17** hoặc cao hơn.
- **MySQL Database**.
- **Redis Server** (khuyến nghị có để bật cache).
- **RabbitMQ Broker** (khuyến nghị có cho hàng đợi bất đồng bộ).

### 1. Cấu Hình Database
Cập nhật thông tin kết nối DB của bạn trong file `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db?createDatabaseIfNotExist=true&useSSL=false
spring.datasource.username=username_cua_ban
spring.datasource.password=password_cua_ban
```

### 2. Biên Dịch và Khởi Chạy
Chạy lệnh sau tại thư mục gốc `Ecommerce` để khởi động ứng dụng:

**Trên Windows (PowerShell)**:
```powershell
.\mvnw spring-boot:run
```

**Trên Linux / macOS**:
```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Ứng dụng sẽ được khởi chạy mặc định tại cổng `8080`.

---

## 🧪 Chạy Bộ Kiểm Thử (Unit Tests)

Bộ kiểm thử đơn vị được thiết lập độc lập và không phụ thuộc database ngoài nhờ sử dụng H2 Database. Để chạy toàn bộ test suite và xuất báo cáo độ ổn định:

```powershell
.\mvnw test
```

Tất cả 5 kịch bản kiểm thử tích hợp (đặc biệt là quy trình đặt hàng, kiểm tra hết hàng, và xử lý mã giảm giá) sẽ tự động thực thi và trả về kết quả thành công.

---

## 📖 Tài Liệu Swagger UI

Dự án tự động sinh tài liệu Swagger trực quan cho tất cả các API endpoints. Khi ứng dụng đang chạy, bạn truy cập tại:

`http://localhost:8080/swagger-ui/index.html` (Nếu chạy ở port 8080)
