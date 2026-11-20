# ntt-data-assessment-api

Spring WebFlux API over SQLite `ntt-data-assessment-db`.

## Prerequisites

1. Build DB: `python ../../database/scripts/build_database.py`
2. (Optional AI) start ollama-api on 8092 and Ollama+Qwen

## Run

```bash
mvn spring-boot:run
```

Port `8091`. OpenAPI at `/openapi.yaml`.
