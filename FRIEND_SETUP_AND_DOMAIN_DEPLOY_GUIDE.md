# ClubHub Complete Setup and Domain Deployment Guide

This guide is written so your friend can:
1. Install from ZIP
2. Run locally on their own PC
3. Connect to a real domain
4. Make code changes and publish those changes to the domain

--------------------------------------------------

## Super Easy Start (Eclipse User)

If you already have Eclipse, follow only these 8 steps first:

1. Install Java JDK 17.
2. Unzip the project.
3. Open Eclipse.
4. Import project:
	File -> Import -> Maven -> Existing Maven Projects -> select project folder -> Finish.
5. In Eclipse, open Terminal (or Windows PowerShell) at project root.
6. Run:

	.\start-local-tomcat.ps1 -Build

7. Open browser:

	http://localhost:8080/ClubHub/

8. After code changes, run again:

	.\start-local-tomcat.ps1 -Build

Stop server when done:

.\stop-local-tomcat.ps1

If browser shows old data, press Ctrl + F5.

--------------------------------------------------

## 1) What your friend needs before starting

Install these tools on the friend PC:

- Git
- Java JDK 17
- Maven is optional (project has Maven Wrapper)
- MySQL 8 (optional, because app can fallback to H2 locally)

Quick checks in PowerShell:

java -version
git --version

--------------------------------------------------

## 2) Unzip and open project

1. Unzip project folder.
2. Open Eclipse.
3. Import as Maven project:
	File -> Import -> Maven -> Existing Maven Projects.
4. Open terminal in project root.

Expected root contains:

- mvnw.cmd
- pom.xml
- schema.sql
- src/main
- start-local-tomcat.ps1
- stop-local-tomcat.ps1

--------------------------------------------------

## 3) Database setup (recommended)

If using MySQL:

1. Create database and tables by running schema.sql.
2. Set these environment variables before run:

PowerShell (current terminal session only):

$env:CLUBHUB_DB_URL="jdbc:mysql://localhost:3306/silicon_clubhub?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:CLUBHUB_DB_USER="root"
$env:CLUBHUB_DB_PASS="password"

If MySQL is not configured, app uses local H2 fallback DB.

--------------------------------------------------

## 4) Run locally on friend PC

From project root:

.\start-local-tomcat.ps1 -Build

Open in browser:

http://localhost:8080/ClubHub/

Stop server:

.\stop-local-tomcat.ps1

If app does not load immediately, wait a few seconds and refresh once.

Eclipse note:

- You do not need to create a separate Run Configuration for Tomcat if you are using the provided scripts.
- Just run the script from terminal after each change.

--------------------------------------------------

## 5) Where friend should edit code

Frontend files:

- src/main/webapp
- src/main/webapp/js
- src/main/webapp/css

Backend files:

- src/main/java/com/silicon/servlet
- src/main/java/com/silicon/dao
- src/main/java/com/silicon/model

After any backend or frontend change:

.\start-local-tomcat.ps1 -Build

Then hard refresh browser (Ctrl + F5).

Beginner rule:

- If you changed Java files, always rebuild with -Build.
- If you changed only CSS/HTML/JS, still run -Build if you are not sure.

--------------------------------------------------

## 6) Important fact about domain updates

Local changes NEVER appear on domain automatically.

To reflect changes on domain, your friend must:

1. Commit and push changes to shared Git repository
2. Deploy latest build to server

You need a deployment process for this.

--------------------------------------------------

## 7) Recommended team workflow (so everyone stays connected)

Use one shared GitHub repository as source of truth.

Daily flow:

1. Pull latest code
2. Make changes
3. Test locally
4. Commit and push
5. Deploy to domain server

Useful commands:

git pull origin main
git add .
git commit -m "Describe change"
git push origin main

--------------------------------------------------

## 8) Host on your own domain (production)

Example architecture:

- Domain DNS -> VPS public IP
- Nginx (80/443) -> Tomcat (8080)
- Tomcat runs ClubHub.war
- MySQL for production data

### 8.1 Server setup (Ubuntu VPS)

Install packages:

sudo apt update
sudo apt install -y openjdk-17-jdk nginx mysql-server git maven

Install Tomcat 10 or use managed service.

### 8.2 Deploy app to Tomcat

On server:

git clone <your-repo-url>
cd <repo-folder>
./mvnw clean package

Copy generated WAR to Tomcat webapps as ClubHub.war.

Set DB environment variables for Tomcat service:

- CLUBHUB_DB_URL
- CLUBHUB_DB_USER
- CLUBHUB_DB_PASS

Restart Tomcat.

### 8.3 Connect domain

At your domain provider DNS settings:

- Add A record: @ -> your server IP
- Add A record: www -> your server IP

### 8.4 Nginx reverse proxy

Configure Nginx to forward domain traffic to Tomcat on 8080.
Enable HTTPS with Certbot.

--------------------------------------------------

## 9) Make changes reflect on domain quickly (automation)

Best option: CI/CD auto deploy from GitHub.

Process:

1. Push code to main branch
2. GitHub Actions builds WAR
3. Action copies WAR to server (SSH/SCP)
4. Action restarts Tomcat

Result: Every successful push updates domain automatically.

If you do not use CI/CD, deploy manually after every push.

--------------------------------------------------

## 10) Simple manual deploy command sequence (server)

cd <repo-folder>
git pull origin main
./mvnw clean package -DskipTests
cp target/ClubHub.war <tomcat-webapps>/ClubHub.war
sudo systemctl restart tomcat

Then verify:

https://yourdomain.com/ClubHub/

--------------------------------------------------

## 11) Common issues and fixes

1. Failed to fetch in browser
- Cause: backend not running or wrong URL/path
- Fix: start Tomcat, verify API URL responds

2. App opens but no data
- Cause: DB connection issue
- Fix: verify CLUBHUB_DB_* variables and MySQL status

3. Port conflict on 8080
- Cause: another process using same port
- Fix: stop conflicting service or change Tomcat port

4. Changes not visible
- Cause: browser cache or old deployment
- Fix: rebuild WAR, redeploy, hard refresh

--------------------------------------------------

## 12) Production safety checklist

- Use strong DB password
- Do not commit secrets in code
- Use HTTPS only in production
- Keep daily DB backup
- Keep Java, Tomcat, MySQL updated

--------------------------------------------------

## 13) Suggested ownership model for your team

- One main branch for stable production
- Feature branches for each member
- Pull Request review before merge
- Auto deploy only from main branch

This prevents accidental breakage on domain.
