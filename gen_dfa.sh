#!/usr/bin/env bash
set -eu

# Compile rxp files to dfa
#
DEST="src/main/resources/dfa/"
TOK="src/main/java/dfa/"

# This is the dfa that is used to parse regular expressions (the new one, as opposed
# to the bespoke parser that doesn't use tokens)
#
dfa input dfas/rexp_parser.rxp \
    output "${DEST}rexp_parser.dfa" \
    ids "${TOK}TokenRegParse.java" \
    example_text dfas/sample.txt
