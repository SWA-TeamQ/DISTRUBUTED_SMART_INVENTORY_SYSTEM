@echo off
echo ========================================================
echo RTDAS Demo Seeder
echo ========================================================
echo.
echo Compiling project...
call mvn compile

echo.
echo Seeding database with users, auctions, and bids...
call mvn exec:java "-Dexec.mainClass=com.auction.server.tools.DemoSeeder"

echo.
echo Generating test placeholder images for the seeded auctions...
call mvn exec:java "-Dexec.mainClass=com.auction.server.tools.SeedTestImages"

echo.
echo Seeding complete! You can now start the server with:
echo mvn exec:java
echo.
pause
