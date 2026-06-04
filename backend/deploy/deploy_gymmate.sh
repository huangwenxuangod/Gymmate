#!/usr/bin/env bash
set -euo pipefail

APP_ROOT="${APP_ROOT:-/opt/gymmate}"
REPO_URL="${REPO_URL:-}"
GIT_BRANCH="${GIT_BRANCH:-main}"

if [[ -z "${REPO_URL}" ]]; then
  echo "REPO_URL is required"
  exit 1
fi

bash "${APP_ROOT}/repo/backend/deploy/deploy_from_git.sh"

echo ""
echo "Gymmate deploy complete."
echo "Local health:"
curl --fail http://127.0.0.1:8000/health
