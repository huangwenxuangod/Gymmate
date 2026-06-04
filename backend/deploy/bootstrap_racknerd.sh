#!/usr/bin/env bash
set -euo pipefail

APP_USER="${APP_USER:-gymmate}"
APP_GROUP="${APP_GROUP:-$APP_USER}"
APP_ROOT="${APP_ROOT:-/opt/gymmate}"
DOMAIN="${DOMAIN:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -z "${DOMAIN}" ]]; then
  echo "DOMAIN is required"
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive

apt-get update
apt-get install -y python3 python3-venv python3-pip nginx unzip curl ufw git

if ! id "${APP_USER}" >/dev/null 2>&1; then
  useradd --system --create-home --shell /bin/bash "${APP_USER}"
fi

mkdir -p "${APP_ROOT}/backend" "${APP_ROOT}/shared" "${APP_ROOT}/data"
chown -R "${APP_USER}:${APP_GROUP}" "${APP_ROOT}"

if [[ ! -f "${APP_ROOT}/shared/.env" ]]; then
  cat > "${APP_ROOT}/shared/.env" <<EOF
DEEPSEEK_API_KEY=
EOF
  chown "${APP_USER}:${APP_GROUP}" "${APP_ROOT}/shared/.env"
  chmod 600 "${APP_ROOT}/shared/.env"
fi

python3 -m venv "${APP_ROOT}/venv"

install -m 644 "${SCRIPT_DIR}/gymmate-api.service" /etc/systemd/system/gymmate-api.service
sed "s|__DOMAIN__|${DOMAIN}|g" "${SCRIPT_DIR}/nginx.gymmate.conf" > /etc/nginx/sites-available/gymmate.conf
ln -sf /etc/nginx/sites-available/gymmate.conf /etc/nginx/sites-enabled/gymmate.conf
rm -f /etc/nginx/sites-enabled/default

nginx -t
systemctl daemon-reload
systemctl enable nginx
systemctl restart nginx

ufw allow 22/tcp || true
ufw allow 80/tcp || true
ufw allow 443/tcp || true
ufw --force enable || true

echo "Bootstrap complete. Next:"
echo "1. Edit ${APP_ROOT}/shared/.env"
echo "2. Run deploy_gymmate.sh"
echo "3. Optionally enable HTTPS with certbot"
