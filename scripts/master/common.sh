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

kc() {
  docker exec "$CONTAINER" $KCADM "$@"
}
