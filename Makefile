LAST_TAG   = $(shell git tag --sort -v:refname | head -1)
COMMITS    = $(shell git rev-list -1 $(LAST_TAG)..HEAD)
TAGS_RANGE = $(shell git tag --sort -v:refname | head -2 | tail -r | sed 'N;s/\n/.../')

patch: NEXT_VERSION = $(shell echo $(LAST_TAG) | awk -F'[v.]' '{$$4++; print $$2"."$$3"."$$4}')
minor: NEXT_VERSION = $(shell echo $(LAST_TAG) | awk -F'[v.]' '{$$3++; print $$2"."$$3".0"}')
major: NEXT_VERSION = $(shell echo $(LAST_TAG) | awk -F'[v.]' '{$$2++; print $$2".0.0"}')

patch minor major: graph
	@if [ -z "${COMMITS}" ]; then echo "No new commits found after ${LAST_TAG}, aborting."; exit 1; fi
	@if [ -n "$(git ls-files -om graph.svg)" ]; then echo "Uncommited graph.svg detected, aborting."; exit 1; fi
	@git tag -s "v${NEXT_VERSION}" -m "Version ${NEXT_VERSION}"

release: check
	@git push origin HEAD:master
	@git push --tags origin HEAD:master
	@hub release create -m "$$(echo "${LAST_TAG}\n\n$$(git log --format='* %h %s' ${TAGS_RANGE})")" "${LAST_TAG}"

check:
	@if ! which hub terraform dot > /dev/null; then echo "Missing dependency. Required: hub, terraform, dot." && exit 1; fi;

graph:
	@terraform graph -module-depth=100 -draw-cycles | dot -Gsplines=ortho -Gconcentrate=true -Grankdir=RL -Tsvg > graph.svg

.PHONY: patch minor major release check graph
