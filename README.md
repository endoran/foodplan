# FoodPlan

A self-hosted meal planning and recipe management application with smart shopping lists, store price integration, and OCR recipe scanning.

## Features

**Recipe Management**
- Create, edit, and organize recipes with structured ingredients and instructions
- Import recipes from any URL (automatic HTML parsing of recipe websites)
- Scan printed recipes from photos or PDFs using OCR (Tesseract)
- Sub-recipe section grouping (e.g., "Marinade", "Sauce", "Filling")

**Meal Planning**
- Drag-and-drop calendar with month and week views
- Schedule recipes for breakfast, lunch, dinner, and snacks
- Adjustable serving sizes per meal

**Smart Shopping Lists**
- Automatically generated from your meal plan for any date range
- Aggregates ingredients across recipes, converts and combines units
- Subtracts pantry inventory so you only buy what you need
- Organized by grocery aisle category

**Store Price Integration**
- Real-time pricing, aisle locations, stock levels, and promo prices
- Kroger API support (Fred Meyer, Ralphs, King Soopers, and all Kroger family stores)
- CHEF'STORE / Cash 'n Carry support (via Algolia search)
- Package-aware pricing: calculates how many store packages you need and the total cost
- Extensible strategy pattern for adding new stores

**Inventory Tracking**
- Track what you have on hand with quantities and expiration dates
- Quick-cook mode: deduct ingredients when you make a meal

**Multi-Tenant**
- Organization-based isolation: each household gets its own data
- User registration and JWT-based authentication

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.4, Spring Security, Spring Data MongoDB |
| Database | MongoDB 7 |
| Frontend | React 19, TypeScript 5.7, React Router 7, Vite 6 |
| OCR | Tesseract 5 (via Tess4J) with tessdata_best LSTM model |
| PDF | Apache PDFBox 3 |
| HTML Parsing | jsoup |
| API Docs | Swagger UI (springdoc-openapi) |
| Containerization | Docker Compose |

## Quick Start (Docker)

The fastest way to run FoodPlan:

```bash
git clone https://github.com/endoran/foodplan.git
cd foodplan
docker compose up -d
```

Open [http://localhost:3000](http://localhost:3000) and register an account.

This starts MongoDB, the Spring Boot backend, and the React frontend with nginx. No external dependencies required.

## Quick Start (Local Development)

### Prerequisites

- Java 21 (e.g., [Eclipse Temurin](https://adoptium.net/))
- Node.js 20+
- MongoDB 7 (running on `localhost:27017`)
- Tesseract 5 (for OCR features): `brew install tesseract` / `apt install tesseract-ocr`

### Backend

```bash
./gradlew bootRun
```

The API starts at `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Opens at `http://localhost:5173` with hot reload. API requests proxy to `localhost:8080`.

## Configuration

All configuration is via environment variables. Copy `.env.example` to `.env` and customize:

```bash
cp .env.example .env
```

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | Yes (prod) | dev key | Base64-encoded 256-bit signing key |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:3000` | Frontend URL for CORS |
| `MONGODB_URI` | No | `mongodb://localhost:27017/food` | MongoDB connection string |
| `SLACK_ERROR_TOKEN` | No | — | Slack bot token for error notifications |
| `SLACK_ERROR_CHANNEL` | No | — | Slack channel ID for error posts |
| `KROGER_CLIENT_ID` | No | — | Kroger API OAuth2 client ID |
| `KROGER_CLIENT_SECRET` | No | — | Kroger API OAuth2 client secret |
| `KROGER_LOCATION_ID` | No | — | Kroger store location ID |
| `CHEFSTORE_ALGOLIA_APP_ID` | No | `70KQ5FEQ31` | CHEF'STORE Algolia app ID |
| `CHEFSTORE_ALGOLIA_API_KEY` | No | `48035a...` | CHEF'STORE Algolia search key |
| `CHEFSTORE_STORE_NUMBER` | No | `553` | CHEF'STORE location number |

### Store API Setup

**Kroger** (Fred Meyer, Ralphs, King Soopers, etc.)

1. Register at [developer.kroger.com](https://developer.kroger.com)
2. Create an application to get a client ID and secret
3. Find your store's location ID via the [Location API](https://developer.kroger.com/api-ref/location)
4. Set `KROGER_CLIENT_ID`, `KROGER_CLIENT_SECRET`, and `KROGER_LOCATION_ID`

**CHEF'STORE / Cash 'n Carry**

Works out of the box with the default Algolia credentials. To use a different store location, change `CHEFSTORE_STORE_NUMBER` to your store's number (visible on their website).

## Production Deployment

1. Copy the example production compose file:
   ```bash
   cp docker-compose.prod.example.yml docker-compose.prod.yml
   ```

2. Edit `docker-compose.prod.yml` — set your `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, and any store API credentials.

3. Generate a JWT secret:
   ```bash
   openssl rand -base64 32
   ```

4. Start the stack:
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
   ```

The application will be available on port 3001 (configurable in `docker-compose.prod.yml`).

This works on any machine with Docker: Linux servers, macOS, Windows (WSL2), cloud VMs, Railway, Fly.io, etc.

## API Documentation

Interactive API documentation is available via Swagger UI at `/swagger-ui` when the backend is running.

All endpoints are under `/api/v1/`:

| Resource | Endpoints |
|----------|-----------|
| Auth | `POST /auth/register`, `POST /auth/login`, `GET /auth/me` |
| Recipes | CRUD + `POST /recipes/import` (URL) + `POST /recipes/scan` (OCR) |
| Ingredients | CRUD + batch create + bulk update + auto-categorize |
| Meal Plan | CRUD + confirm meals |
| Shopping List | `GET /shopping-list?from=&to=&store=` |
| Inventory | CRUD + batch deduct |
| Reference Data | Units, categories, tags, meal types, store types |

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────┐
│   React UI  │────>│  Spring Boot │────>│ MongoDB │
│  (nginx)    │     │   REST API   │     │         │
└─────────────┘     └──────┬───────┘     └─────────┘
                           │
                    ┌──────┴───────┐
                    │    Store     │
                    │ Enrichment   │
                    │ Orchestrator │
                    ├──────┬───────┤
                    │ Kroger│Chef  │
                    │  API  │Store │
                    └───────┴──────┘
```

- **Multi-tenant**: every document is scoped to an organization via `orgId`
- **Authentication**: JWT (HS256) with 24-hour expiration
- **Store integration**: strategy pattern — implement `StoreEnrichmentService` to add new stores
- **Unit conversion**: volume (TSP→GALLON) and weight (OZ→LBS) families with cross-family bridging (oz weight ≈ fl oz volume)
- **OCR pipeline**: EXIF orientation correction → grayscale preprocessing → Tesseract LSTM → structured text parsing

## Roadmap

Potential future features (contributions welcome):

- **More stores**: Costco, Walmart, Safeway, WinCo
- **Nutrition tracking**: calories, macros per recipe and meal plan
- **Meal suggestions**: recommend recipes based on what's in your pantry
- **Recipe sharing**: public recipe URLs, import from other FoodPlan users
- **Mobile app**: React Native or PWA
- **Barcode scanning**: scan grocery receipts to auto-update inventory

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup, coding guidelines, and the PR process.

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
