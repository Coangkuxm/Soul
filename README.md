# SOUL - Social Entertainment Network

## Giới thiệu

SOUL là ứng dụng mạng xã hội trên nền tảng Android cho phép người dùng kết nối, chia sẻ sở thích cá nhân và các hoạt động giải trí như nghe nhạc, xem phim với bạn bè và cộng đồng.

Ứng dụng được xây dựng nhằm tạo ra một môi trường tương tác thân thiện, hiện đại và tập trung vào việc chia sẻ trải nghiệm giải trí giữa những người dùng có cùng sở thích.

---

## Công nghệ sử dụng

### Mobile App

* Kotlin
* Android Studio
* MVVM Architecture
* Retrofit
* Glide
* Coroutines

### Backend

* Node.js
* Express.js
* JWT Authentication
* RESTful API

### Database

* PostgreSQL
* Neon Database

### Deployment

* Render

---

## Chức năng chính

### Người dùng

* Đăng ký tài khoản
* Đăng nhập / Đăng xuất
* Quản lý hồ sơ cá nhân
* Đăng bài viết
* Chỉnh sửa bài viết
* Xóa bài viết
* Bình luận bài viết
* Thích bài viết
* Theo dõi người dùng
* Nhắn tin trực tiếp
* Nhận thông báo
* Báo cáo nội dung vi phạm

### Quản trị viên

* Quản lý người dùng
* Quản lý nội dung vi phạm
* Xử lý báo cáo

---

## Kiến trúc hệ thống

Client (Android Kotlin)
↓
REST API
↓
Node.js + Express
↓
PostgreSQL (Neon)

---

# Hướng dẫn cài đặt

## Yêu cầu

* Android Studio Hedgehog hoặc mới hơn
* JDK 17
* Node.js 18+
* PostgreSQL hoặc Neon Database
* Git

---

## Clone dự án

```bash
git clone https://github.com/your-username/SOUL.git
```

---

## Cài đặt Backend

Di chuyển vào thư mục backend:

```bash
cd backend
```

Cài đặt thư viện:

```bash
npm install
```

Tạo file `.env`

```env
PORT=3000

DATABASE_URL=postgresql://username:password@host/database

JWT_SECRET=your_secret_key
```

Khởi động server:

```bash
npm start
```

Hoặc:

```bash
npm run dev
```

Backend mặc định chạy tại:

```text
http://localhost:3000
```

---

## Cài đặt Android App

Mở Android Studio.

Chọn:

```text
File -> Open
```

Mở thư mục:

```text
SOUL/android-app
```

Cập nhật URL API trong:

```kotlin
Constants.kt
```

Ví dụ:

```kotlin
const val BASE_URL = "http://10.0.2.2:3000/"
```

Sau đó:

```text
Sync Gradle
Build Project
Run App
```

---

## Tài khoản thử nghiệm

```text
Email: demo@soul.com
Password: 123456
```

---

## Hình ảnh demo

Thêm ảnh giao diện tại đây:

* Login Screen
* Home Screen
* Profile Screen
* Chat Screen
* Notification Screen

---

## Thành viên thực hiện

Sinh viên: Hoàng Văn Quang

Trường: Đại học Công nghệ GTVT

Đề tài:

Phân tích, thiết kế và xây dựng ứng dụng mạng xã hội SOUL
