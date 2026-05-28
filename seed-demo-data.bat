@echo off
set CP=target\classes
for /f "delims=" %%a in (cp.txt) do set CP=%CP%;%%a
java -cp "%CP%" com.auction.server.tools.DatabaseResetUtil
