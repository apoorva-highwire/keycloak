#!/usr/bin/env bash
# Shared config for every numbered script in THIS folder. REALM_NAME is
# derived from the folder's own name — not hardcoded — so standing up a
# new publisher is: copy this whole folder to scripts/<new-realm-name>/,
# adjust DISPLAY_NAME/ADMIN_USERNAME below if needed, run ./run-all.sh.
# Nothing else has to change.

REALM_NAME="$(basename "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)")"

CONTAINER="${KEYCLOAK_CONTAINER:-keycloak-keycloak-1}"
KC_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KC_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
KCADM="/opt/keycloak/bin/kcadm.sh"

DISPLAY_NAME="${DISPLAY_NAME:-NEJM}"

ADMIN_USERNAME="${ADMIN_USERNAME:-realm-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-changeme}"
ADMIN_ROLE="${ADMIN_ROLE:-publisher-admin}"

CLIENT_ID="${CLIENT_ID:-drupal-client}"

# Space-separated list of this publisher's journal codes — override with
# JOURNALS="code-a code-b code-c" for a publisher with a different set.
# Every journal-scoped script (06-09) loops over this same list, so it's
# the one place that actually defines "how many journals this publisher
# has" for the whole chain.
read -ra JOURNALS <<< "${JOURNALS:-nejm-core nejm-catalyst}"

# The Keycloak client that "owns" this publisher's Authorization Services
# resources/policies/permissions (see 08/09) — one per publisher, not one
# per journal; each journal gets its own RESOURCE under this same client.
RESOURCE_SERVER_CLIENT_ID="${RESOURCE_SERVER_CLIENT_ID:-${REALM_NAME}-journal-api}"

# Host-reachable addresses (these two scripts run curl from the HOST, not
# `kc()`'s docker-exec-into-the-container) — the externally-mapped ports
# from docker/docker-compose.yml, not the internal docker-network ones.
KEYCLOAK_EXTERNAL_URL="${KEYCLOAK_EXTERNAL_URL:-http://localhost:8081}"
IAM_SERVICE_URL="${IAM_SERVICE_URL:-http://localhost:8090}"

# The SAME master-realm, cross-realm-admin credential iam-service itself
# uses (see iam/src/main/resources/application.yml) — reused here to
# authenticate curl calls against iam-service's own API, treating this
# setup script as just another trusted backend caller (SERVICE scope),
# not a new credential to provision.
IAM_MASTER_CLIENT_ID="${IAM_MASTER_CLIENT_ID:-iam-service}"
IAM_MASTER_CLIENT_SECRET="${IAM_MASTER_CLIENT_SECRET:-f9EkWmySeiv04KrppIRoGQNRgG61f5aopyh7oaUl3ODONjHkFr3GCdmzMzqXhIYtlBWi3eYImSshu0RoTsJ8ju}"

kc() {
  docker exec "$CONTAINER" $KCADM "$@"
}

# A token for curl calls against iam-service's own REST API (08/09) —
# NOT the same as 01-get-token.sh's kcadm config (that's for `kc()`
# itself, stored inside the container, not usable from the host's curl).
iam_master_token() {
  curl -s -X POST "$KEYCLOAK_EXTERNAL_URL/realms/master/protocol/openid-connect/token" \
    -d grant_type=client_credentials \
    -d client_id="$IAM_MASTER_CLIENT_ID" \
    -d client_secret="$IAM_MASTER_CLIENT_SECRET" \
    | grep -o '"access_token":"[^"]*"' | sed 's/"access_token":"//;s/"$//'
}
