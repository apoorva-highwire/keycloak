#!/usr/bin/env bash
# Creates the iam-service client in the master realm, with a service
# account granted master's own "admin" role (confirmed live: master has
# "admin", a composite bundling full super-admin rights — same one the
# bootstrap admin user holds — vs. the narrower, non-composite
# "create-realm" role, which can create realms but not manage what's
# inside them afterward). "admin" is realm-independent, so it already
# covers any realm created after this script runs, including ones that
# don't exist yet — unlike granting per-realm {realm}-realm proxy client
# roles one at a time, which only works for realms that already exist.
#
# Run once per environment. In production, run this against that
# environment's own Keycloak with its own bootstrap admin, then put the
# printed secret into iam-service's config via a secrets store — never
# hardcode it, and never reuse the local dev secret anywhere else.
#
# Usage: ./scripts/create-iam-client.sh
set -euo pipefail

CONTAINER="${KEYCLOAK_CONTAINER:-keycloak-keycloak-1}"
KC_ADMIN="${KEYCLOAK_ADMIN:-admin}"
KC_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
CLIENT_ID="${IAM_CLIENT_ID:-iam-service}"

KCADM="/opt/keycloak/bin/kcadm.sh"

docker exec "$CONTAINER" $KCADM config credentials \
  --server http://localhost:8080 --realm master \
  --user "$KC_ADMIN" --password "$KC_ADMIN_PASSWORD"

EXISTING=$(docker exec "$CONTAINER" $KCADM get clients -r master -q clientId="$CLIENT_ID" --fields id 2>/dev/null | grep -o '"[a-f0-9-]*"' | tr -d '"' || true)

if [ -n "$EXISTING" ]; then
  echo "Client '$CLIENT_ID' already exists in master (id=$EXISTING) — not recreating."
  CLIENT_UUID="$EXISTING"
else
  echo "Creating client '$CLIENT_ID' in master ..."
  CLIENT_UUID=$(docker exec "$CONTAINER" $KCADM create clients -r master \
    -s clientId="$CLIENT_ID" \
    -s serviceAccountsEnabled=true \
    -s publicClient=false \
    -s standardFlowEnabled=false \
    -s directAccessGrantsEnabled=false \
    -i)
  echo "Created client, id=$CLIENT_UUID"
fi

echo "Granting master's 'admin' role to the service account ..."
docker exec "$CONTAINER" $KCADM add-roles -r master \
  --uusername "service-account-$CLIENT_ID" \
  --rolename admin

echo ""
echo "Client secret:"
docker exec "$CONTAINER" $KCADM get clients/"$CLIENT_UUID"/client-secret -r master 2>/dev/null \
  | grep '"value"' | sed -E 's/.*"value"\s*:\s*"([^"]*)".*/\1/'

echo ""
echo "Done. Put this into iam-service's config as the client_credentials"
echo "grant's client_id/client_secret — token endpoint:"
echo "  http://keycloak:8080/realms/master/protocol/openid-connect/token"
