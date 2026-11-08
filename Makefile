.PHONY: up down restart build logs ps \
        shell-backend shell-frontend \
        test check compile verify lint format test-frontend test-template build-jar build-services image sbom \
        init clean-volumes help

DC      = docker compose -f compose.dev.yml
GRADLE  = $(DC) exec backend gradle

help: ## Muestra esta ayuda
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-22s\033[0m %s\n", $$1, $$2}'

# ── Servicios ─────────────────────────────────────────────────────────────────

init: ## Personaliza una copia nueva: nombre visible y namespace Java
	python3 backend/scripts/init-template.py

up: ## Levanta todos los servicios en segundo plano
	$(DC) up -d

down: ## Detiene y elimina todos los contenedores
	$(DC) down

restart: ## Reinicia todos los servicios
	$(DC) restart

build: ## Reconstruye las imágenes
	$(DC) build

build-no-cache: ## Reconstruye sin caché
	$(DC) build --no-cache

logs: ## Logs de todos los servicios en tiempo real
	$(DC) logs -f

logs-backend: ## Logs solo del backend
	$(DC) logs -f backend

ps: ## Estado de los contenedores
	$(DC) ps

# ── Shells ────────────────────────────────────────────────────────────────────

shell-backend: ## Shell en el contenedor backend
	$(DC) exec backend sh

shell-frontend: ## Shell en el contenedor frontend
	$(DC) exec frontend sh

shell-db: ## psql directo en postgres
	$(DC) exec postgres psql -U $${POSTGRES_USER:-postgres} -d $${POSTGRES_DB:-boilerplate_spring}

# ── Java / Gradle ─────────────────────────────────────────────────────────────
# The Gradle root project is backend/; services/ are optional included subprojects.

test: ## Ejecuta los tests dentro del contenedor
	$(GRADLE) test --no-daemon

compile: ## Compila sin ejecutar nada
	$(GRADLE) compileJava --no-daemon

check: ## Gate completo: tests + ArchUnit + Checkstyle + SpotBugs + Error Prone + Spotless
	$(GRADLE) check --no-daemon

format: ## Auto-formatea el código Java (Spotless / palantir-java-format)
	$(GRADLE) spotlessApply --no-daemon

verify: check tsc test-frontend ## check del backend + type-check + tests del frontend

build-jar: ## Genera el JAR de producción
	$(GRADLE) bootJar -x test --no-daemon

image: ## Construye la imagen del backend con Jib (daemon Docker local)
	$(GRADLE) jibDockerBuild --no-daemon

sbom: ## Genera el SBOM CycloneDX (backend/build/reports/bom.json)
	$(GRADLE) cyclonedxBom --no-daemon

deps: ## Descarga dependencias
	$(GRADLE) dependencies --no-daemon

build-services: ## Compila los scaffolds opcionales de microservicios
	$(GRADLE) buildServices --no-daemon --max-workers=1

test-template: ## Prueba el inicializador de copias nuevas
	python3 -m unittest discover -s backend/scripts -p 'test_*.py'

# ── Frontend ──────────────────────────────────────────────────────────────────

install-frontend: ## pnpm install en el frontend
	$(DC) exec frontend pnpm install

tsc: ## TypeScript check en el frontend
	$(DC) exec frontend pnpm exec tsc --noEmit

lint: ## oxlint en el frontend
	$(DC) exec frontend pnpm run lint

test-frontend: ## Vitest en el frontend
	$(DC) exec frontend pnpm run test:run

# ── BD ────────────────────────────────────────────────────────────────────────

clean-volumes: ## Elimina contenedores y volúmenes  ⚠️  borra datos
	$(DC) down -v
