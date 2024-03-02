#!/usr/bin/env sh
set -eu

APPNAME=dfa

echo "Installing driver for $APPNAME"

mvn package "$@"

BINDIR=/usr/local/bin
JARSDIR=${BINDIR}/jpsember_jars
JARNAME=${APPNAME}-1.0-jar-with-dependencies.jar
OUTFILE=${BINDIR}/${APPNAME}

mkdir -p ${JARSDIR}
cp -rf target/${JARNAME} ${JARSDIR}
cp -rf driver.sh.txt ${OUTFILE}
chmod 755 ${OUTFILE}
