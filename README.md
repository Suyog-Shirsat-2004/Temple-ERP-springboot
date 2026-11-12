# Temple ERP – Enterprise Resource Planning System

## 🚀 Live Project

- **Deployed Page (GitHub Pages):** [https://suyog-shirsat2004.github.io/Temple-ERP-Java/](https://suyog-shirsat2004.github.io/Temple-ERP-Java/)
- **Project Showcase:** Static landing page for the Temple ERP project (hosted from `docs/` via GitHub Actions)

> **Note:** GitHub Pages hosts a **static project landing/showcase page** only. The full Temple ERP application is a **Java Spring Boot + MongoDB** backend app (authentication, bookings, donations, restaurant orders, PDFs, QR, email, uploads, admin operations) and requires a Java server/database to run all features. You can run it locally using the instructions below or deploy the backend to a Java/Docker hosting platform with MongoDB Atlas.

## Project Overview

Temple ERP is a Java Spring Boot-based web application designed to digitize and simplify temple management operations. It provides a centralized platform for devotees and administrators to manage darshan passes, room bookings, donations, restaurant orders, payments, notifications, and administrative workflows.

## Project Features

- **User Registration and Login** (devotee) and **Admin Login & Dashboard**
- **Darshan Pass Booking** with status tracking, admin approval/rejection, and pass details
- **QR Code Generation and Verification** for approved darshan passes
- **Room Booking** management with booking history and booking tokens/PDFs
- **Donation Management** with records and downloadable donation receipts (PDF)
- **Restaurant Menu and Multi-item Orders**, billing/tokens, and order history
- **Payment Integration** and transaction/payment status tracking (as implemented in the project)
- **PDF Generation & Downloads** (darshan pass, room booking token, donation receipt, restaurant order token)
- **Gmail/SMTP Notifications** with PDF attachments (configurable via environment variables or admin settings)
- **User Profile & Management** (profile details/photo support where applicable)
- **Admin User Management** and operational modules (users/devotees, passes, bookings, rooms, donations, restaurant/menu/orders, reports, content/settings)
- **Booking and Transaction History** for users and admin views
- **Responsive UI** for mobile, tablet, laptop, and desktop screens

## Technology Stack

- **Backend:** Java 17, Spring Boot 3.3, Spring Web MVC, Spring Security, Spring Data MongoDB, Spring Mail
- **Frontend:** Thymeleaf, HTML, CSS, JavaScript, Bootstrap (selected pages), Font Awesome
- **Database:** MongoDB (local for development, MongoDB Atlas for production)
- **PDF / QR:** openhtmltopdf, OpenPDF, PDFBox, ZXing
- **Build / Deployment:** Maven (wrapper included), Docker, GitHub Pages (static showcase)

## Screenshots

_Add screenshots of key pages here (Home, Login/Register, User Dashboard, Admin Dashboard, Darshan Pass, Room Booking, Donation, Restaurant, Payment, PDF/Token views, Profile, Admin Management)._

Suggested locations to capture:
- `Home` (`/`)
- `Login` (`/login`) and `Register` (`/register`)
- `User Dashboard` (`/dashboard`)
- `Admin Dashboard` (`/admin/dashboard`)
- `Darshan Pass` (`/passes/new`, `/passes/history`)
- `Room Booking` (`/bookings/new`, `/bookings/history`)
- `Donation` (`/donations`)
- `Restaurant` (`/restaurant`)
- `Payment / Receipt / PDF` pages

## Project Links

- **GitHub Repository:** https://github.com/suyog-shirsat2004/Temple-ERP-Java
- **Project Report (PDF):** [View Project Report](https://github.com/suyog-shirsat2004/Temple-ERP-Java/blob/main/java-project-temple-erp.pdf)
- **Detailed Documentation:** [Read Documentation](https://github.com/suyog-shirsat2004/Temple-ERP-Java/blob/main/java-project-temple-erp.md)
- **Local Application (development):** `http://localhost:5000/` (run locally; not a public live link)

## Academic Project – 2026–27

This project is developed as an academic project for the **2026–27 academic year**, based on the Temple ERP System concept. The implementation uses Java and Spring Boot with additional features such as QR-based verification, PDF generation, email notifications, and improved management workflows.

## Installation Instructions (Local)

### Prerequisites
- **Java JDK 17+**
- **MongoDB** (local instance) or a MongoDB connection URI
- **Maven** (optional; Maven Wrapper is included)

### 1. Clone the repository
```bash
git clone https://github.com/suyog-shirsat2004/Temple-ERP-Java.git
cd Temple-ERP-Java
```

### 2. Configure environment (recommended)
Set environment variables (or use local defaults for development). See [`.env.example`](.env.example).

Minimum for local development:
- `MONGO_URI=mongodb://localhost:27017/temple_erp` (default)
- Optional mail settings (`APP_MAIL_ENABLED`, `MAIL_*`) for email/PDF notifications

> **Security note:** Do not hardcode database credentials, email passwords, API keys, or payment secrets in source code.

### 3. Run the application
**Windows (PowerShell):**
```powershell
.\mvnw.cmd spring-boot:run
```

**macOS/Linux:**
```bash
./mvnw spring-boot:run
```

### 4. Open locally
- Home: `http://localhost:5000/`
- User Login: `http://localhost:5000/login`
- Admin Login: `http://localhost:5000/admin/login`
- Health Check: `http://localhost:5000/api/health`

### 5. Build a production JAR (optional)
```bash
# Windows
.\mvnw.cmd -DskipTests clean package

# macOS/Linux
./mvnw -DskipTests clean package

java -jar target/temple-erp-java-1.0.0.jar
```

## GitHub Pages (Static Showcase)

This repository includes a static showcase site in `docs/` for GitHub Pages:
- **Live URL:** https://suyog-shirsat2004.github.io/Temple-ERP-Java/
- **Source:** `main` branch, `/docs` folder (deployed via GitHub Actions workflow: `.github/workflows/pages.yml`)

To publish/update:
1. Go to **Settings → Pages** and set **Source** to **GitHub Actions**.
2. Run or wait for the **Deploy GitHub Pages (docs)** workflow on `main`.
3. Open the live URL above.

> GitHub Pages shows the project showcase/pages overview only. It does not run the Spring Boot backend, MongoDB, authentication, payments, PDF/QR generation, or email services.

## Application Pages / Routes (Full Project)

### Public pages
- `/`, `/home` — Home
- `/about` — About temple
- `/services` — Services
- `/events` — Events
- `/festivals` — Festivals
- `/news` — News / updates
- `/temple-updates` — Temple updates
- `/login` — User login
- `/register` — User registration
- `/forgot-password` — Forgot/reset password
- `/admin/login` — Admin login
- `/passes/verify/{token}` — Public pass verification (QR/token)

### User pages (authenticated)
- `/dashboard` — User dashboard
- `/profile` — User profile
- `/change-password` — Change password
- `/notifications` — Notifications
- `/passes/new` — Book darshan pass
- `/passes/history` — My passes
- `/passes/{id}` — Pass details
- `/passes/{id}/pdf` — Download pass PDF
- `/bookings/new` — Room booking
- `/bookings/history` — My room bookings
- `/bookings/token/{id}` — Booking token
- `/bookings/token/{id}/pdf` — Booking token PDF
- `/donations` — Donations / my donations
- `/donations/receipt/{id}` — Donation receipt
- `/donations/receipt/{id}/pdf` — Donation receipt PDF
- `/restaurant` — Restaurant menu / place order
- `/restaurant/history` — My restaurant orders
- `/restaurant/token/{id}` — Order token
- `/restaurant/token/{id}/pdf` — Order token PDF

### Admin pages (admin role)
- `/admin/dashboard` — Admin dashboard
- `/admin/users` — User management
- `/admin/devotees` — Devotee management
- `/admin/passes` — Darshan pass management
- `/admin/bookings` — Room booking management
- `/admin/rooms` — Room management
- `/admin/donations` — Donation management
- `/admin/restaurant` — Restaurant/menu/order management
- `/admin/reports` — Reports
- `/admin/events` — Events management
- `/admin/festivals` — Festivals management
- `/admin/news` — News management
- `/admin/mail-settings` — Email settings

### API / system
- `/api/health` — Health check endpoint
- `/api/**` — REST API endpoints (auth/content/passes/bookings/donations/restaurant/admin operations)

## Backend Hosting (for full working app)

To run the complete application with all features, deploy the Spring Boot backend to a Java/Docker-capable hosting platform and connect **MongoDB Atlas**.

### Recommended environment variables
| Variable | Description |
|---|---|
| `PORT` | Server port (app supports `${PORT:5000}`) |
| `MONGO_URI` | MongoDB Atlas connection string (required for production DB) |
| `JWT_SECRET` | Long random secret for JWT signing |
| `APP_ADMIN_USERNAME` / `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` | Bootstrap admin credentials (override defaults) |
| `APP_MAIL_ENABLED`, `APP_MAIL_FROM`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP/email settings for notifications and PDF attachments |
| `UPLOAD_DIR` | Upload storage path (use persistent storage path on hosts that support volumes/disks) |
| `THYMELEAF_CACHE` | Set `true` in production |

> Do not commit secrets to GitHub. Configure credentials as environment variables on your hosting platform or via Admin > Email Settings where applicable.

## Configuration Notes

- **MongoDB:** Production should use `MONGO_URI` (MongoDB Atlas). Local default is `mongodb://localhost:27017/temple_erp`.
- **Email:** Can be configured via environment variables and/or Admin > Email Settings (project feature). Use Gmail App Passwords for SMTP.
- **Uploads:** Stored on local filesystem (`UPLOAD_DIR`). Use persistent storage on your hosting platform if you need uploaded files to survive restarts/redeploys.
- **Reverse proxy:** `server.forward-headers-strategy` is configurable for deployments behind a proxy/load balancer.

## Documentation

- **Detailed Project Documentation:** [`java-project-temple-erp.md`](java-project-temple-erp.md)
- **Project Report (PDF):** [`java-project-temple-erp.pdf`](java-project-temple-erp.pdf)
- **Local Run Guide:** [`HOW_TO_RUN.md`](HOW_TO_RUN.md)

## Developers

- Shirsat Suyog Madhav
- Shinde Kiran Uttam
