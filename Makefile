LAST_TAG := $(shell git describe --abbrev=0 --tags)
COMMITS := $(shell git rev-list -1 $(LAST_TAG)..HEAD)

patch: NEXT_VERSION = $(shell echo $(LAST_TAG) | awk -F'[v.]' '{$$4++; print $$2"."$$3"."$$4}')
minor: NEXT_VERSION = $(shell echo $(LAST_TAG) | awk -F'[v.]' '{$$3++; print $$2"."$$3".0"}')
major: NEXT_VERSION = $(shell echo $(LAST_TAG) | awk -F'[v.]' '{$$2++; print $$2".0.0"}')

patch minor major: graph
	@if [ -z "${COMMITS}" ]; then echo "No new commits found after ${LAST_TAG}, aborting."; fi
	@if [ -n "${COMMITS}" ]; then git tag -s "v${NEXT_VERSION}" -m "Version ${NEXT_VERSION}"; fi

release: check
	@git push origin HEAD:master
	@git push --tags origin HEAD:master
	@hub release create "v${NEXT_VERSION}"

check:
	@if ! which hub terraform dot > /dev/null; then echo "Missing dependency. Required: hub, terraform, dot." && exit 1; fi;

.PHONY: patch minor major release check
