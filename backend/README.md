# FloodGuardAI Backend (Plain PHP + MySQL)

## Stack
- PHP 8.2+
- MySQL 8
- cPanel-compatible public entrypoint (`public/index.php`)
- Cron job for alert checks (`scripts/cron_alerts.php`)

## Setup
1. Copy `.env.example` to `.env` and fill API keys/secrets.
2. Import `sql/schema.sql` into MySQL.
3. Point cPanel document root to `backend/public`.
4. Ensure upload directory is writable (default resolves under `public/../storage/uploads`).

## Cron (every 15 minutes)
```bash
*/15 * * * * /usr/local/bin/php /home/your-user/public_html/backend/scripts/cron_alerts.php >> /home/your-user/logs/floodguard_cron.log 2>&1
```

## Implemented API Endpoints
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/me`
- `POST /api/v1/predictions/location`
- `POST /api/v1/routes/analyze`
- `POST /api/v1/reports`
- `GET /api/v1/reports/{id}/image`
- `POST /api/v1/reports/{id}/flag`
- `GET /api/v1/map/layers`
- `POST /api/v1/alerts`
- `GET /api/v1/alerts`
- `DELETE /api/v1/alerts/{id}`
- `GET /api/v1/alerts/history`
- `POST /api/v1/chat/ask`
- `POST /api/v1/device-tokens`
