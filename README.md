# Temple ERP – Enterprise Resource Planning System

<h2>🚀 Live Project Demo</h2>

**[🌐 CLICK HERE TO OPEN LIVE TEMPLE ERP PROJECT](https://smile-costume-salaries-himself.trycloudflare.com)**

*Note: This is a temporary Cloudflare Tunnel link. It works only while the local application and tunnel are running.*

**Local Live:** [http://localhost:5000/](http://localhost:5000/) *(when running locally)*

---

## Project Overview

Temple ERP (Enterprise Resource Planning) is a Java Spring Boot-based web application designed to digitize and simplify temple management operations. It provides a centralized platform for devotees and administrators to manage temple services efficiently.

### Key Modules & Features
- **Darshan Passes:** Apply, track, approve/reject, and generate/download pass PDFs/tokens.
- **Room Bookings:** Book temple rooms, manage booking status, and generate/download booking tokens/PDFs.
- **Donations & Payments:** Submit and manage donations/payments with receipts and PDF generation.
- **Restaurant / Annakshetra Orders:** Browse menu, place orders, manage orders, and generate/download order tokens/PDFs.
- **Admin Operations:** Dashboard and management for users, passes, bookings, rooms, donations, restaurant/menu/orders, feedback/contact, notifications, login activity, and email settings.
- **User Services:** Registration/login, profile management (photo/logo & details), notifications, feedback/contact, and service history.
- **System Highlights:** Styled PDF generation (Darshan Pass format with logo), email delivery (SMTP/Gmail App Password), async processing for faster actions, and responsive web pages.

## Technology Stack

- **Backend:** Java 17+, Spring Boot 3.x, Spring Web MVC, Spring Security
- **Frontend:** Thymeleaf, HTML, CSS, JavaScript, Bootstrap/icons
- **Database:** MongoDB (Spring Data MongoDB)
- **PDF & Email:** openhtmltopdf, Spring Mail / Jakarta Mail
- **Build/Deploy:** Maven (`mvnw`/`mvnw.cmd`), JAR, Docker

## Project Links

- **Live Project:** https://smile-costume-salaries-himself.trycloudflare.com
- **GitHub Repository:** https://github.com/suyog-shirsat2004/Temple-ERP-Java
- **Project Report:** [View PDF](https://github.com/suyog-shirsat2004/Temple-ERP-Java/blob/main/java-project-temple-erp.pdf)
- **Detailed Documentation:** [Read Documentation](https://github.com/suyog-shirsat2004/Temple-ERP-Java/blob/main/java-project-temple-erp.md)

## Run Locally

```bash
# Prerequisites: Java 17+, Maven (or wrapper), MongoDB
# Configure MONGO_URI / mail settings in src/main/resources/application.properties or environment variables

.\mvnw.cmd spring-boot:run
# Open: http://localhost:5000
```

Build: `.\mvnw.cmd -DskipTests clean package` → `java -jar target/*.jar` (or use included `Dockerfile`).

## Academic Project – 2026–27

Developed as an academic project for the 2026–27 academic year.

**Developed By:** Shirsat Suyog Madhav; Shinde Kiran Uttam
