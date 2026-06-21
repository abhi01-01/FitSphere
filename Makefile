BACKEND_SERVICES := eureka configserver userservice activityservice aiservice gateway
FRONTEND_DIR := fitness-app-frontend

.PHONY: build-backend test-backend build-frontend lint-frontend compose-up compose-down clean

build-backend:
	@set -e; for service in $(BACKEND_SERVICES); do \
		echo "Building $$service"; \
		(cd $$service && ./mvnw -q -DskipTests package); \
	done

test-backend:
	@set -e; for service in $(BACKEND_SERVICES); do \
		echo "Testing $$service"; \
		(cd $$service && ./mvnw test); \
	done

build-frontend:
	@cd $(FRONTEND_DIR) && npm run build

lint-frontend:
	@cd $(FRONTEND_DIR) && npm run lint

compose-up:
	docker compose up --build

compose-down:
	docker compose down

clean:
	@set -e; for service in $(BACKEND_SERVICES); do \
		(cd $$service && ./mvnw -q clean); \
	done
	@rm -rf $(FRONTEND_DIR)/dist
