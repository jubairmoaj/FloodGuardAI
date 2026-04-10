# FloodGuardAI

![Platform](https://img.shields.io/badge/platform-Android%20%2B%20PHP-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-orange)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Backend](https://img.shields.io/badge/backend-Plain%20PHP%208.2%2B-777BB4)
![Database](https://img.shields.io/badge/database-MySQL%208-4479A1)
![AI](https://img.shields.io/badge/AI-Gemini%202.0%20Flash-34A853)

FloodGuardAI is a full-stack flood intelligence platform with an Android app and a cPanel-friendly PHP backend. It combines weather signals, map risk layers, AI-assisted analysis, and community reports to support safer movement and faster flood response decisions.

## Problem Statement
Urban and peri-urban flood response often suffers from delayed risk visibility, fragmented data sources, and weak last-mile communication. Citizens and field teams need one place to understand risk, plan safer routes, submit flood evidence, and receive alerts before conditions worsen.

## Your Solution
FloodGuardAI delivers a unified flood operations workflow:
- Predicts location-level flood risk
- Visualizes flood/safe zones and live report pins on map layers
- Assists users with AI chat for situational guidance
- Analyzes safer route options for a requested time
- Accepts image-based flood reports for rapid field intelligence
- Triggers threshold-based alerts with history tracking

## Key Features
- Android app with 7 production screens: Dashboard, Map, AI Chat, Route Planner, Report Upload, Alerts, Settings/Profile
- Plain PHP REST API (no heavy framework), deployable on standard cPanel hosting
- JWT-style auth flow: register, login, refresh, profile
- Cron-based alert evaluation (`backend/scripts/cron_alerts.php`)
- Gemini-assisted prediction/chat/report analysis with deterministic fallbacks in app repository logic
- Bilingual app support (`en`, `bn`)

## Target Users
Primary target: Government (B2G)
- Disaster management authorities
- Municipal corporations and city operations centers
- Local government emergency response units
- Public infrastructure and transport coordination teams

## Current Project Structure
- `app/` Android application (Kotlin + Jetpack Compose)
- `backend/` PHP API, SQL schema, cron script, tests
- `backend/sql/schema.sql` MySQL schema

## Setup Instructions (Current)

### 1) Prerequisites
- Android Studio (latest stable)
- JDK 17
- PHP 8.2+
- MySQL 8
- Web server pointing to `backend/public`

### 2) Backend Setup
1. Copy env file:
   - `cp backend/.env.example backend/.env`
2. Update values in `backend/.env`:
   - `APP_SECRET`, DB credentials, and API keys
   - `OPENWEATHER_API_KEY`, `GOOGLE_MAPS_API_KEY`, `GEMINI_API_KEY`, `FCM_SERVER_KEY`
3. Import schema:
   - `backend/sql/schema.sql`
4. Ensure upload directory is writable:
   - `backend/storage/uploads`
5. Point document root to:
   - `backend/public`
6. Add cron every 15 minutes:
   - `*/15 * * * * /usr/local/bin/php /path/to/backend/scripts/cron_alerts.php >> /path/to/floodguard_cron.log 2>&1`

### 3) Android Setup
1. Open the project in Android Studio.
2. Review and update runtime config in `app/build.gradle.kts`:
   - `API_BASE_URL`
   - `manifestPlaceholders["googleMapsApiKey"]`
   - `GOOGLE_MAPS_API_KEY`, `OPENWEATHER_API_KEY`, `GEMINI_API_KEY`
3. Sync Gradle and run on emulator/device.

Note: The current `app/build.gradle.kts` already contains concrete API keys and a hosted `API_BASE_URL`. For secure deployment, replace these with environment-specific secrets before release.

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
- `POST /api/v1/device-tokens`
- `POST /api/v1/chat/ask`

## Quick Test Commands
- Backend tests: `php backend/tests/run.php`
- Android unit tests: `./gradlew test`
