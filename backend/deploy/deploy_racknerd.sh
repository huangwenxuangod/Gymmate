#!/usr/bin/env bash
set -euo pipefail

APP_USER="${APP_USER:-gymmate}"
APP_GROUP="${APP_GROUP:-$APP_USER}"
APP_ROOT="${APP_ROOT:-/opt/gymmate}"
RELEASE_ARCHIVE="${1:-/tmp/gymmate-backend.tar.gz}"
TMP_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

if [[ ! -f "${RELEASE_ARCHIVE}" ]]; then
  echo "Release archive not found: ${RELEASE_ARCHIVE}"
  exit 1
fi

tar -xzf "${RELEASE_ARCHIVE}" -C "${TMP_DIR}"

mkdir -p "${APP_ROOT}"
rm -rf "${APP_ROOT}/backend"
mv "${TMP_DIR}/backend" "${APP_ROOT}/backend"
chown -R "${APP_USER}:${APP_GROUP}" "${APP_ROOT}/backend"

"${APP_ROOT}/venv/bin/pip" install --upgrade pip
"${APP_ROOT}/venv/bin/pip" install -r "${APP_ROOT}/backend/requirements.txt"

systemctl daemon-reload
systemctl enable gymmate-api
systemctl restart gymmate-api
systemctl restart nginx

sleep 2
curl --fail http://127.0.0.1:8000/health
