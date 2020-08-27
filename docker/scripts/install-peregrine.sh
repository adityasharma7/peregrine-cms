#!/bin/bash

usage () {
  echo "Usage: `basename $0` <peregrine repo> <peregrine branch>"
}

if [ $# -ne 2 ]; then
  usage
  exit 1
fi

REPO=$1
BRANCH=$2
DIR=peregrine-cms

echo "Fetching peregrine from repo: ${REPO}"
echo "Building peregrine using branch: ${BRANCH}"

# Build Peregrine
git clone ${REPO}
cd ${DIR}
git checkout ${BRANCH}
mvn clean install -PautoInstallPackage
