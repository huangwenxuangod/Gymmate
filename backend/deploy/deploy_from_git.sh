#!/usr/bin/env bash
set -euo pipefail

APP_USER="${APP_USER:-gymmate}"
APP_GROUP="${APP_GROUP:-$APP_USER}"
APP_ROOT="${APP_ROOT:-/opt/gymmate}"
REPO_URL="${REPO_URL:-}"
GIT_BRANCH="${GIT_BRANCH:-main}"

if [[ -z "${REPO_URL}" ]]; then
  echo "REPO_URL is required"
  exit 1
fi

if [[ ! -d "${APP_ROOT}/repo/.git" ]]; then
  rm -rf "${APP_ROOT}/repo"
  git clone --depth=1 --branch "${GIT_BRANCH}" "${REPO_URL}" "${APP_ROOT}/repo"
else
  git -C "${APP_ROOT}/repo" fetch origin "${GIT_BRANCH}"
  git -C "${APP_ROOT}/repo" checkout "${GIT_BRANCH}"
  git -C "${APP_ROOT}/repo" reset --hard "origin/${GIT_BRANCH}"
fi

rm -rf "${APP_ROOT}/backend"
cp -R "${APP_ROOT}/repo/backend" "${APP_ROOT}/backend"
chown -R "${APP_USER}:${APP_GROUP}" "${APP_ROOT}/backend" "${APP_ROOT}/repo"

"${APP_ROOT}/venv/bin/pip" install --upgrade pip
"${APP_ROOT}/venv/bin/pip" install -r "${APP_ROOT}/backend/requirements.txt"

install -m 644 "${APP_ROOT}/backend/deploy/gymmate-api.service" /etc/systemd/system/gymmate-api.service
systemctl daemon-reload
systemctl enable gymmate-api
systemctl restart gymmate-api
systemctl restart nginx

sleep 2
curl --fail http://127.0.0.1:8000/health
