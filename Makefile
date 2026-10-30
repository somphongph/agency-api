init:
	@echo "Initializing..."
	@pre-commit install
	@./mvnw -B -ntp dependency:go-offline

dev:
	@echo "Running..."
	@./mvnw spring-boot:run

test:
	@echo "Testing..."
	@./mvnw -B -ntp test

test-cover:
	@echo "Testing with coverage..."
	@./mvnw -B -ntp test

clean:
	@echo "Cleaning..."
	@./mvnw -B -ntp clean

vuln:
	@echo "Checking for vulnerabilities..."
	@trivy fs \
		--db-repository ghcr.io/aquasecurity/trivy-db \
		--timeout 15m \
		--exit-code 1 \
		--severity HIGH,CRITICAL .

sec:
	@echo "Checking for security issues..."
	@trivy fs \
		--db-repository ghcr.io/aquasecurity/trivy-db \
		--timeout 15m \
		--exit-code 1 \
		--severity HIGH,CRITICAL .

build:
	@echo "Building..."
	@./mvnw -B -ntp package -DskipTests

trivy:
	@echo "Running Trivy scan..."
	@trivy fs \
		--db-repository ghcr.io/aquasecurity/trivy-db \
		--timeout 15m \
		--exit-code 1 \
		--severity HIGH,CRITICAL .

check:
	@echo "Pre-commit check..."
	@pre-commit run --all-files
