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

if [ -d "$REPO_ROOT/ui/dist" ]; then
  echo "Copying UI dist files..."
  rm -rf "$APP_DIR/ui/dist"
  mkdir -p "$APP_DIR/ui"
  cp -rpf "$REPO_ROOT/ui/dist" "$APP_DIR/ui/dist"
else
  echo "UI dist directory not found. Please build the UI first."
fi

# If a systemd service unit is provided in the repo, install/update it and reload systemd
SERVICE_SRC="$REPO_ROOT/.github/scripts/cosmic.service"
if [ -f "$SERVICE_SRC" ]; then
  TMP_UNIT="/tmp/cosmic.service"

  # Replace WorkingDirectory and ExecStart to use the deployed APP_DIR absolute paths
  sed -e "s|^WorkingDirectory=.*|WorkingDirectory=$APP_DIR|" \
      "$SERVICE_SRC" > "$TMP_UNIT"

  sudo cp "$TMP_UNIT" /etc/systemd/system/cosmic.service
  sudo chown root:root /etc/systemd/system/cosmic.service
  sudo chmod 644 /etc/systemd/system/cosmic.service
  sudo systemctl daemon-reload
  sudo systemctl enable cosmic || true
  rm -f "$TMP_UNIT"
fi
