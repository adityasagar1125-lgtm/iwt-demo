# College Club Management System (CCMS)

This project is implemented from the PRD in `public/CCMS_PRD.docx`.

CCMS is a full-stack web application built with:
- HTML5 + CSS3 + JavaScript (DOM manipulation)
- CSS Box Model, Flexbox, and CSS Grid for layout
- JSON request/response APIs
- Jakarta Servlet (Tomcat 10+)
- JDBC with MySQL
- Maven standard Java project structure

## Tech Mapping to Requirements

- CSS Box Model: cards, forms, tables, announcements, and auth layouts in `src/main/webapp/css/style.css`
- CSS Flexbox: navbar, hero stats, filter bars, form actions, list rows
- CSS Grid: clubs/events cards, dashboard KPI blocks, admin panels
- DOM Manipulation: dynamic rendering, filtering, validation, live updates in `src/main/webapp/js/app.js`
- JSON Response/Request: Fetch API across auth, clubs, memberships, events, announcements, admin stats
- Jakarta Servlet (Tomcat): multi-endpoint Servlet layer under `src/main/java/com/silicon/servlet`
- JDBC: DAO pattern with prepared statements for all database operations

## Standard Project Structure

```text
src/
  main/
    java/
      com/silicon/
        dao/
          ClubDAO.java
          EventDAO.java
        model/
          Club.java
          Event.java
        servlet/
          ClubServlet.java
          EventServlet.java
        util/
          DBConnection.java
    webapp/
      index.html
      login.html
      register.html
      dashboard.html
      clubs.html
      club-detail.html
      events.html
      admin.html
      profile.html
      manage.html (redirect)
      css/
        style.css
      js/
        app.js
      WEB-INF/
        web.xml
schema.sql
pom.xml
```

## Database Setup (MySQL)

1. Create schema and sample data:
   - Run `schema.sql` in MySQL.

2. Configure DB credentials (recommended via environment variables):
   - `CLUBHUB_DB_URL`
   - `CLUBHUB_DB_USER`
   - `CLUBHUB_DB_PASS`

Default fallback (if env vars are not set):
- URL: `jdbc:mysql://localhost:3306/silicon_clubhub?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
- USER: `root`
- PASS: `password`

If MySQL is not running, the app automatically falls back to embedded H2 (file DB at `.local/clubhubdb`) and seeds users, clubs, memberships, events, and announcements.

## Build and Run

1. Build WAR:
```bash
./mvnw clean package
```
(Windows PowerShell: `./mvnw.cmd clean package`)

2. Deploy generated WAR to Tomcat 10+:
- Artifact: `target/ClubHub.war`

3. Open app:
- `http://localhost:8080/ClubHub/`

### Quick localhost (project-local Tomcat)

Use helper scripts:

```powershell
./start-local-tomcat.ps1
./stop-local-tomcat.ps1
```

Optionally rebuild before start:

```powershell
./start-local-tomcat.ps1 -Build
```

## Default Login Accounts (Seed Data)

- Student: `student@ccms.local` / `student123`
- Club Admin: `admin@ccms.local` / `admin123`
- Super Admin: `super@ccms.local` / `super123`

## API Endpoints

### Clubs API
- `GET /api/clubs` -> list clubs
- `GET /api/clubs?id=1` -> single club with members and announcements
- `POST /api/clubs` -> create club
- `PUT /api/clubs` -> update club
- `DELETE /api/clubs?id=1` -> delete club
- `POST /api/clubs/join` -> join or leave membership

### Auth API
- `POST /api/auth/register` -> register student
- `POST /api/auth/login` -> authenticate user
- `GET /api/auth/profile?userId=` -> fetch profile
- `PUT /api/auth/profile` -> update profile

### Events API
- `GET /api/events` -> list events
- `GET /api/events?clubId=1` -> events by club
- `GET /api/events?upcoming=true` -> upcoming events
- `POST /api/events` -> create event
- `PUT /api/events` -> update event
- `DELETE /api/events?id=1` -> delete event
- `POST /api/events/register` -> register student for event

### Announcements API
- `GET /api/announcements` -> global feed
- `GET /api/announcements?clubId=1` -> club feed
- `POST /api/announcements` -> post announcement

### Admin API
- `GET /api/admin/dashboard` -> system analytics summary

## Notes

- `src/main/webapp` is the canonical frontend source used for Tomcat deployment.
- Project root is cleaned to standard Maven/Eclipse web-app layout.
- Visual style intentionally uses system fonts and text-first branding (no generated logo assets or sticker-like icon packs).
