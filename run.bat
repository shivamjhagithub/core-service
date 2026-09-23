@echo off
REM Clone-and-run helper. Requires: JDK 21+, MySQL on localhost:3306, Redis on localhost:6379.
REM Default DB user/password: root/root  — edit .env or set DATABASE_PASSWORD if different.

cd /d "%~dp0"

if not exist "storage" mkdir storage

echo Starting College ERP core-service...
echo API:      http://localhost:8082
echo Swagger:  http://localhost:8082/swagger-ui.html
echo Login:    MainAdmin001 / Admin@123
echo.

call mvnw.cmd spring-boot:run
