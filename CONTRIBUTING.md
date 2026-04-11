# Contributing to FoodPlan

Thank you for your interest in contributing to FoodPlan! This guide covers the development workflow and expectations for pull requests.

## Development Setup

### Prerequisites

- Java 21 ([Eclipse Temurin](https://adoptium.net/) recommended)
- Node.js 20+
- MongoDB 7 (local or Docker)
- Tesseract 5 (for OCR): `brew install tesseract` / `apt install tesseract-ocr`

### Running Locally

**Backend:**
```bash
./gradlew bootRun
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

**With Docker (full stack):**
```bash
docker compose up -d
```

## Workflow

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature` or `fix/your-fix`
3. Make your changes
4. Run tests (see below)
5. Commit with a descriptive message
6. Push and open a pull request

## Running Tests

Before submitting a PR, make sure both pass:

```bash
# Backend (unit + integration tests with embedded MongoDB)
./gradlew test

# Frontend (TypeScript type checking)
cd frontend && npx tsc --noEmit
```

## Code Style

- Follow existing patterns in the codebase
- Backend: standard Java/Spring conventions, records for DTOs
- Frontend: functional React components, TypeScript strict mode
- Keep changes focused — one concern per PR
- Tests for new backend logic (functional tests preferred over heavy mocking)

## Adding a New Store Integration

FoodPlan uses a strategy pattern for store integrations. To add a new store:

1. Add the store to `StoreType.java` enum
2. Implement the `StoreEnrichmentService` interface:
   ```java
   public interface StoreEnrichmentService {
       Map<String, StoreProductMatch> enrich(List<String> ingredientNames);
       String storeName();
   }
   ```
3. Register it in `StoreEnrichmentOrchestrator`
4. Add the store option in `frontend/src/inventory/ShoppingListPage.tsx`
5. Document any API keys or setup required in `.env.example`

## Pull Request Guidelines

- Describe what your PR does and why
- Include a test plan (how to verify)
- Keep PRs reasonably sized — split large changes into multiple PRs if possible
- Make sure tests pass before requesting review

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
