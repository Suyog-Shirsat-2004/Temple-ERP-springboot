# Temple-ERP-Java

**Live Project:** [Add Live Demo Link Here](#deploy--live-link) *(deploy to Render/Railway/Koyeb/Fly.io/Azure with MongoDB Atlas to generate the live URL)*

Java Spring Boot based Temple ERP System for managing Darshan Passes, Room Bookings, Donations, Restaurant Orders, Payments, and Admin Operations.

- **GitHub Repository:** [https://github.com/suyog-shirsat2004/Temple-ERP-Java](https://github.com/suyog-shirsat2004/Temple-ERP-Java)
- **Project Report (PDF):** [`java-project-temple-erp.pdf`](java-project-temple-erp.pdf)
- **Detailed Docs:** [`java-project-temple-erp.md`](java-project-temple-erp.md)

> **Project basis:** Developed from the *Temple ERP System* academic project specification (2025–2026). The original specification/sample stack referenced PHP/MySQL/WAMP; this repository is a **complete Java-based implementation** of the same Temple ERP concept and modules, extended with additional UI, PDF, email, and performance improvements.

## Academic / Project Details

| Item | Details |
|---|---|
| Project | Temple ERP System (Java Project) |
| Submitted to | Savitribai Phule Pune University — Bachelor of Computer Science |
| Institute | K.K. Wagh Arts, Science and Commerce College, Pimpalgaon (Baswant) |
| Academic Year | 2025–2026 |
| Developed By | Shirsat Suyog Madhav; Shinde Kiran Uttam |
| Guided By | Prof. A. S. Mokat Mam |
| HOD | Mrs. M. B. Sonawane Mam |

## Overview

In the digital era, temples manage devotee services such as darshan passes, room bookings, donations, food/Annakshetra services, payments, and feedback. Manual processes can be slow, error-prone, and difficult to track. **Temple ERP** provides a centralized web-based platform to automate and manage these activities efficiently for devotees and administrators.

**Objectives**
- Manage temple activities and devotee services from a single platform
- Reduce manual work and improve accuracy/speed
- Maintain secure, organized records in MongoDB
- Provide a user-friendly interface for devotees and administrators
- Enable online darshan passes, room bookings, donations, restaurant/Annakshetra orders, and feedback
- Generate and deliver styled PDF passes/receipts/tokens (and email them when configured)

## Features / Modules

### Devotee (User)
- **Authentication & account:** register, login, forgot/reset password, profile view/update.
- **Profile:** update name/email/mobile/address/gender and profile photo/logo (click avatar/icon or upload), single-field/partial updates supported, validation and clear error messages.
- **Darshan Passes:** apply for pass, view status/history, view/download token/PDF.
- **Room Booking:** book rooms, view bookings/status, view/download booking token/PDF.
- **Donations:** submit donations (with payment details as applicable), view receipts/history, download donation receipt PDF.
- **Restaurant / Annakshetra:** browse menu, place orders/payments, view order/token, download token PDF.
- **Feedback & contact:** submit feedback/contact messages.
- **Notifications:** view notifications and delete selected/all/individual notifications.

### Admin Operations
- **Dashboard & monitoring:** overview, login records/activity.
- **Darshan management:** pass requests, pass types, approve/reject/update, PDF/token actions.
- **Room management:** rooms and booking requests, approve/reject/update, token/PDF actions.
- **Donations & payments:** donation/payment records and approvals, receipt PDF actions.
- **Restaurant:** menu/items and orders, approve/reject/status updates, token/PDF actions (download original generated PDF, same as emailed).
- **Users:** view/manage user records and status.
- **Feedback/Contact/Notifications:** review and manage records.
- **Email settings:** configure SMTP/mail settings (DB `mail_settings` preferred over properties when configured).

### Key Implementation Highlights
- **Faster submissions & admin actions:** PDF generation and email sending run asynchronously in background where applicable.
- **Darshan Pass–styled PDFs:** donation receipt, restaurant token, and room booking token PDFs match the colorful pass format with temple logo support (e.g. `images/temple/icon symbol.png`) and fallbacks.
- **PDF optimization:** embedded images normalized/resized/compressed (target under ~5 MB where possible); undecodable non-SVG images handled safely.
- **Email delivery:** improved SMTP handling (Gmail/App Password), prefers DB `MailSetting` over `application.properties`.
- **Notifications:** owner-scoped select, bulk, and single delete (`/notifications`).
- **Profile:** avatar-click photo/logo update, partial/single-field updates, clearer validation/error reporting.
- **Accounts:** multiple accounts can share the same email (startup drops unique `users.email` index; `spring.data.mongodb.auto-index-creation=false`). Username remains unique.
- **Admin downloads:** original generated PDFs available (same as emailed), e.g. restaurant token `/restaurant/token/{tokenId}/pdf`.

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| Framework | Spring Boot 3.x, Spring Web MVC, Spring Security |
| View/UI | Thymeleaf, HTML, CSS, JavaScript, Bootstrap/icons |
| Database | MongoDB (Spring Data MongoDB) |
| PDF | openhtmltopdf + Thymeleaf HTML render templates |
| Email | Spring Mail / Jakarta Mail (SMTP, Gmail App Password) |
| Build Tool | Maven (`mvnw`, `mvnw.cmd`) |
| Deployment | JAR / Docker (`Dockerfile` included) |

## Project Structure

- `src/main/java/com/temple/erp/` — controllers (`web`, `api`, admin), services, repositories, models, config, security, DTO/util
- `src/main/resources/templates/` — Thymeleaf pages (`admin/`, `fragments/`, `pdf/`)
- `src/main/resources/templates/pdf/` — PDF render templates (`pass-render.html`, `donation-receipt-render.html`, `restaurant-token-render.html`, `booking-token-render.html`)
- `src/main/resources/static/` — static web assets
- `images/` — project images served by the app (e.g. `/images/temple/**`)
- `src/main/resources/application.properties` — configuration
- `java-project-temple-erp.md` — detailed documentation
- `java-project-temple-erp.pdf` — updated project report

## MongoDB Collections (core)

`users`, `loginrecords`, `darshanpasses`, `passtypes`, `donations`, `rooms`, `roombookings`, `restaurantitems`, `restaurantorders`, `feedbacks`, `contacts`, `notifications`, `mail_settings` (see `model/` and repositories).

## Setup & Run (Local)

### Prerequisites
- Java 17+
- Maven (or included wrapper)
- MongoDB (local or URI)

### Configure
Edit `src/main/resources/application.properties` or set environment variables:
- `server.port=${PORT:5000}` (default `5000`)
- `spring.data.mongodb.uri=${MONGO_URI:mongodb://localhost:27017/temple_erp}`
- `spring.data.mongodb.auto-index-creation=false`
- Mail (optional): `app.mail.enabled`, `app.mail.from`, `spring.mail.*` / DB `mail_settings` (Gmail: 2FA + App Password)

> **Security:** do not commit real credentials. Prefer env vars or admin-managed DB settings.

### Run
```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

### Build JAR
```bash
.\mvnw.cmd -DskipTests clean package
java -jar target/*.jar
```

Open: `http://localhost:5000`

### Docker
```bash
docker build -t temple-erp-java .
docker run -p 5000:5000 -e MONGO_URI="mongodb://..." temple-erp-java
```

## Deploy / Live Link

**Live Project (1st link):** _Publish your deployed URL here after hosting._

1. Create MongoDB database (e.g. **MongoDB Atlas**) and set `MONGO_URI`.
2. Deploy to a Java/Docker platform (**Render / Railway / Koyeb / Fly.io / Azure App Service**):
   - Build: `.\mvnw.cmd -DskipTests clean package`
   - Start: `java -jar target/*.jar` or deploy via `Dockerfile`
   - Env vars: `MONGO_URI`, `PORT` (if required), mail vars as needed
3. Verify pages/routes, PDFs, uploads, and email settings.
4. Replace the **Live Project** placeholder at the top of this README and in the GitHub repo description/website.

## Important Notes

- **Restart/rebuild** after Java/template changes; regenerate PDFs to get latest styling/logo/compression.
- **Duplicate emails:** allowed; startup attempts to drop unique `users.email` index. If E11000 occurs, drop index manually (`db.users.getIndexes()` / `db.users.dropIndex(...)`) and restart.
- **Email:** `EmailService` prefers DB `MailSetting`; Gmail requires App Password. Check logs on send failures.

## Repository & References

- **GitHub:** [https://github.com/suyog-shirsat2004/Temple-ERP-Java](https://github.com/suyog-shirsat2004/Temple-ERP-Java)
- **Report:** [`java-project-temple-erp.pdf`](java-project-temple-erp.pdf)
- **Docs:** [`java-project-temple-erp.md`](java-project-temple-erp.md)
- Savitribai Phule Pune University — B.Sc. (Computer Science), 2025–2026

## License / Usage

Academic project implementation. Use and adapt as permitted by your institution/project guidelines.
