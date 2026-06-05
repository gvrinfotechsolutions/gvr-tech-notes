#!/bin/bash
# =============================================================================
# GVR Tech Notes — EC2 One-Time Setup Script
# =============================================================================
# USAGE (run this ONCE on a fresh EC2 instance):
#   chmod +x setup-ec2.sh
#   ./setup-ec2.sh
#
# WHAT THIS SCRIPT DOES:
#   1.  Updates OS packages
#   2.  Installs Java 17
#   3.  Installs MySQL 8
#   4.  Creates MySQL database + user
#   5.  Creates a dedicated 'appuser' Linux user (app never runs as root)
#   6.  Creates /opt/gvr-tech-notes directory
#   7.  Installs Nginx as reverse proxy (port 80 → 8091)
#   8.  Installs systemd service for auto-start on reboot
#   9.  Opens firewall rules (via iptables)
#   10. Prints all env vars you need to set before starting
#
# TESTED ON: Amazon Linux 2023 (al2023) — the default for new EC2 instances
# =============================================================================

set -e  # Exit immediately on any error

# ── COLOURS ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${CYAN}[$(date '+%H:%M:%S')] $1${NC}"; }
ok()   { echo -e "${GREEN}[$(date '+%H:%M:%S')] ✅ $1${NC}"; }
err()  { echo -e "${RED}[$(date '+%H:%M:%S')] ❌ $1${NC}"; exit 1; }

# ── CONFIG — change these if needed ──────────────────────────────────────────
APP_DIR="/opt/gvr-tech-notes"
APP_USER="appuser"
SERVICE="gvr-tech-notes"
JAR_NAME="SBRW_Demo-0.0.1-SNAPSHOT.jar"
APP_PORT="8091"
DB_NAME="gvr_tech_notes"
DB_USER="gvrapp"
# DB_PASSWORD will be prompted below — never hardcode in scripts
# ─────────────────────────────────────────────────────────────────────────────

log "================================================"
log "  GVR Tech Notes — EC2 Setup Starting"
log "================================================"

# ── STEP 1: Update OS ─────────────────────────────────────────────────────────
log "STEP 1/10 — Updating OS packages..."
sudo dnf update -y -q
ok "OS updated"

# ── STEP 2: Install Java 17 ───────────────────────────────────────────────────
log "STEP 2/10 — Installing Java 17..."
sudo dnf install -y java-17-amazon-corretto-headless
java -version
ok "Java 17 installed"

# ── STEP 3: MySQL — Using AWS RDS (skip local install) ───────────────────────
log "STEP 3/10 — Skipping local MySQL install (using AWS RDS)..."
ok "Using RDS: gvr-notes-db.c9o6cseei145.ap-south-2.rds.amazonaws.com"

# ── STEP 4: Verify RDS connectivity ──────────────────────────────────────────
log "STEP 4/10 — Verifying RDS connectivity..."
sudo dnf install -y mariadb105 -q   # install mysql client only (no server)
if mysql -h gvr-notes-db.c9o6cseei145.ap-south-2.rds.amazonaws.com \
         -u admin -p"${RDS_PASSWORD_CHECK:-CHANGE_ME}" \
         -e "SELECT 'RDS connection OK';" gvr_notes 2>/dev/null; then
    ok "RDS connection verified"
else
    log "⚠️  RDS connection check failed — continuing anyway. Verify Security Group allows EC2 → RDS on port 3306."
fi

# ── STEP 5: Create app user ───────────────────────────────────────────────────
log "STEP 5/10 — Creating dedicated Linux app user ($APP_USER)..."
if ! id "$APP_USER" &>/dev/null; then
    sudo useradd -r -m -s /sbin/nologin $APP_USER
    ok "User '$APP_USER' created (no login shell — security best practice)"
else
    ok "User '$APP_USER' already exists"
fi

# ── STEP 6: Create app directory ──────────────────────────────────────────────
log "STEP 6/10 — Creating app directory at $APP_DIR..."
sudo mkdir -p $APP_DIR/logs
sudo chown -R $APP_USER:$APP_USER $APP_DIR
sudo chmod 750 $APP_DIR
ok "App directory created"

# ── STEP 7: Install Nginx ─────────────────────────────────────────────────────
log "STEP 7/10 — Installing Nginx (port 80 → $APP_PORT reverse proxy)..."
sudo dnf install -y nginx

sudo tee /etc/nginx/conf.d/gvr-tech-notes.conf > /dev/null <<EOF
server {
    listen 80;
    server_name _;

    # Max upload size (for file uploads, profile images etc.)
    client_max_body_size 20M;

    # Proxy all requests to Spring Boot
    location / {
        proxy_pass         http://localhost:${APP_PORT};
        proxy_http_version 1.1;
        proxy_set_header   Host              \$host;
        proxy_set_header   X-Real-IP         \$remote_addr;
        proxy_set_header   X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto \$scheme;

        # Increase timeouts for AI generation (can take 60-120s)
        proxy_read_timeout    180s;
        proxy_connect_timeout 30s;
        proxy_send_timeout    180s;
    }

    # Health check endpoint
    location /actuator/health {
        proxy_pass http://localhost:${APP_PORT}/actuator/health;
        access_log off;
    }
}
EOF

sudo systemctl start nginx
sudo systemctl enable nginx
ok "Nginx installed and configured"

# ── STEP 8: Create systemd service ────────────────────────────────────────────
log "STEP 8/10 — Creating systemd service ($SERVICE)..."

sudo tee /etc/systemd/system/${SERVICE}.service > /dev/null <<EOF
[Unit]
Description=GVR Tech Notes — Spring Boot Application
After=network.target
# Using AWS RDS — no local MySQL dependency

[Service]
Type=simple
User=${APP_USER}
Group=${APP_USER}
WorkingDirectory=${APP_DIR}

# ── Environment variables (sensitive values — set via EnvironmentFile) ──
EnvironmentFile=${APP_DIR}/app.env

# ── Start command ──
ExecStart=/usr/bin/java \\
    -Xms256m -Xmx512m \\
    -Dspring.profiles.active=prod \\
    -Dspring.config.additional-location=file:${APP_DIR}/ \\
    -jar ${APP_DIR}/${JAR_NAME}

# ── Restart policy ──
Restart=on-failure
RestartSec=10
StartLimitIntervalSec=60
StartLimitBurst=3

# ── Logging ──
StandardOutput=journal
StandardError=journal
SyslogIdentifier=${SERVICE}

# ── Security hardening ──
NoNewPrivileges=yes
PrivateTmp=yes

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
ok "Systemd service created"

# ── STEP 9: Create app.env template ───────────────────────────────────────────
log "STEP 9/10 — Creating environment variables file at ${APP_DIR}/app.env..."

sudo tee ${APP_DIR}/app.env > /dev/null <<EOF
# ══════════════════════════════════════════════════════
# GVR Tech Notes — Environment Variables
# Edit this file to set your secrets before starting
# ══════════════════════════════════════════════════════

# Database (AWS RDS)
SPRING_DATASOURCE_URL=jdbc:mysql://gvr-notes-db.c9o6cseei145.ap-south-2.rds.amazonaws.com:3306/gvr_notes?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=CHANGE_ME

# Groq AI
GROQ_API_KEY=CHANGE_ME

# Admin credentials
ADMIN_USERNAME=venky
ADMIN_PASSWORD=CHANGE_ME

# App
SERVER_PORT=8091
EOF

sudo chown ${APP_USER}:${APP_USER} ${APP_DIR}/app.env
sudo chmod 600 ${APP_DIR}/app.env   # Only appuser can read — secrets protected
ok "app.env created at ${APP_DIR}/app.env"

# ── STEP 10: Summary ──────────────────────────────────────────────────────────
log "STEP 10/10 — Setup complete! Next steps:"

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  ✅ EC2 Setup Complete!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
echo ""
echo "  BEFORE running deploy.ps1 from your laptop:"
echo ""
echo "  1. Edit secrets in the env file:"
echo "     sudo nano ${APP_DIR}/app.env"
echo "     → Set GROQ_API_KEY"
echo "     → Set ADMIN_PASSWORD (change default!)"
echo "     (DB_PASSWORD is already set from STEP 4)"
echo ""
echo "  2. Open these ports in your EC2 Security Group (AWS Console):"
echo "     Port 22  (SSH)   — your IP only"
echo "     Port 80  (HTTP)  — 0.0.0.0/0"
echo "     Port 443 (HTTPS) — 0.0.0.0/0 (if you add SSL later)"
echo ""
echo "  3. From your Windows laptop, run:"
echo "     .\\deploy.ps1"
echo ""
echo "  4. After first deploy, verify:"
echo "     curl http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)/actuator/health"
echo ""
echo "  Useful commands:"
echo "     sudo journalctl -u ${SERVICE} -f          # live logs"
echo "     sudo systemctl status ${SERVICE}          # service status"
echo "     sudo systemctl restart ${SERVICE}         # restart app"
echo "     sudo tail -f /var/log/nginx/error.log     # nginx errors"
echo ""
