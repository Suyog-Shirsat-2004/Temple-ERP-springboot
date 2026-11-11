# Temple ERP – Enterprise Resource Planning System

## 🚀 Live Project

**Live URL (Render):** _Add the actual Render deployment URL here after successful deployment and verification (for example: `https://temple-erp.onrender.com`)._

> **Status:** Permanent public hosting is configured for **Render** (see [Deployment Instructions](#deployment-instructions-render)). Do not treat this README as confirmation that deployment is complete until the Render URL above is added and tested.

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
- **Build / Deployment:** Maven (wrapper included), Docker, Render

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

## Deployment Instructions (Render)

This project is prepared for deployment on **Render** as a Docker-based web service with **MongoDB Atlas**.

### 1. Prepare MongoDB Atlas (production database)
1. Create a MongoDB Atlas account/project and cluster: https://www.mongodb.com/atlas
2. Create a database user with a strong password.
3. Configure **Network Access** to allow Render to connect (for testing you may allow access from anywhere, but restrict in production as appropriate).
4. Get the connection string (SRV URI), e.g.:
   `mongodb+srv://<username>:<password>@<cluster>.mongodb.net/temple_erp?retryWrites=true&w=majority&appName=TempleERP`
5. Set this value as `MONGO_URI` in Render (do not commit it).

### 2. Deploy to Render
You can deploy using the included [`render.yaml`](render.yaml) blueprint or configure manually.

#### Option A: Blueprint (`render.yaml`)
1. Push your code to GitHub.
2. In Render, create a new **Blueprint** and connect the repository.
3. Render will detect `render.yaml` and propose a web service.
4. Fill in required secret/sync environment variables (especially `MONGO_URI`, mail credentials, admin credentials if overriding).
5. Deploy and wait for build/startup logs to complete.

#### Option B: Manual Web Service
1. In Render, create a **New + > Web Service** from the GitHub repo.
2. Choose **Docker** runtime (Dockerfile is included).
3. Set **Health Check Path** to `/api/health`.
4. Configure environment variables (see below).
5. (Recommended) Add a **Persistent Disk** and mount it (e.g. `/var/data`) and set `UPLOAD_DIR=/var/data/uploads` so uploaded files persist across restarts/redeploys (plan-dependent).

### 3. Required / recommended Render environment variables
| Variable | Required | Description |
|---|---|---|
| `PORT` | Yes (Render provides) | App listens on Render's port (`server.port=${PORT:5000}` already configured) |
| `MONGO_URI` | Yes | MongoDB Atlas connection string |
| `JWT_SECRET` | Yes | Long random secret for JWT signing |
| `APP_ADMIN_USERNAME` | Recommended | Admin login username (override default) |
| `APP_ADMIN_EMAIL` | Recommended | Admin email (override default) |
| `APP_ADMIN_PASSWORD` | Recommended | Admin password (override default) |
| `APP_MAIL_ENABLED` | Optional | `true`/`false` to enable SMTP email notifications |
| `APP_MAIL_FROM` | Optional | Sender email address |
| `MAIL_HOST` | Optional | SMTP host (default `smtp.gmail.com`) |
| `MAIL_PORT` | Optional | SMTP port (default `587`) |
| `MAIL_USERNAME` | Optional | SMTP username/email |
| `MAIL_PASSWORD` | Optional | SMTP password/app password (Gmail App Password recommended) |
| `UPLOAD_DIR` | Recommended | Upload path (use mounted disk path in production, e.g. `/var/data/uploads`) |
| `THYMELEAF_CACHE` | Optional | `true` recommended in production |

> Use Render **Environment** settings (or secret files) for all credentials. Never commit secrets to GitHub.

### 4. Build/start behavior (Docker)
- **Docker build:** uses multi-stage Maven build (`Dockerfile`)
- **Container start:** runs the packaged Spring Boot JAR
- **Port binding:** app reads `PORT` environment variable (Render-compatible)
- **Health check:** `/api/health`

### 5. Post-deployment verification checklist
- Open the Render URL and verify home page loads
- Test login/registration and admin login/dashboard
- Test MongoDB-backed operations (bookings, donations, passes, orders)
- Test PDF downloads and QR verification
- Test email sending (if SMTP configured)
- Test all routes/APIs and file uploads
- Verify the site works without your local PC running

## Configuration Notes

- **MongoDB:** Production should use `MONGO_URI` (MongoDB Atlas). Local default is `mongodb://localhost:27017/temple_erp`.
- **Email:** Can be configured via environment variables and/or Admin > Email Settings (project feature). Use Gmail App Passwords for SMTP.
- **Uploads:** Stored on local filesystem (`UPLOAD_DIR`). On Render, attach a persistent disk for durability across restarts; otherwise uploads may be lost on redeploy/restart.
- **Reverse proxy:** `server.forward-headers-strategy` is configurable for correct behavior behind Render's proxy.

## Documentation

- **Detailed Project Documentation:** [`java-project-temple-erp.md`](java-project-temple-erp.md)
- **Project Report (PDF):** [`java-project-temple-erp.pdf`](java-project-temple-erp.pdf)
- **Local Run Guide:** [`HOW_TO_RUN.md`](HOW_TO_RUN.md)

## Developers

- Shirsat Suyog Madhav
- Shinde Kiran Uttam
