#!/usr/bin/env bash
# The one Keycloak client this publisher's Authorization Services
# resources/policies/permissions live under (see 09) — confidential,
# authorizationServicesEnabled=true is the one setting that actually
# matters here; without it, iam-service's ResourceController/
# PolicyController/PermissionController calls against this client
# fail (confirmed live against a client missing this flag).
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")" && source common.sh

EXISTING=$(kc get clients -r "$REALM_NAME" -q clientId="$RESOURCE_SERVER_CLIENT_ID" --fields id 2>/dev/null \
  | grep -o '"[a-f0-9-]*"' | tr -d '"' || true)

if [ -n "$EXISTING" ]; then
  echo "[08] Resource-server client '$RESOURCE_SERVER_CLIENT_ID' already exists in '$REALM_NAME' (id=$EXISTING)."
else
  echo "[08] Creating resource-server client '$RESOURCE_SERVER_CLIENT_ID' in '$REALM_NAME' ..."
  kc create clients -r "$REALM_NAME" \
    -s clientId="$RESOURCE_SERVER_CLIENT_ID" \
    -s publicClient=false \
    -s serviceAccountsEnabled=true \
    -s authorizationServicesEnabled=true \
    -s standardFlowEnabled=false \
    -s directAccessGrantsEnabled=false \
    -s secret="${RESOURCE_SERVER_CLIENT_ID}-secret"
fi

echo "[08] Done. Resource-server clientId='$RESOURCE_SERVER_CLIENT_ID'"
echo "     (used by 09, and by any consuming service's highwire.auth.resource-server-client-id)."
