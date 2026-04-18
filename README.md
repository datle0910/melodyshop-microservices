# MelodyShop Microservices 🎵

Dự án Hệ thống Backend Microservices cho Melody Shop (Bán Nhạc Cụ / Âm Nhạc).
Dự án được xây dựng dựa trên kiến trúc tiên tiến **Spring Cloud / Spring Boot 3 / MariaDB / Docker**.

---

## 🏗 Kiến trúc Mạng (Architecture)
- **Eureka Server** (`:8761`): Máy chủ danh bạ, tự động nhận diện và quản lý các services.
- **API Gateway** (`:8080`): Cổng giao tiếp duy nhất ra bên ngoài. **Toàn bộ API test qua cổng này.**
- **Auth Service** (`:8081`): Xử lý thực thể User, Auth, và JWT.
- **User Service** (`:8084`): Quản lý thông tin chi tiết (Profile) và Địa chỉ.
- **Healthcheck Actuator**: Mỗi service cung cấp endpoint `/actuator/health` để theo dõi trạng thái.

---

## 🚀 Hướng Dẫn Chạy Dự Án (Dành cho Team)

### Bước 1: Khởi tạo biến môi trường
1. Copy file `.env.example` -> `.env`.
2. Điền các tham số cần thiết (có thể dùng mặc định để chạy ở Local).

### Bước 2: Chạy hệ thống
```bash
docker-compose up -d --build
```
*Đợi khoảng 45-60 giây cho hệ thống khởi động hoàn toàn (Xem trạng thái tại [http://localhost:8761](http://localhost:8761)).*

---

## 🧪 Hướng Dẫn Test API CHI TIẾT

Toàn bộ API sử dụng **Base URL**: `http://localhost:8080` (API Gateway).

### 1. Auth Service API (`/api/auth/**`)
Quản lý đăng ký, đăng nhập và định danh.

| Method | Endpoint | Mô tả | Yêu cầu Auth |
|:---:|:---|:---|:---:|
| **POST** | `/api/auth/register` | Đăng ký tài khoản mới | Không |
| **POST** | `/api/auth/login` | Đăng nhập nhận Access Token & Refresh Token | Không |
| **POST** | `/api/auth/refresh` | Làm mới Access Token bằng Refresh Token | Có (RT) |
| **POST** | `/api/auth/logout` | Đăng xuất, hủy bỏ Token | Có |
| **GET** | `/api/auth/me` | Lấy thông tin tài khoản đang đăng nhập | Có |

**Ví dụ Request Body (Register):**
```json
{
    "fullName": "Nguyen Van A",
    "email": "test@gmail.com",
    "password": "password123",
    "phone": "0987654321"
}
```

---

### 2. User Service API (`/api/users/**`)
Quản lý hồ sơ cá nhân và địa chỉ giao hàng.

#### Hồ sơ người dùng (Profiles)
| Method | Endpoint | Mô tả | Yêu cầu Auth |
|:---:|:---|:---|:---:|
| **GET** | `/api/users/profile` | Lấy thông tin chi tiết Profile | Có |
| **PUT** | `/api/users/profile` | Cập nhật thông tin Profile (Họ tên, SĐT, Avatar) | Có |

#### Địa chỉ giao hàng (Addresses)
| Method | Endpoint | Mô tả | Yêu cầu Auth |
|:---:|:---|:---|:---:|
| **GET** | `/api/users/addresses` | Danh sách địa chỉ của tôi | Có |
| **POST** | `/api/users/addresses` | Thêm địa chỉ mới | Có |
| **PUT** | `/api/users/addresses/{id}` | Sửa địa chỉ | Có |
| **DELETE** | `/api/users/addresses/{id}` | Xóa địa chỉ | Có |
| **PUT** | `/api/users/addresses/{id}/default` | Đặt làm địa chỉ mặc định | Có |

---

### 3. API Dành Cho Quản Trị Viên (Admin - `/api/users/admin/**`)
*Yêu cầu tài khoản có Role: `ROLE_ADMIN`*

| Method | Endpoint | Mô tả |
|:---:|:---|:---|
| **GET** | `/api/users/admin` | Liệt kê toàn bộ người dùng trong hệ thống |
| **GET** | `/api/users/admin/{id}` | Xem chi tiết thông tin 1 người dùng bất kỳ |

---

### 4. Healthcheck & Monitoring
Dùng để kiểm tra trạng thái sống/chết của các Service và Database.

- **Gateway Health**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Auth Health**: [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health) (Kiểm tra cả MariaDB Auth)
- **User Health**: [http://localhost:8084/actuator/health](http://localhost:8084/actuator/health) (Kiểm tra cả MariaDB User)

---

## 📂 Sử dụng Postman Collection (Khuyên dùng)
Tôi đã chuẩn bị sẵn file `MelodyShop_Postman_Collection.json` trong thư mục gốc.
1. **Import** file này vào Postman.
2. Chuỗi luồng test chuẩn: `Register` -> `Login` (Sau khi Login, Postman sẽ tự lưu `accessToken` vào biến môi trường) -> Test các API `Profile` và `Addresses`.

🚀 Chúc bạn phát triển tính năng thuận lợi!

---

## 💻 Test nhanh bằng cURL (Copy & Paste)

Dành cho những bạn muốn test nhanh qua Terminal mà không cần mở Postman.

### 1. Đăng ký tài khoản (Register)
```bash
curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
           "fullName": "Test User",
           "email": "test@gmail.com",
           "password": "password123",
           "phone": "0123456789"
         }'
```

### 2. Đăng nhập (Login)
```bash
curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
           "email": "test@gmail.com",
           "password": "password123"
         }'
```
*(Sau lệnh này, hãy copy chuỗi `accessToken` trong kết quả trả về để dùng cho các lệnh bên dưới).*

### 3. Xem Profile (Yêu cầu Token)
Thay `YOUR_TOKEN_HERE` bằng chuỗi token bạn vừa nhận được:
```bash
curl -X GET http://localhost:8080/api/users/profile \
     -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 4. Kiểm tra Healthcheck (Sức khỏe hệ thống)
```bash
# Kiểm tra Gateway
curl http://localhost:8080/actuator/health

# Kiểm tra Auth Service & DB
curl http://localhost:8081/actuator/health

# Kiểm tra User Service & DB
curl http://localhost:8084/actuator/health
```
