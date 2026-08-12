🌐 **Ngôn ngữ**: [English](README.md) | **Tiếng Việt**

---

# MelodyShop Microservices Monorepo 🎵

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot 3.2](https://img.shields.io/badge/Spring_Boot-3.2.x-6DB33F.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud 2023](https://img.shields.io/badge/Spring_Cloud-2023.0.x-blue.svg?style=flat-square&logo=spring)](https://spring.io/projects/spring-cloud)
[![MariaDB 10.11](https://img.shields.io/badge/MariaDB-10.11-003545.svg?style=flat-square&logo=mariadb)](https://mariadb.org/)
[![Elasticsearch 8.13](https://img.shields.io/badge/Elasticsearch-8.13-005571.svg?style=flat-square&logo=elasticsearch)](https://www.elastic.co/)
[![ELK Stack](https://img.shields.io/badge/ELK_Stack-Logstash_%7C_Kibana-005571.svg?style=flat-square&logo=elasticstack)](https://www.elastic.co/elk-stack)
[![GitHub Actions CI](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF.svg?style=flat-square&logo=githubactions)](https://github.com/features/actions)
[![Docker Compose](https://img.shields.io/badge/Docker-Compose-2496ED.svg?style=flat-square&logo=docker)](https://www.docker.com/)
[![AI Powered](https://img.shields.io/badge/AI-Gemini_2.0_%7C_Ollama-purple.svg?style=flat-square&logo=googlegemini)](https://deepmind.google/technologies/gemini/)

Hệ thống Backend Microservices hoàn chỉnh cho Nền tảng Thương mại Điện tử **MelodyShop** (Bán Nhạc Cụ & Thiết Bị Âm Nhạc). Dự án bao gồm **13 Microservices** chuyên biệt, tuân thủ nguyên tắc thiết kế **Database-per-Service**, quản lý qua **Netflix Eureka Service Discovery** và bảo mật tập trung tại **Spring Cloud API Gateway**.

---

## 🚀 Điểm Sáng Kỹ Thuật & Nền Tảng Công Nghệ (Engineering Highlights)

| Hạng mục | Công nghệ / Công cụ | Mô tả & Lợi ích kỹ thuật |
|---|---|---|
| **Service Discovery** | **Netflix Eureka Server** (`:8761`) | Tự động đăng ký, phát hiện dịch vụ và theo dõi healthcheck thời gian thực của 13 microservices. |
| **API Gateway & Auth Filter** | **Spring Cloud Gateway** (`:8080`) | Cổng giao tiếp duy nhất xử lý JWT Validation Filter tập trung, Dynamic Routing, CORS & Rate Limiting. |
| **Ghi Log Tập Trung (Centralized Logging)** | **ELK Stack** (`docker-compose.elk.yml`) | Tích hợp Logstash (TCP/UDP Log Appender), Elasticsearch 8.13 & Kibana Dashboard (`:5601`) để truy vấn log tập trung. |
| **Tự động hóa CI/CD** | **GitHub Actions** (`ci.yml`) | Pipeline tự động hóa build Maven Java 21 multi-module, chạy unit test và publish Docker Image lên GitHub Container Registry (GHCR). |
| **Quản lý Database Schema** | **Flyway Migration** (22+ Scripts) | Quản lý phiên bản cơ sở dữ liệu (Database Schema Versioning) và tự động seed data ban đầu cho các DBs. |
| **Giao Tiếp Liên Service** | **Spring Cloud OpenFeign** | Giao tiếp khai báo REST Client giữa Order, Cart, Inventory, Payment và Notification Services. |
| **Mapping DTO ↔ Entity** | **MapStruct** | Chuyển đổi dữ liệu chuẩn kiểu an toàn (Type-safe mapping), compile-time generation với chi phí CPU gần như bằng 0. |
| **Tài liệu API Tương tác** | **Swagger / OpenAPI 3** | Tự động tạo giao diện trải nghiệm API tương tác (`/swagger-ui.html`) cho từng microservice. |
| **Trí Tuệ Nhân Tạo & Định Danh Sinh Trắc** | **Gemini 2.0 / Ollama & Face Auth** | Tư vấn mua sắm nhạc cụ thông minh bằng AI (LLM) và xác thực đăng nhập bằng sinh trắc học khuôn mặt. |

---

## 🏛️ Sơ đồ Kiến trúc Hệ thống (Architecture Diagram)

```mermaid
graph TD
    classDef client fill:#e0f2fe,stroke:#0284c7,stroke-width:2px,color:#0369a1;
    classDef gateway fill:#f3e8ff,stroke:#9333ea,stroke-width:2px,color:#6b21a8;
    classDef infra fill:#fef3c7,stroke:#d97706,stroke-width:2px,color:#92400e;
    classDef core fill:#dcfce7,stroke:#16a34a,stroke-width:2px,color:#15803d;
    classDef engagement fill:#fce7f3,stroke:#db2777,stroke-width:2px,color:#9d174d;
    classDef ai fill:#ffedd5,stroke:#ea580c,stroke-width:2px,color:#c2410c;
    classDef db fill:#f1f5f9,stroke:#64748b,stroke-width:1px,color:#334155;

    Client["💻 Client Apps<br/>(React SPA / Mobile / Postman)"]:::client
    
    subgraph Infrastructure ["🛡️ Hạ tầng Spring Cloud & Observability"]
        Gateway["🌐 API Gateway<br/>(:8080)<br/><i>JWT Auth Filter & Routing</i>"]:::gateway
        Eureka["🔍 Eureka Server<br/>(:8761)<br/><i>Service Registry & Discovery</i>"]:::infra
        ELK["📊 ELK Stack<br/>(:5601 Kibana)<br/><i>Logstash + Elasticsearch</i>"]:::infra
    end

    subgraph CoreServices ["🛒 E-Commerce Microservices"]
        Auth["🔑 Auth Service (:8081)"]:::core
        User["👤 User Service (:8084)"]:::core
        Product["🎸 Product Service (:8082)"]:::core
        Inventory["📦 Inventory Service (:8086)"]:::core
        Cart["🛒 Cart Service (:8091)"]:::core
        Order["📜 Order Service (:8092)"]:::core
        Payment["💳 Payment Service (:8087)"]:::core
    end

    subgraph SupportingServices ["📣 Search & Media Microservices"]
        Notif["📧 Notification Service (:8085)"]:::engagement
        Engage["⭐ Customer Engagement (:8088)"]:::engagement
        Search["🔎 Search Service (:8090)"]:::engagement
        Media["🖼️ Media Service (:8089)"]:::engagement
    end

    subgraph AIServices ["🤖 Smart AI & Biometrics"]
        AIService["🧠 AI Service (:8093)<br/><i>Gemini 2.0 / Ollama</i>"]:::ai
        FaceService["📸 Face Service (:8097)<br/><i>Facial Auth Recognition</i>"]:::ai
    end

    subgraph StorageLayer ["🗄️ Persistence & Infrastructure Layer"]
        AuthDB[("auth_db")]:::db
        UserDB[("user_db")]:::db
        ProductDB[("product_db")]:::db
        InventoryDB[("inventory_db")]:::db
        CartDB[("cart_db")]:::db
        OrderDB[("order_db")]:::db
        PaymentDB[("payment_db")]:::db
        ESDB[("Elasticsearch 8.13")]:::db
        Cloudinary[("Cloudinary CDN")]:::db
    end

    %% Client Interactions
    Client -->|HTTP / REST| Gateway

    %% Discovery Registers
    Gateway <-->|Register & Discover| Eureka
    Auth <--> Eureka
    User <--> Eureka
    Product <--> Eureka
    Inventory <--> Eureka
    Cart <--> Eureka
    Order <--> Eureka
    Payment <--> Eureka
    Notif <--> Eureka
    Engage <--> Eureka
    Search <--> Eureka
    Media <--> Eureka
    AIService <--> Eureka

    %% Gateway Routing
    Gateway -->|/api/auth/**| Auth
    Gateway -->|/api/users/**| User
    Gateway -->|/api/products/**| Product
    Gateway -->|/api/inventory/**| Inventory
    Gateway -->|/api/cart/**| Cart
    Gateway -->|/api/orders/**| Order
    Gateway -->|/api/payments/**| Payment
    Gateway -->|/api/notifications/**| Notif
    Gateway -->|/api/engagement/**| Engage
    Gateway -->|/api/search/**| Search
    Gateway -->|/api/media/**| Media
    Gateway -->|/api/ai/**| AIService

    %% Inter-service Feign Communication
    Order -.->|OpenFeign| Cart
    Order -.->|OpenFeign| Inventory
    Order -.->|OpenFeign| Product
    Order -.->|OpenFeign| Payment
    Order -.->|OpenFeign| Notif
    Auth -.->|HTTP Client| FaceService

    %% Log Appender
    CoreServices -.->|TCP Log Appender| ELK

    %% Service Database Attachments
    Auth --> AuthDB
    User --> UserDB
    Product --> ProductDB
    Inventory --> InventoryDB
    Cart --> CartDB
    Order --> OrderDB
    Payment --> PaymentDB
    Search --> ESDB
    Media --> Cloudinary
```

---

## 📊 Trạng Thái Thực Tế Trên Eureka Service Discovery Dashboard

Các microservices tự động phát hiện và đăng ký với máy chủ **Netflix Eureka Server** (`http://localhost:8761`). Dưới đây là ảnh chụp màn hình thực tế (Authentic Live Screenshot) từ giao diện Eureka Server đang chạy thời gian thực trên môi trường local (hiển thị các microservices như `API-GATEWAY`, `AI-SERVICE`, `NOTIFICATION-SERVICE`, `MEDIA-SERVICE`, `CUSTOMER-ENGAGEMENT-SERVICE` đã kết nối và báo trạng thái `UP`):

![Spring Cloud Eureka Dashboard Live Screenshot](docs/assets/eureka-dashboard.png)

---

## 🛠️ Danh Sách Chi Tiết 13 Microservices

Hệ thống được chia nhỏ thành 13 thành phần độc lập giúp đạt khả năng mở rộng (scalability), dễ bảo trì và phân tách trách nhiệm nghiệp vụ rõ ràng:

| # | Service Name | Port | Database / Storage | Trách Nhiệm & Công Nghệ Nổi Bật | API Path chính |
|---|---|:---:|---|---|---|
| **1** | **`eureka-server`** | `8761` | In-Memory Registry | Máy chủ Service Registry & Discovery (Netflix Eureka), tự động phát hiện và theo dõi healthcheck của 12 services còn lại. | `/` |
| **2** | **`api-gateway`** | `8080` | None | Gateway duy nhất điều hướng request, xử lý CORS, Rate Limiting và áp dụng Centralized JWT Filter xác thực người dùng. | `/api/**` |
| **3** | **`auth-service`** | `8081` | MariaDB (`auth_db`) | Quản lý định danh (User Identity), Đăng ký/Đăng nhập, Cấp phát & Validate JWT (Access/Refresh Tokens), Phân quyền RBAC. | `/api/auth/**` |
| **4** | **`user-service`** | `8084` | MariaDB (`user_db`) | Quản lý thông tin hồ sơ cá nhân (Profile), danh sách sổ địa chỉ giao hàng và quản trị tài khoản dành cho Admin. | `/api/users/**` |
| **5** | **`product-service`** | `8082` | MariaDB (`product_db`) | Quản lý danh mục sản phẩm (Categories), Thương hiệu (Brands), Biến thể SP (Variants) và Thông số kỹ thuật (Specs JSON). | `/api/products/**` |
| **6** | **`inventory-service`** | `8086` | MariaDB (`inventory_db`) | Quản lý kho hàng (Warehouses), Giữ hàng/Trừ tồn kho khi đặt đơn (Stock Reservation), Cảnh báo hàng sắp hết và Số Serial. | `/api/inventory/**` |
| **7** | **`cart-service`** | `8091` | MariaDB (`cart_db`) | Quản lý giỏ hàng thời gian thực (Shopping Cart), tự động tính toán tổng tiền và đồng bộ giỏ hàng theo phiên đăng nhập. | `/api/cart/**` |
| **8** | **`order-service`** | `8092` | MariaDB (`order_db`) | Quản lý quy trình đặt hàng (Order Orchestration), chuyển trạng thái State Machine (`PENDING` ➔ `DELIVERED`), Thống kê doanh thu. | `/api/orders/**` |
| **9** | **`payment-service`** | `8087` | MariaDB (`payment_db`) | Tích hợp các cổng thanh toán (VietQR/SePay, Stripe, COD), xử lý Webhook và lưu vết lịch sử giao dịch (Audit Logs). | `/api/payments/**` |
| **10** | **`notification-service`**| `8085` | In-Memory Log | Gửi email bất đồng bộ (Welcome email, Email xác nhận đơn hàng, OTP Password Reset) qua Spring Boot Mail (SMTP). | `/api/notifications/**`|
| **11** | **`customer-engagement-service`** | `8088` | MariaDB (`engagement_db`)| Quản lý Đánh giá & Báo giá sản phẩm (Reviews/Ratings), Danh sách yêu thích (Wishlist) và Mã giảm giá (Coupons/Vouchers). | `/api/engagement/**` |
| **12** | **`search-service`** | `8090` | Elasticsearch 8.13 | Tìm kiếm toàn văn (Full-text search), lọc đa tiêu chí (danh mục, mức giá, thương hiệu, đánh giá) trên Elasticsearch. | `/api/search/**` |
| **13** | **`ai-service` & `face-service`** | `8093` / `8097` | Vector DB / AI Engine | Trợ lý tư vấn mua sắm bằng AI (Gemini 2.0 / Ollama Llama 3.2) & Xác thực đăng nhập sinh trắc học khuôn mặt (Facial Auth). | `/api/ai/**` |

---

## 🗄️ Kiến Trúc Dữ Liệu (Database-per-Service & Flyway)

Hệ thống tuân thủ nghiêm ngặt nguyên tắc **Database-per-Service**, quản lý phiên bản tự động bằng **Flyway Migration** (22+ migration scripts):

```
melodyshop-microservices/
├── 🔑 auth-db        (MariaDB Port 3307) -> Flyway: V1__init_auth.sql (Users, Roles, RefreshTokens)
├── 👤 user-db        (MariaDB Port 3310) -> Flyway: V1__init_user.sql (Profiles, Addresses)
├── 🎸 product-db     (MariaDB Port 3308) -> Flyway: V1__init_product.sql (Categories, Brands, Variants)
├── 📦 inventory-db   (MariaDB Port 3309) -> Flyway: V1__init_inventory.sql (Warehouses, Stock, Logs)
├── 🛒 cart-db        (MariaDB Port 3313) -> Flyway: V1__init_cart.sql (Carts, Items)
├── 📜 order-db       (MariaDB Port 3312) -> Flyway: V1__init_order.sql (Orders, Items, Timeline)
├── 💳 payment-db     (MariaDB Port 3311) -> Flyway: V1__init_payment.sql (Payments, Webhook logs)
└── 🔎 search-index   (Elasticsearch 9200)-> Replicated Product Index
```

---

## 🚀 Hướng Dẫn Chạy Dự Án (Quick Start Guide)

### Bước 1: Khởi tạo tệp môi trường
```bash
cp .env.example .env
```

### Bước 2: Khởi chạy hệ thống bằng Docker Compose

- **Chạy chế độ chuẩn (Microservices + Databases)**:
  ```bash
  docker-compose up -d --build
  ```

- **Chạy kèm ELK Stack (Ghi log tập trung & Kibana Dashboard)**:
  ```bash
  docker-compose -f docker-compose.yml -f docker-compose.elk.yml up -d
  ```

### Bước 3: Kiểm tra trạng thái hệ thống
- **Eureka Registry Dashboard**: [http://localhost:8761](http://localhost:8761)
- **Kibana Log Dashboard (nếu bật ELK)**: [http://localhost:5601](http://localhost:5601)
- **API Gateway Base URL**: `http://localhost:8080`

---

## 🧪 Danh Mục API Endpoint Chi Tiết (API Reference)

Toàn bộ API được gọi thông qua **Base URL**: `http://localhost:8080`.

### 1. Auth Service (`/api/auth/**`)
| Method | Endpoint | Mô tả | Authorization |
|:---:|:---|:---|:---:|
| **POST** | `/api/auth/register` | Đăng ký tài khoản khách hàng mới | Public |
| **POST** | `/api/auth/login` | Đăng nhập nhận Access Token & Refresh Token | Public |
| **POST** | `/api/auth/refresh` | Làm mới Access Token khi hết hạn | Refresh Token |
| **POST** | `/api/auth/logout` | Đăng xuất và vô hiệu hóa token | Bearer Token |
| **GET** | `/api/auth/me` | Lấy thông tin tài khoản hiện tại | Bearer Token |

### 2. User Service (`/api/users/**`)
| Method | Endpoint | Mô tả | Authorization |
|:---:|:---|:---|:---:|
| **GET** | `/api/users/profile` | Lấy thông tin cá nhân chi tiết (Profile) | Bearer Token |
| **PUT** | `/api/users/profile` | Cập nhật thông tin cá nhân (Họ tên, SĐT, Avatar) | Bearer Token |
| **GET** | `/api/users/addresses` | Danh sách sổ địa chỉ nhận hàng | Bearer Token |
| **POST** | `/api/users/addresses` | Thêm địa chỉ nhận hàng mới | Bearer Token |

### 3. Product Service (`/api/products/**`)
| Method | Endpoint | Mô tả | Authorization |
|:---:|:---|:---|:---:|
| **GET** | `/api/products` | Danh sách sản phẩm (Phân trang, lọc, sắp xếp) | Public |
| **GET** | `/api/products/{id}` | Xem chi tiết thông tin sản phẩm & biến thể | Public |
| **GET** | `/api/products/categories` | Danh sách cây danh mục nhạc cụ | Public |
| **POST** | `/api/products` | (Admin) Thêm sản phẩm mới | Admin Token |

### 4. Cart & Order Service (`/api/cart/**` & `/api/orders/**`)
| Method | Endpoint | Mô tả | Authorization |
|:---:|:---|:---|:---:|
| **GET** | `/api/cart` | Lấy thông tin giỏ hàng hiện tại | Bearer Token |
| **POST** | `/api/cart/items` | Thêm sản phẩm vào giỏ hàng | Bearer Token |
| **POST** | `/api/orders` | Khởi tạo đơn hàng từ giỏ hàng | Bearer Token |
| **GET** | `/api/orders/{id}` | Xem chi tiết trạng thái đơn hàng | Bearer Token |

### 5. Payment & AI Service (`/api/payments/**` & `/api/ai/**`)
| Method | Endpoint | Mô tả | Authorization |
|:---:|:---|:---|:---:|
| **POST** | `/api/payments/vietqr` | Tạo mã QR chuyển khoản ngân hàng VietQR | Bearer Token |
| **POST** | `/api/ai/chat` | Trò chuyện với Trợ lý AI tư vấn chọn mua nhạc cụ | Public / User |

---

## 📂 Postman Collection & Testing Guide

Thư mục gốc chứa sẵn file cấu hình Postman: **`MelodyShop_Postman_Collection.json`**.
1. **Import** file `MelodyShop_Postman_Collection.json` vào Postman.
2. Thực hiện luồng test chuẩn:
   `1. Register` ➔ `2. Login` (Postman tự động lưu `accessToken` vào Environment Variable) ➔ `3. Test Profile/Cart/Orders`.

---

💻 **MelodyShop Microservices** — *Kiến trúc hiện đại, khả năng mở rộng cao, sẵn sàng cho sản xuất.*
