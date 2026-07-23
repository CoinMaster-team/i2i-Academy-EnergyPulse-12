# EnergyPulse

EnergyPulse is the implementation of the VoltWise Project: a real-time IoT energy monitoring, billing and anomaly detection platform.

This branch (feature/streaming-engine) specifically contains the high-frequency telemetry processing, Kafka event streaming, In-Memory Data Grid (Apache Ignite) integration, rule engine algorithms, and the standalone sensor simulator.

## Architecture

- **Spring Boot Core:** Registration, history, telemetry, billing and notification modules

- **PostgreSQL:** Permanent home, billing, event and consumption records

- **Apache Kafka:** Registration and telemetry event streaming

- **Apache Ignite:** Live metrics, tariff state and anomaly counters

- **React Web:** Dashboard, charts and notifications

- **Sensor Simulator (Standalone):** Autonomous scheduled service simulating hardware appliance metrics

## Project Structure

```text
EnergyPulse/
├─ database/init/001_schema.sql
├─ energypulse-core/
├─ energypulse-web/
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
git clone [https://github.com/CoinMaster-team/i2i-Academy-EnergyPulse-12.git](https://github.com/CoinMaster-team/i2i-Academy-EnergyPulse-12.git)
cd i2i-Academy-EnergyPulse-12
```

Create the local environment file:

```powershell
Copy-Item .env.example .env
```

Set a secure local POSTGRES_PASSWORD inside .env. Never commit this file.

Start infrastructure (Postgres, Kafka, Ignite):

```powershell
docker compose config --quiet
docker compose up -d
docker compose ps -a
```

Expected services:

- `energypulse-postgres`: healthy

- `energypulse-kafka`: healthy

- `energypulse-ignite`: running

- `energypulse-kafka-init`: exited with code 0

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

1. Run the Backend Core:

```powershell
cd .\energypulse-core
.\mvnw.cmd spring-boot:run
```

2. Run the Sensor Simulator (In a new terminal):
This module listens to new home registrations and autonomously generates high-frequency telemetry data.

```powershell
cd .\sensor-simulator
.\mvnw.cmd spring-boot:run
```

## API Documentation
Swagger UI: `http://localhost:8080/swagger-ui.html`

OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Health: `http://localhost:8080/actuator/health`

## Live Telemetry Streaming (Ignite-backed)
Provides zero-latency, RAM-based real-time data for the frontend dashboard without hitting the PostgreSQL disk.

```http
GET /api/telemetry/live
```

## Register a Home
Triggers a Kafka event (energypulse.home-registration.v1) which the sensor simulator catches to start broadcasting.

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

## Branch Strategy

| Branch | Responsibility |
|---|---|
| `main` | Integrated final version |
| `feature/platform-data` | Infrastructure, database and backend foundation |
| `feature/streaming-engine` | Kafka, sensors, Ignite and live data |
| `feature/web-notifications` | Frontend and notifications |

All feature work must reach `main` through pull requests.

## Integration Notes

Streaming team:

- Implement `HomeRegistrationPublisher`
- Replace `LoggingHomeRegistrationPublisher`
- Consume telemetry and update Ignite
- Provide the Ignite-backed live status endpoint

Web team:

- Dashboard, history charts, home registration and notification APIs are connected
- Backend validation errors are displayed safely
- Switch dashboard polling to the streaming team's live-status endpoint after that endpoint is delivered
- Verify the complete anomaly → Gemini → email → notification flow during final integration

## Security

- Never commit `.env`
- Never hardcode passwords or API keys
- Keep PostgreSQL as the permanent source of truth
- Do not return raw stack traces to clients




