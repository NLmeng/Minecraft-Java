#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

mvn clean package

mvn spotless:apply

java -XstartOnFirstThread -jar target/minecraft-1.0-SNAPSHOT.jar