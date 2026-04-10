# FloodGuardAI

FloodGuardAI is a full-stack flood intelligence system with:
- Android app (Kotlin + Jetpack Compose)
- Plain PHP REST backend (cPanel-friendly)
- MySQL 8 schema and cron-based alerting
- Gemini-powered prediction/chat/vision workflows with deterministic fallbacks

## Project Structure
- `app/` Android application with 7 screens:
  - Dashboard
  - Map
  - AI Chat
  - Route Planner
  - Report Upload
  - Alerts
  - Settings/Profile
- `backend/` PHP API and SQL schema

## Backend Setup
1. Copy `backend/.env.example` to `backend/.env`.
2. Fill DB/API keys (`OPENWEATHER_API_KEY`, `GOOGLE_MAPS_API_KEY`, `GEMINI_API_KEY`, `FCM_SERVER_KEY`).
3. Import `backend/sql/schema.sql`.
4. Configure document root to `backend/public`.
5. Add cron every 15 min:
   - `php /path/to/backend/scripts/cron_alerts.php`

## Android Setup
1. Update `app/build.gradle.kts` placeholders:
   - `API_BASE_URL`
   - `googleMapsApiKey`
2. Build/run from Android Studio.

## Contracted API Endpoints
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/me`
- `POST /api/v1/predictions/location`
- `POST /api/v1/routes/analyze`
- `POST /api/v1/reports`
- `GET /api/v1/map/layers`
- `POST /api/v1/alerts`
- `GET /api/v1/alerts`
- `DELETE /api/v1/alerts/{id}`
- `GET /api/v1/alerts/history`
- `POST /api/v1/chat/ask`
