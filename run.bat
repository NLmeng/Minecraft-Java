@echo off
mvn clean package && mvn spotless:apply && java -jar target/minecraft-1.0-SNAPSHOT.jar
pause