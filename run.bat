@echo off
mvn clean compile && mvn spotless:apply && mvn exec:java
pause
