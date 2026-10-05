#!/usr/bin/env bash
# Import one realm JSON (realms/*.json) into the running local Keycloak
# container via kcadm.sh. Idempotent-ish: Keycloak refuses to create a
# realm that already exists, so re-running after a realm was created by
# hand first requires deleting it, or editing it via the admin console.
#
# Usage: ./scripts/import-realm.sh scripts/realms/highwire-admin.json
set -euo pipefail

REALM_FILE="${1:?Usage: $0 <path-to-realm-json>}"
CONTAINER="${KEYCLOAK_CONTAINER:-keycloak-keycloak-1}"
KC_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KC_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

BASENAME="$(basename "$REALM_FILE")"
DEST="/tmp/$BASENAME"

# cd into the file's own directory and pass only its basename (a relative
# path) to docker cp — on Windows + Git Bash, docker cp's path-conversion
# logic mangles ANY absolute unix-style source path, whether /c/... or
# /tmp/..., into a bogus Windows path (confirmed both ways); a relative
# path from the right cwd is the one form that survives untouched.
(cd "$(dirname "$REALM_FILE")" && docker cp "$BASENAME" "$CONTAINER:$DEST")

docker exec "$CONTAINER" /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master \
  --user "$KC_ADMIN" --password "$KC_ADMIN_PASSWORD"

docker exec "$CONTAINER" /opt/keycloak/bin/kcadm.sh create realms -f "$DEST"

REALM_NAME=$(grep -o '"realm"[[:space:]]*:[[:space:]]*"[^"]*"' "$REALM_FILE" | head -1 | sed -E 's/.*"realm"[[:space:]]*:[[:space:]]*"([^"]*)".*/\1/')

# Confirmed live (first real journal grant attempt, 'nejm' realm):
# Keycloak's declarative User Profile silently DROPS any user/client
# attribute not declared in its schema (journal_codes is exactly that
# kind) unless unmanagedAttributePolicy is explicitly enabled — the realm
# default is NOT enabled. kcadm's `update -s` does a GET-merge-PUT, so
# this only touches this one field, leaving the rest of the profile
# config (declared attributes, groups) untouched.
docker exec "$CONTAINER" /opt/keycloak/bin/kcadm.sh update users/profile -r "$REALM_NAME" \
  -s unmanagedAttributePolicy=ENABLED

echo "Imported $(basename "$REALM_FILE") — realm '$REALM_NAME' is now live, unmanaged attributes enabled."
