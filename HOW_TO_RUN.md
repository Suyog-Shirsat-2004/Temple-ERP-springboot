# How to Run — Temple ERP System (Full-Stack Java)

This is a **Java-based Temple ERP System** (Spring Boot 3.3, Java 17, Thymeleaf, MongoDB, Spring Security). All application code in this project is Java/server-side Java web stack (with Thymeleaf/HTML/CSS/JS for the UI). Project information/modules are based on `suyog project.pdf` (Temple ERP System).

 # 0 . Run the application (runtime)

Open PowerShell in the project folder (**requires Java 17+**):

```powershell
cd "D:\MCS 1\Project Java\Temple-ERP-Java"
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"   # if needed
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"

.\mvnw.cmd spring-boot:run



```


## 1. Prerequisites

| Requirement | Version | Check (PowerShell) |
|---|---:|---|
| Java JDK | 17 or newer (**required**) | `java -version` |
| Maven | 3.9+ *(optional if using wrapper)* | `mvn -version` |
| MongoDB | 5+ (local) **or** a MongoDB URI | `mongod --version` (local) |

> The project includes the **Maven Wrapper** (`mvnw.cmd` / `mvnw`). If **Java 17+** is installed, you can run without installing Maven system-wide. **Java is still required.**

### If `java` / `JAVA_HOME` is not recognized
```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"   # adjust to your JDK 17 path
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
java -version
```
Reopen PowerShell after installing the JDK so PATH updates apply.

### Install on Windows (if missing)
```powershell
# JDK 17 (required)
winget install --id EclipseAdoptium.Temurin.17.JDK -e --source winget

# Optional: system Maven (not required if you use mvnw.cmd)
winget install --id Apache.Maven -e --source winget

# Optional: local MongoDB
winget install --id MongoDB.Server -e --source winget
```

## 2. Start MongoDB

**Option A — Local MongoDB (default)**
1. Start the MongoDB service (Windows Services: “MongoDB Server”) **or** run `mongod`.
   - PowerShell (Admin): `Start-Service MongoDB` (service name may vary)
   - cmd (Admin): `net start MongoDB`
2. Default connection: `mongodb://localhost:27017/temple_erp` (database `temple_erp` is created automatically).

**Option B — MongoDB URI (Atlas/remote)**
Set `MONGO_URI` before running (see step 3).

## 3. Configure (optional)

Defaults work for local development. Edit `src/main/resources/application.properties` or set environment variables:

```powershell
$env:MONGO_URI = "mongodb://localhost:27017/temple_erp"
$env:JWT_SECRET = "replace-with-a-long-random-secret"
# $env:UPLOAD_DIR = "uploads"
```

**Default admin (change in production):**
- **Username:** `Admin123` (or `templeerp2026@gmail.com`)
- **Password:** `admin@123`

Configured via `app.admin.username`, `app.admin.email`, `app.admin.password` in `application.properties`.

## 4. Run the application (runtime)

Open PowerShell in the project folder (**requires Java 17+**):

```powershell
cd "D:\MCS 1\Project Java\Temple-ERP-Java"
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"   # if needed
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd spring-boot:run
```

- Wait for **`Tomcat started on port 5000`** / **`Started TempleErpApplication`**.
- On startup the app prints **access URLs** in the console — **Ctrl+Click** a URL (VS Code/Windows Terminal) to open it.
- Keep the terminal open while using the site. Stop with **`Ctrl+C`**.

> First run may download Maven/the wrapper JAR (needs internet). If you installed Maven system-wide, `mvn spring-boot:run` also works.

### Alternative: build a JAR, then run
```powershell
cd "D:\MCS 1\Project Java\Temple-ERP-Java"
.\mvnw.cmd -DskipTests package
java -jar target\temple-erp-java-1.0.0.jar
```

## 5. Open the app (URLs)

Default base URL: **http://localhost:5000**

| Area | URL |
|---|---|
| Home (temple_erp-style, animations) | http://localhost:5000/ or http://localhost:5000/home |
| About | http://localhost:5000/about |
| Services hub | http://localhost:5000/services |
| Temple Updates | http://localhost:5000/temple-updates |
| News / Events / Festivals | http://localhost:5000/news · http://localhost:5000/events · http://localhost:5000/festivals |
| Devotee login / Register / Forgot password | http://localhost:5000/login · /register · /forgot-password |
| Devotee dashboard | http://localhost:5000/dashboard |
| Darshan Pass (book/history/detail) | http://localhost:5000/passes/new · /passes/history · /passes/{id} |
| Room Booking | http://localhost:5000/bookings/new · /bookings/history |
| Donations | http://localhost:5000/donations |
| Restaurant / Orders | http://localhost:5000/restaurant · /restaurant/history |
| Notifications / Profile / Change password | http://localhost:5000/notifications · /profile · /change-password |
| Admin login / Dashboard | http://localhost:5000/admin/login · /admin/dashboard |
| Admin modules | /admin/users, /admin/passes, /admin/bookings, /admin/donations, /admin/restaurant, /admin/rooms, /admin/devotees, /admin/reports, /admin/festivals, /admin/events, /admin/news |
| REST API health | http://localhost:5000/api/health |

**temple_erp route aliases (Java redirects):** `/darshan_pass`, `/darshan_pass/new`, `/darshan_pass/history`, `/pass_history`, `/my_passes`, `/darshan_pass/view_pass/{id}`, `/room_booking`, `/room_bookings_history`, `/my_bookings`, `/donations/donate`, `/my_donations`, etc.

After opening, **hard refresh** with `Ctrl+F5` if styles/templates look stale.

## 6. Images / Static Assets (important)

- **Project-root images folder:** `d:\MCS 1\Project Java\Temple-ERP-Java\images\` is served at **`/images/**`** (configured in `WebMvcConfig`, with classpath fallback). This folder is used for temple images, gallery assets, room/restaurant/payment images, icons, and PDFs (e.g., `/images/arati.pdf`, `/images/HERITAGE_GUIDELINES_FOR_TRAYAMBAKESHWAR.pdf`, `/images/temple/icon-symbol.webp`).
- **Static assets:** `src/main/resources/static/assets/**` served at `/assets/**` (e.g., home/gallery hero images).
- **Uploads:** `app.upload.dir` (default `uploads/`) served at `/uploads/**`.

If an image appears missing, confirm the file exists under `images/` or `src/main/resources/static/` and restart/hard-refresh.

## 7. Quick test flow

1. Open home: http://localhost:5000/ — verify hero animations, `ॐ`/sacred text, gallery, guidelines modal, PDFs/images.
2. Register a devotee at `/register`, then log in at `/login`.
3. Book a pass (`/passes/new`), make a donation (`/donations`), book a room (`/bookings/new`), place a restaurant order (`/restaurant`), view dashboard (`/dashboard`), notifications, and profile/change password.
4. Log in as admin at `/admin/login` (**`Admin123` / `admin@123`**), open `/admin/dashboard`, and manage passes/bookings/donations/users.
5. Check public pages: `/about`, `/services`, `/temple-updates`, `/news`, `/events`, `/festivals`.

## 8. Troubleshooting

- **Port 5000 in use** (`Web server failed to start. Port 5000 was already in use`): stop the existing process, then start again:
  ```powershell
  netstat -ano | findstr ":5000" | findstr "LISTENING"   # find PID
  taskkill /PID <PID> /F
  .\mvnw.cmd spring-boot:run
  ```
  Or change `server.port` in `application.properties`.
- **`ERR_CONNECTION_REFUSED`**: the app is not running — start it with `.\mvnw.cmd spring-boot:run` and wait for `Started TempleErpApplication`.
- **MongoDB connection error** (`MongoSocketOpenException` / `Connection refused: localhost:27017`): ensure MongoDB is running and `MONGO_URI`/`spring.data.mongodb.uri` is correct.
- **`mvn`/`java` not recognized** or **`JAVA_HOME is not set`**: install **JDK 17+** (required), set `JAVA_HOME`/PATH, then use `.\mvnw.cmd spring-boot:run`. **Reopen PowerShell** after installing.
- **Whitelabel 500 on a page**: check the `spring-boot:run` terminal for the stack trace (`Caused by:`). If you changed Java code/config/templates in a way that requires restart, restart the app; hard refresh (`Ctrl+F5`).
- **401 on `/api/**`**: protected API endpoints require `Authorization: Bearer <token>` (get token via `POST /api/auth/login` or `/api/auth/admin/login`).
- **Missing rooms/menu data**: add `rooms` / `restaurantmenus` documents in MongoDB (collection names per the models) or extend admin UI/API.
- **Images not loading**: verify files under `d:\MCS 1\Project Java\Temple-ERP-Java\images\` or `src\main\resources\static\`; `/images/**` is served from the project-root `images` folder by `WebMvcConfig`.

## 9. Use as backend for the original React frontend (optional)

The REST API preserves `/api/*` paths. In the MERN frontend `.env` set:

```env
VITE_API_URL=http://localhost:5000/api
```

Then run the React app as usual. CORS is enabled for `/api/**`.
