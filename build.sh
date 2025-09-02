#!/usr/bin/env bash
set -eu

PROGRAM=dfa

# This builds the program and installs it to the local machine,
# essentially what the 'dev install program ...' command would do
dev resettest

# Build a jar file that contains everything
mvn package

# Copy the jar file to the bin directory
cp target/${PROGRAM}-1.0-jar-with-dependencies.jar ~/bin
