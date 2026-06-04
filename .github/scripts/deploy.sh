#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
APP_DIR="/home/ubuntu/app/Cosmic"

rm -rf "$APP_DIR"
mkdir -p "$APP_DIR/target"

cp -rpf "$REPO_ROOT/target/Cosmic.jar" "$APP_DIR/target/"
cp -rpf "$REPO_ROOT/.mvn" "$APP_DIR/.mvn"
cp -rpf "$REPO_ROOT/mvnw" "$APP_DIR/mvnw"
cp -rpf "$REPO_ROOT/handbook" "$APP_DIR/handbook"
cp -rpf "$REPO_ROOT/pom.xml" "$APP_DIR/pom.xml"
cp -rpf "$REPO_ROOT/scripts" "$APP_DIR/scripts"
cp -rpf "$REPO_ROOT/src" "$APP_DIR/src"
cp -rpf "$REPO_ROOT/tools" "$APP_DIR/tools"
cp -rpf "$REPO_ROOT/wz" "$APP_DIR/wz"
cp -rpf "$REPO_ROOT/config.yaml" "$APP_DIR/config.yaml"
