#!/bin/bash
./mvnw clean test
echo "Opening coverage report..."
open target/site/jacoco/index.html
