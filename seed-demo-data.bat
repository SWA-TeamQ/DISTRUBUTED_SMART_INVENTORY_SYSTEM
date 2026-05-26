@echo off
REM seed-demo-data.bat
REM Convenience script to seed the database and generate test images
REM Usage: seed-demo-data.bat

setlocal enabledelayedexpansion

echo.
echo 🌱 RTDAS Demo Data Seeding
echo ==========================
echo.

REM Check if Maven is available
where mvn >nul 2>nul
if errorlevel 1 (
    echo ❌ Maven not found. Please ensure Maven is in your PATH.
    exit /b 1
)

echo 1️⃣  Running DemoSeeder...
call mvn exec:java -Dexec.mainClass=com.auction.server.core.DemoSeeder
if errorlevel 1 (
    echo ❌ DemoSeeder failed
    exit /b 1
)

echo.
echo 2️⃣  Generating Test Images...
call mvn exec:java -Dexec.mainClass=com.auction.server.core.SeedTestImages
if errorlevel 1 (
    echo ❌ Image generation failed
    exit /b 1
)

echo.
echo ✅ Demo data setup complete!
echo.
echo 📋 Next steps:
echo    1. Start server: mvn exec:java
echo    2. Open client UI
echo    3. Login as bella-247 / pass123
echo.
echo 🔓 Test accounts:
echo    - bella-247 (bidder) / pass123
echo    - seller-alice / pass123
echo    - seller-bob / pass123
echo    - seller-charlie / pass123
echo.
echo 📖 See TESTING_GUIDE.md for detailed test scenarios
echo.
pause
