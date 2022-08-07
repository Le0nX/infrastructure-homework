#SHELL := /bin/bash

# ==============================================================================
# Makefile Description
#		- Show all targets:
# 			- make list
#
# ==============================================================================
# Run docker compose:
# 		- make compose-up BUILD_TYPE=[dev,prod] IMG_TAG=v1.0(default: latest) PORT=9090(default: 8080)
#
# Stop docker compose:
# 		- make compose-down BUILD_TYPE=[dev,prod]
#
# Reload docker compose with rebuilding the image:
# 		- make compose-reload BUILD_TYPE=[dev,prod] IMG_TAG=v1.0(default: latest) PORT=9090(default: 8080)
#
# Get docker compose logs:
# 		- make compose-logs BUILD_TYPE=[dev,prod]
#
# ==============================================================================
# Building Docker containers:
#		- make docker-build BUILD_TYPE=[dev,prod] IMG_TAG=v1.0(default: latest) PORT=9090(default: 8080)
#
# Pushing Docker containers to AWS:
#		- make docker-push BUILD_TYPE=[dev,prod] IMG_TAG=v1.0(default: latest) PORT=9090(default: 8080) AWS_REPO=aws_account_id.dkr.ecr.region.amazonaws.com/my-repository
#
# ==============================================================================
# Local run:
# 		- make local-run LOG_LVL=[warn,info,debug,trace]
#



# ==============================================================================
# CLI Arguments

PORT ?= 8080
IMG_TAG ?= latest # to get latest version fom gradle - run ./gradlew properties --no-daemon --console=plain -q | grep "^version:" | awk '{printf $NF}'
VCS_REF = `git rev-parse --verify --short HEAD`
DATE = `date -u +"%Y-%m-%dT%H:%M:%SZ"`

# ==============================================================================
# Running from within docker compose

# rebuilds docker image and restarts compose
compose-reload: compose-down local-build docker-build compose-up

compose-up:
	@echo "compose-up [BUILD_TYPE=$(BUILD_TYPE)]"
	@:$(call check_defined, BUILD_TYPE, dev prod)
	docker-compose -f tools/devops/compose-config.yaml -f tools/devops/$(BUILD_TYPE).compose.yaml up --detach --remove-orphans

compose-down:
	@echo "compose-down [BUILD_TYPE=$(BUILD_TYPE)]"
	@:$(call check_defined, BUILD_TYPE, dev prod)
	docker-compose -f tools/devops/compose-config.yaml -f tools/devops/$(BUILD_TYPE).compose.yaml down --remove-orphans

compose-logs:
	@echo "compose-logs [BUILD_TYPE=$(BUILD_TYPE)]"
	@:$(call check_defined, BUILD_TYPE, dev prod)
	docker-compose -f tools/devops/$(BUILD_TYPE).compose.yaml logs -f


# ==============================================================================
# Push prod image to AWS

docker-push: docker-build
	@echo "docker-push [AWS_REPO=$(AWS_REPO) BUILD_TYPE=$(BUILD_TYPE) IMG_TAG=$(IMG_TAG) PORT=$(PORT)]"
	@:$(call check_defined, AWS_REPO, aws_account_id.dkr.ecr.region.amazonaws.com/my-repository)
	docker tag people-api-$(BUILD_TYPE):$(IMG_TAG) $(AWS_REPO):$(IMG_TAG)
	docker push $(AWS_REPO):$(IMG_TAG)

# ==============================================================================
# Building containers -t people-api-$(BUILD_TYPE):$(IMG_TAG) \

docker-build:
	@echo "docker-build [BUILD_TYPE=$(BUILD_TYPE) IMG_TAG=$(IMG_TAG) PORT=$(PORT)]"
	@:$(call check_defined, BUILD_TYPE, dev prod)
	docker build \
		-f tools/devops/$(BUILD_TYPE).dockerfile \
		-t people-api-$(BUILD_TYPE):$(IMG_TAG) \
		--build-arg PORT=$(PORT) \
		--build-arg VCS_REF=$(VCS_REF) \
		--build-arg BUILD_DATE=$(DATE) \
		.


# ==============================================================================
# Docker support

FILES := $(shell docker ps -aq)

docker-down:
	docker stop $(FILES)
	docker rm $(FILES)

docker-clean:
	docker system prune -f

docker-logs:
	docker logs -f $(FILES)


# ==============================================================================
# Local run

local-build: local-clean
	@echo "local-build"
	./gradlew bootJar -PbuildSHA=$(VCS_REF) --warning-mode=all

local-run: local-build
	@echo "local-run [LOG_LVL=$(LOG_LVL)]"
	@:$(call check_defined, LOG_LVL, info warn debug trace)
	java -jar build/libs/*.jar --logging.level.root=$(LOG_LVL)

local-clean:
	@echo "local-clean..."
	./gradlew clean

local-clean-full: local-clean docker-clean
	@echo "local-clean-full..."
	./gradlew dependencyCheckPurge

test: local-build
	@echo "building the project locally..."
	./gradlew test

check: local-build
	@echo "Checking the project after the local build..."
	./gradlew check

#==============================================================================
# AWS

aws-up:
	 aws ecs update-service --desired-count 1 --cluster "stringconcat-course" --service "spring-service"

aws-down:
	 aws ecs update-service --desired-count 0 --cluster "stringconcat-course" --service "spring-service"

#==============================================================================
# CI/CD

ci-run-and-check:
	@echo "Run as CI does..."

# ==============================================================================
# List all targets

.PHONY: list
list:
	@echo "\nTARGETS:\n"
	@LC_ALL=C $(MAKE) -pRrq -f $(lastword $(MAKEFILE_LIST)) : 2>/dev/null | awk -v RS= -F: '/^# File/,/^# Finished Make data base/ {if ($$1 !~ "^[#.]") {print $$1}}' | sort | egrep -v -e '^[^[:alnum:]]' -e '^$@$$'
	@echo ""



# ==============================================================================
# Helper functions

# Check that given variables are set and all have non-empty values,
# die with an error otherwise.
#
# Params:
#   1. Variable name(s) to test.
#   2. (optional) Error message to print.
check_defined = \
    $(strip $(foreach 1,$1, \
        $(call __check_defined,$1,$(strip $(value 2)))))
__check_defined = \
    $(if $(value $1),, \
        $(error Undefined $1$(if $2, ($2))$(if $(value @), \
                required by target '$@')))
