# EnergyPulse

EnergyPulse is the implementation of the **VoltWise Project**: a real-time IoT
energy monitoring, billing and anomaly detection platform.

## Architecture

- **Spring Boot Core:** Registration, history, telemetry, billing and notification modules
- **PostgreSQL:** Permanent home, billing, event and consumption records
- **Apache Kafka:** Registration and telemetry event streaming
- **Apache Ignite:** Live metrics, tariff state and anomaly counters
- **React Web:** Dashboard, charts and notifications
- **Sensor Simulator:** Scheduled appliance telemetry generation

## Project Structure

```text
EnergyPulse/
├─ database/init/001_schema.sql
├─ energypulse-core/
├─ energypulse-web/
├─ sensor-simulator/
├─ .env.example
├─ docker-compose.yml
└─ README.md
```

## Requirements

- Java 17
- Docker Desktop
- Git
- PowerShell
- Node.js and npm for the web application

## Local Setup

Clone the project:

```powershell
git clone https://github.com/CoinMaster-team/i2i-Academy-EnergyPulse-12.git
cd i2i-Academy-EnergyPulse-12
```

Create the local environment file:

```powershell
Copy-Item .env.example .env
```

Set a secure local `POSTGRES_PASSWORD` inside `.env`. Never commit this file.

Start infrastructure:

```powershell
docker compose config --quiet
docker compose up -d
docker compose ps -a
```

Expected services:

- `energypulse-postgres`: healthy
- `energypulse-kafka`: healthy
- `energypulse-ignite`: running
- `energypulse-kafka-init`: exited with code `0`

Load environment variables:

```powershell
Get-Content .\.env |
    Where-Object { $_ -and -not $_.StartsWith('#') } |
    ForEach-Object {
        $name, $value = $_ -split '=', 2
        Set-Item -Path "Env:$($name.Trim())" -Value $value.Trim()
    }
```

## Running the Services

Run the backend core:

```powershell
cd .\energypulse-core
.\mvnw.cmd spring-boot:run
```

Run the web application in a second terminal:

```powershell
cd .\energypulse-web
npm install
npm run dev
```

The web application uses `http://localhost:8080` by default. Set
`VITE_API_BASE_URL` before starting Vite when the backend runs at another
address.

Run the sensor simulator in another terminal:

```powershell
cd .\sensor-simulator
.\mvnw.cmd spring-boot:run
```

The simulator listens for registered homes and publishes appliance telemetry.

### Optional Demo Data

Set `DEMO_DATA_ENABLED=true` in the local `.env` file to create `Home A` and
`Home B` when the database is empty. Each demo home includes three appliances
and seven days of consumption history. Existing homes are never replaced.

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

### Register a Home

```http
POST /api/homes
```

Example:

```json
{
  "name": "Demo Smart Home",
  "contactEmail": "demo@energypulse.com",
  "energyQuotaKwh": 450,
  "budgetLimit": 1500,
  "baseTariff": 2.5,
  "penaltyTariff": 4.25,
  "appliances": [
    {
      "name": "Refrigerator",
      "safeLimitWatt": 500,
      "simulationMinWatt": 100,
      "simulationMaxWatt": 650
    }
  ]
}
```

### Get Daily Consumption History

```http
GET /api/homes/{homeId}/consumption-history
```

Optional date range:

```http
GET /api/homes/{homeId}/consumption-history?from=2026-07-19&to=2026-07-21
```

### Add an Appliance

```http
POST /api/homes/{homeId}/appliances
```

The endpoint persists the appliance and republishes the updated home topology.

### List Notifications

```http
GET /api/notifications
GET /api/notifications/{notificationId}
```

The notification console is available at `http://localhost:5173/notifications`.

## AI and Email Notifications

Energy events are converted into Turkish Gemini recommendations and delivered
to the home's contact email. If Gemini is unavailable, EnergyPulse stores and
sends a safe Turkish fallback recommendation.

Set these values in the local `.env` file:

```dotenv
GEMINI_API_KEY=your-local-api-key
GEMINI_MODEL=gemini-3.5-flash
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=your-smtp-username
MAIL_PASSWORD=your-smtp-password
MAIL_FROM=energypulse@example.com
NOTIFICATION_PROCESSING_ENABLED=true
```

Keep `NOTIFICATION_PROCESSING_ENABLED=false` until the credentials are ready.
Never commit real Gemini or SMTP credentials.

### Get Live Telemetry

```http
GET /api/telemetry/live
```

## Kafka Topics

| Topic | Purpose |
|---|---|
| `energypulse.home-registration.v1` | Home and appliance registration |
| `energypulse.telemetry.v1` | Appliance telemetry data |

## Database

PostgreSQL contains:

- `homes`
- `appliances`
- `billing_ledger`
- `consumption_snapshots`
- `operational_events`
- `ai_notifications`

List tables:

```powershell
docker exec energypulse-postgres `
    psql -U energypulse -d energypulse -c "\dt"
```

## Tests

```powershell
cd .\energypulse-core
.\mvnw.cmd clean test
```

```powershell
cd .\sensor-simulator
.\mvnw.cmd clean test
```

## Branch Strategy

| Branch | Responsibility |
|---|---|
| `main` | Integrated final version |
| `feature/platform-data` | Infrastructure, database and backend foundation |
| `feature/streaming-engine` | Kafka, sensors, Ignite and live data |
| `feature/web-notifications` | Frontend and notifications |

All feature work must reach `main` through pull requests without squashing the
feature commit history.

## Integration Notes

Streaming team:

- `HomeRegistrationPublisher` is backed by Kafka.
- Telemetry is consumed and cached in Ignite.
- Live usage and budget metrics are exposed through the telemetry API.

Web team:

- Dashboard, history charts, home registration and notification APIs are connected.
- Dashboard polling uses the streaming live-status endpoint.
- Backend validation errors are displayed safely.
- Verify the complete anomaly → Gemini → email → notification flow during final integration.

## Security

- Never commit `.env`
- Never hardcode passwords or API keys
- Keep PostgreSQL as the permanent source of truth
- Do not return raw stack traces to clients
