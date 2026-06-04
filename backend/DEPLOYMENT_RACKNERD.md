# Gymmate Backend Deployment for RackNerd

This is the simplest production path for the current Gymmate backend:

- FastAPI
- Uvicorn
- SQLite
- Nginx reverse proxy
- DeepSeek API
- GitHub-based deployment

Goal:

- local `git push`
- server `git pull`
- restart service
- reverse proxy stays unchanged

## 1. What is actually required

For your current project, only this environment variable is truly required:

```env
DEEPSEEK_API_KEY=your_real_key
```

These are optional:

- `DEEPSEEK_MODEL`
  Default is already `deepseek-v4-flash`
- `DEEPSEEK_BASE_URL`
  Default is already `https://api.deepseek.com`
- `GYMMATE_DB_PATH`
  Not needed unless you want the database outside the repo path
- `GYMMATE_ALLOWED_ORIGINS`
  Not needed for the Android app
- `DEEPSEEK_TIMEOUT_SECONDS`
  Not needed unless you want to tune timeout behavior

## 2. Why those extra variables are not necessary

### `GYMMATE_DB_PATH`

This means: where the SQLite file is stored.

If you do not set it, the backend already defaults to:

- `backend/gymmate.db`

So for your current MVP, you can ignore it.

### `GYMMATE_ALLOWED_ORIGINS`

This is a CORS setting for browser-based frontend requests.

Your current client is Android native, not a browser page, so CORS is basically not the main issue here.

You can ignore it for now.

### `DEEPSEEK_TIMEOUT_SECONDS`

This is only for fine-tuning network timeout.

The backend already defaults to `20` seconds.

You do not need to configure it unless you specifically want a different timeout.

## 3. Recommended server

For the current MVP:

- minimum: `1 vCPU / 1 GB RAM`
- recommended: `2 vCPU / 2 GB RAM`

Because:

- pose recognition runs on-device
- server only does light API work
- SQLite is enough for current traffic

## 4. Final deployment structure

Recommended server layout:

```text
/opt/gymmate/
  repo/        <- full GitHub repo clone
  backend/     <- deployed backend copy
  shared/.env  <- private env vars
  venv/        <- Python virtualenv
```

Traffic flow:

```text
Android App -> Nginx : 80/443 -> FastAPI/Uvicorn : 127.0.0.1:8000
```

## 5. First-time setup

### Step 1: prepare DNS

Point:

- `api.yourdomain.com`

to:

- your RackNerd server public IP

### Step 2: upload repo once or clone manually

SSH into the server and run:

```bash
mkdir -p /opt/gymmate
cd /opt/gymmate
git clone https://github.com/yourname/yourrepo.git repo
cd repo/backend
chmod +x deploy/bootstrap_racknerd.sh deploy/deploy_from_git.sh deploy/deploy_gymmate.sh deploy/enable_https.sh
DOMAIN=api.yourdomain.com bash deploy/bootstrap_racknerd.sh
```

This will:

- install Python, git, Nginx, UFW
- create user `gymmate`
- create `/opt/gymmate/shared/.env`
- create `/opt/gymmate/venv`
- install Nginx reverse proxy
- install `systemd` service

### Step 3: fill the env file

Edit:

- `/opt/gymmate/shared/.env`

Use the minimum:

```env
DEEPSEEK_API_KEY=your_real_key
```

Optional if you want explicit model pinning:

```env
DEEPSEEK_API_KEY=your_real_key
DEEPSEEK_MODEL=deepseek-v4-flash
```

## 6. Deploy from GitHub

This is now the main deployment flow.

Run on the server:

```bash
REPO_URL=https://github.com/yourname/yourrepo.git \
GIT_BRANCH=main \
APP_ROOT=/opt/gymmate \
bash /opt/gymmate/repo/backend/deploy/deploy_from_git.sh
```

What it does:

1. clone or fetch the GitHub repo
2. reset to the latest remote branch
3. copy `backend/` to the live deploy path
4. install Python dependencies
5. restart `gymmate-api`
6. restart `nginx`
7. check `127.0.0.1:8000/health`

## 7. Your actual daily workflow

From your local machine:

```bash
git add .
git commit -m "update backend"
git push
```

Then on the server:

```bash
cd /opt/gymmate/repo
git pull
REPO_URL=https://github.com/yourname/yourrepo.git GIT_BRANCH=main APP_ROOT=/opt/gymmate bash backend/deploy/deploy_from_git.sh
```

If you want it even simpler later, we can wrap this into one alias like:

```bash
deploy-gymmate
```

That wrapper now exists. The simplest server-side update command is:

```bash
REPO_URL=https://github.com/yourname/yourrepo.git GIT_BRANCH=main APP_ROOT=/opt/gymmate bash /opt/gymmate/repo/backend/deploy/deploy_gymmate.sh
```

## 8. Enable HTTPS

After DNS is effective:

```bash
DOMAIN=api.yourdomain.com EMAIL=you@example.com bash /opt/gymmate/repo/backend/deploy/enable_https.sh
```

## 9. Android production URL

After the server is ready, Android should point to:

```text
https://api.yourdomain.com/
```

Current Android config is now split by build type:

- `debug` -> local LAN backend
- `release` -> production domain

## 10. Service commands

Check status:

```bash
systemctl status gymmate-api
```

Restart backend:

```bash
systemctl restart gymmate-api
```

Check logs:

```bash
journalctl -u gymmate-api -n 200 --no-pager
```

Check local health:

```bash
curl http://127.0.0.1:8000/health
```

Check public health:

```bash
curl https://api.yourdomain.com/health
```

## 11. Pressure judgment

Current pressure is not high.

Main reasons:

- no server-side pose inference
- no heavy file upload
- only light API traffic
- SQLite is enough for current MVP

First likely bottlenecks later:

- DeepSeek latency
- SQLite serialized writes
- not CPU
