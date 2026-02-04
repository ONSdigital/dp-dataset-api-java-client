.PHONY: all
all: audit lint build test

.PHONY: audit
audit:
	mvn ossindex:audit

## TODO: Add linting when time available
.PHONY: lint
lint:
	exit

.PHONY: build
build:
	mvn clean package -Dmaven.test.skip -Dossindex.skip=true

.PHONY: test
test:
	mvn clean test -Dossindex.skip
