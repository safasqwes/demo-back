@echo off
REM Startup script for NovelHub (Windows)

echo Starting NovelHub Application...

REM Build the project
echo Building project...
call mvn clean package -DskipTests

REM Run the application
echo Starting application...
java -jar target\stock.jar

pause

