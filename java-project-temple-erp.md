# Temple ERP (Java Project) — `java-project-temple-erp`

A **Temple Enterprise Resource Planning (ERP) System** implemented in **Java** (Spring Boot + Thymeleaf + MongoDB). This project manages temple operations through a centralized, web-based platform for devotees and administrators.

- **GitHub Repository:** [https://github.com/suyog-shirsat2004/Temple-ERP-Java](https://github.com/suyog-shirsat2004/Temple-ERP-Java)
- **Project Report (updated):** [`java-project-temple-erp.pdf`](java-project-temple-erp.pdf)
- **Live Project Link:** _Add your deployed URL here after deployment_ (see [Deploy / Live Link](#deploy--live-link) below)

> **Project basis:** Developed from the *Temple ERP System* academic project specification (2025–2026). The original specification/sample stack referenced PHP/MySQL/WAMP; this repository is a **complete Java-based implementation** of the same Temple ERP concept and modules, extended with additional UI, PDF, email, and performance improvements.

## Project Details

| Item | Details |
|---|---|
| Project | Temple ERP System (Java Project) |
| Developed By | Shirsat Suyog Madhav; Shinde Kiran Uttam |

> `java-project-temple-erp.pdf` is the updated project report for this Java implementation.

## Introduction & Objectives

In the digital era, temples manage devotee services such as darshan passes, room bookings, donations, food services, and feedback. Manual processes can be slow, error-prone, and difficult to track. Temple ERP provides a centralized system to automate and manage these activities efficiently.

**Objectives:**
- Manage temple activities and devotee services from a single platform
- Reduce manual work and improve accuracy/speed
- Maintain secure, organized records in MongoDB
- Provide a user-friendly interface for devotees and administrators
- Enable online darshan passes, room bookings, donations, restaurant/Annakshetra orders, and feedback
- Generate and deliver styled PDF passes/receipts/tokens (and email them when configured)

## Scope

- **Devotee/User:** registration/login, profile management, darshan pass booking, room booking, donations, restaurant/Annakshetra ordering, feedback, notifications, and PDF/token/receipt viewing/download (where permitted).
- **Admin:** dashboard and management of users, darshan passes/types, rooms/bookings, donations, restaurant menu/orders, feedback/contact, login activity, notifications, and email settings.

## Modules / Features

### User (Devotee)
- **Authentication & account:** register, login, forgot/reset password, profile view/update.
- **Profile:** update name/email/mobile/address/gender and profile photo/logo (click avatar/icon or upload in form), single-field updates supported, validation and clear error messages.
- **Darshan passes:** apply for pass, view status/history, view/download token/PDF.
- **Room booking:** book rooms, view bookings/status, view/download booking token/PDF.
- **Donations:** submit donations, view receipts/history, download donation receipt PDF.
- **Restaurant / Annakshetra:** browse menu, place orders, view order/token, download token PDF.
- **Feedback & contact:** submit feedback/contact messages.
- **Notifications:** view notifications and delete selected/all/individual notifications.

### Admin
- **Dashboard & monitoring:** overview, login records/activity.
- **Darshan management:** pass requests, pass types, approve/reject/update, PDF/token actions.
- **Room management:** rooms and booking requests, approve/reject/update, token/PDF actions.
- **Donations:** donation records/approvals, receipt PDF actions.
- **Restaurant:** menu/items and orders, approve/reject/status updates, token/PDF actions (download original generated PDF, same as emailed).
- **Users:** view/manage user records and status.
- **Feedback/Contact/Notifications:** review and manage records.
- **Email settings:** configure SMTP/mail settings (stored in DB/mail settings; can override/fallback to properties).

### Recent Improvements (Java implementation)
- **Faster submissions & admin actions:** pass/booking/donation/restaurant submissions and admin approve/reject/status/send-PDF actions run PDF generation and email sending asynchronously in background where applicable (responses return quickly with “will be emailed shortly” messaging where relevant).
- **PDFs styled like Darshan Pass:** donation receipt, restaurant token, and room booking token PDFs restyled to match the colorful Darshan Pass format (headings/layout), with temple logo support (including `images/temple/icon symbol.png` and fallbacks).
- **PDF optimization:** embedded images are normalized/resized/compressed (profile/logo handling) to keep generated PDFs within target size (under ~5 MB where possible); undecodable non-SVG images are skipped safely to avoid render failures.
- **Email delivery:** improved SMTP handling (Gmail/App Password support), prefers DB `MailSetting` (`mail_settings`) over `application.properties` when configured.
- **Notifications:** select, bulk delete, and single delete from `/notifications` (owner-scoped DB deletion).
- **Profile updates:** avatar-click photo/logo update, partial/single-field updates allowed, better validation/error reporting, and support for updating accounts without forcing all fields/photo.
- **Multiple accounts with same email:** allowed in application logic; on startup the app attempts to drop any unique `users.email` index (`MongoConfig`) and auto-index creation is disabled to avoid startup conflicts. Username remains unique.

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| Framework | Spring Boot 3.x, Spring Web MVC, Spring Security |
| View/UI | Thymeleaf, HTML, CSS, JavaScript, Bootstrap/icons (project assets) |
| Database | MongoDB (Spring Data MongoDB) |
| PDF | openhtmltopdf + Thymeleaf HTML render templates |
| Email | Spring Mail / Jakarta Mail (SMTP, Gmail App Password supported) |
| Build Tool | Maven (wrapper included: `mvnw`, `mvnw.cmd`) |

## Project Structure (key paths)

- `src/main/java/com/temple/erp/` — controllers (`web`, `api`, `admin`), services, repositories, models, config, security, DTO/util
- `src/main/resources/templates/` — Thymeleaf pages (`admin/`, `fragments/`, `pdf/`)
- `src/main/resources/templates/pdf/` — PDF render templates (`pass-render.html`, `donation-receipt-render.html`, `restaurant-token-render.html`, `booking-token-render.html`)
- `src/main/resources/static/` — static web assets
- `images/` — project images served by the app (e.g. `/images/temple/**`, including `icon symbol.png`)
- `src/main/resources/application.properties` — app configuration
- `check/` — sample/generated PDF outputs (if present)
- `java-project-temple-erp.pdf` — updated project report for this Java implementation
- `java-project-temple-erp.md` — project documentation (this file)

## Data / Collections (MongoDB)

Core models/collections (Spring Data MongoDB) include: `users`, `loginrecords`, `darshanpasses`, `passtypes`, `donations`, `rooms`, `roombookings`, `restaurantitems`, `restaurantorders`, `feedbacks`, `contacts`, `notifications`, `mail_settings` (names may vary by model/config; see `model/` and repositories).

## Setup & Run

### Prerequisites
- **Java 17+**
- **Maven** (or use included Maven wrapper)
- **MongoDB** running locally or accessible via URI

### 1) Configure
Edit `src/main/resources/application.properties` (or use environment variables):

- **Server:** `server.port=5000` (default in this project)
- **MongoDB:** `spring.data.mongodb.uri=${MONGO_URI:mongodb://localhost:27017/temple_erp}`
- **Index creation:** `spring.data.mongodb.auto-index-creation=false` (set to avoid startup conflicts with existing indexes; app drops old unique `users.email` index on startup)
- **Mail (optional but recommended for PDF emails):**
  - `app.mail.enabled=true`
  - `app.mail.from` / `spring.mail.username` / `spring.mail.password` (Gmail App Password recommended)
  - Prefer configuring via Admin Email Settings / DB `mail_settings` when available

> **Security:** do not commit real credentials/passwords. Prefer environment variables or admin-managed DB settings in production.

### 2) Run (development)
```bash
# Windows (PowerShell / CMD)
cd "Temple-ERP-Java"
.\mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

### 3) Build & run JAR
```bash
# Windows
.\mvnw.cmd -DskipTests clean package
java -jar target\*.jar

# macOS/Linux
./mvnw -DskipTests clean package
java -jar target/*.jar
```

Open: `http://localhost:5000`

## Important Notes

- **Restart required after code/template changes:** Java and Thymeleaf/PDF template changes take effect after rebuilding/restarting the running app. Regenerate/re-download PDFs (and resend emails if needed) to get latest logo/format/compression.
- **Duplicate email accounts:** application allows multiple accounts with the same email; startup attempts to drop unique `users.email` index automatically. If you still get duplicate-key/E11000 errors, manually drop the email unique index in MongoDB (`db.users.getIndexes()` then `db.users.dropIndex(...)`) and retry.
- **Email ambiguity:** when multiple accounts share the same email, email-based login/forgot-password may match the first account; usernames remain unique.
- **Gmail SMTP:** requires 2FA + App Password; `EmailService` prefers DB `MailSetting` over properties. Verify Admin Email Settings and server logs on send failures.
- **PDFs:** generated from HTML templates via openhtmltopdf; admin/user download endpoints can serve the same generated PDF used for email (e.g. restaurant token `/restaurant/token/{tokenId}/pdf`).

## Deploy / Live Link

This is a Spring Boot + MongoDB application (server port defaults to `5000`, configurable via `server.port` / `PORT`). To create a **live project link**, deploy to a Java-capable host with a MongoDB database (e.g. MongoDB Atlas + Render/Railway/Koyeb/Fly.io/Azure App Service):

1. **Build:** `.\mvnw.cmd -DskipTests clean package` (produces a JAR in `target/`).
2. **Environment variables (recommended):**
   - `MONGO_URI` — MongoDB connection string (e.g. Atlas URI)
   - `PORT` — if required by platform (and set `server.port=${PORT:5000}` or platform mapping)
   - Mail settings (`APP_MAIL_ENABLED`, `APP_MAIL_FROM`, `MAIL_USERNAME`, `MAIL_PASSWORD`, etc.) for PDF/email features
3. **Start command:** `java -jar target/*.jar` (or platform-specific Java/Spring Boot deploy).
4. After deployment, update the **Live Project Link** at the top of this file and in the repository description.

> Note: A live URL cannot be generated from this local workspace without deployment credentials/platform access. Once deployed, all pages/routes should work as in local run when MongoDB, static/images, and environment settings are configured correctly.

## Upload / Push to GitHub

Repository: [https://github.com/suyog-shirsat2004/Temple-ERP-Java](https://github.com/suyog-shirsat2004/Temple-ERP-Java)

```bash
cd "Temple-ERP-Java"
git init
git add .
git commit -m "Initial commit: Temple ERP Java project (java-project-temple-erp)"
git branch -M main
git remote add origin https://github.com/suyog-shirsat2004/Temple-ERP-Java.git
git push -u origin main
```

If the remote already has history, pull/rebase or push as appropriate. Authentication (PAT/SSH) is required for push.

## References

- Project report (updated): [`java-project-temple-erp.pdf`](java-project-temple-erp.pdf)
- GitHub: [suyog-shirsat2004/Temple-ERP-Java](https://github.com/suyog-shirsat2004/Temple-ERP-Java)
- Temple ERP System — Java Spring Boot, Thymeleaf, MongoDB implementation

## License / Usage

Academic project implementation. Use and adapt as permitted by your institution/project guidelines.
