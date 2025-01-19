#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Compile
mvn clean compile

# auto format
mvn spotless:apply

# Run
mvn exec:java
