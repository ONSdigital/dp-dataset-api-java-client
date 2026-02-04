.PHONY: all
all: audit lint build test

.PHONY: audit
audit:
	mvn ossindex:audit

.PHONY: lint
lint:
	mvn clean checkstyle:check test-compile

.PHONY: build
build:
	mvn clean package -Dmaven.test.skip -Dossindex.skip=true

.PHONY: test
test:
	mvn clean test -Dossindex.skip
