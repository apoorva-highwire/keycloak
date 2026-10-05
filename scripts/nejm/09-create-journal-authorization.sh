#!/usr/bin/env bash
# For each journal in JOURNALS: one resource (read+write scopes), one
# role-policy per scope, and the permission tying them together — all
# through iam-service's own REST API (ResourceController/PolicyController/
# PermissionController), not raw Keycloak admin REST, since Authorization
# Services config is exactly what iam-service exists to own. Idempotent
# via GET-before-POST (iam-service's create endpoints 409 on a repeat,
# same as every other create endpoint in this project).
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")" && source common.sh

BASE="$IAM_SERVICE_URL/api/v1/realms/$REALM_NAME/clients/$RESOURCE_SERVER_CLIENT_ID"

# Re-fetched per journal, not once at the top — this master token is
# short-lived (~60s observed live), and a publisher with many journals
# could easily outlive it mid-loop, turning every later call into a
# silent 401 instead of a clean create/skip.
refresh_token() {
  TOKEN=$(iam_master_token)
  if [ -z "$TOKEN" ]; then
    echo "[09] Could not get a master-realm token — is Keycloak reachable at $KEYCLOAK_EXTERNAL_URL?" >&2
    exit 1
  fi
}

exists() {
  local path="$1"
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/$path") || true
  [ "$status" = "200" ]
}

post() {
  local path="$1"
  local body="$2"
  local status resp_file
  # A fresh temp file per call, not one shared fixed path — a shared
  # path saw intermittent curl exit-23 "write error"s under rapid
  # successive calls (confirmed live, Windows/git-bash filesystem
  # timing), which `set -e` turned into a silent, unexplained script
  # death with no FAILED line ever printed.
  resp_file=$(mktemp)
  status=$(curl -s -o "$resp_file" -w "%{http_code}" -X POST \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    "$BASE/$path" -d "$body") || true
  if [ "$status" != "201" ]; then
    echo "[09]   FAILED ($status): $(cat "$resp_file")" >&2
    rm -f "$resp_file"
    return 1
  fi
  rm -f "$resp_file"
}

for journal in "${JOURNALS[@]}"; do
  resource="${journal}-articles"
  read_policy="${journal}-readers"
  write_policy="${journal}-writers"

  refresh_token
  echo "[09] Journal '$journal':"

  if exists "resources/$resource"; then
    echo "[09]   resource '$resource' already exists."
  else
    echo "[09]   creating resource '$resource' (scopes: read, write) ..."
    post "resources" "{\"name\":\"$resource\",\"scopes\":[\"read\",\"write\"]}"
  fi

  if exists "policies/$read_policy"; then
    echo "[09]   policy '$read_policy' already exists."
  else
    echo "[09]   creating policy '$read_policy' (role: ${journal}-read) ..."
    post "policies" "{\"name\":\"$read_policy\",\"roleNames\":[\"${journal}-read\"]}"
  fi

  if exists "policies/$write_policy"; then
    echo "[09]   policy '$write_policy' already exists."
  else
    echo "[09]   creating policy '$write_policy' (role: ${journal}-write) ..."
    post "policies" "{\"name\":\"$write_policy\",\"roleNames\":[\"${journal}-write\"]}"
  fi

  if exists "permissions/${resource}-read-perm"; then
    echo "[09]   permission '${resource}-read-perm' already exists."
  else
    echo "[09]   creating permission '${resource}-read-perm' ..."
    post "permissions" "{\"name\":\"${resource}-read-perm\",\"resourceName\":\"$resource\",\"scopeNames\":[\"read\"],\"policyNames\":[\"$read_policy\"]}"
  fi

  if exists "permissions/${resource}-write-perm"; then
    echo "[09]   permission '${resource}-write-perm' already exists."
  else
    echo "[09]   creating permission '${resource}-write-perm' ..."
    post "permissions" "{\"name\":\"${resource}-write-perm\",\"resourceName\":\"$resource\",\"scopeNames\":[\"write\"],\"policyNames\":[\"$write_policy\"]}"
  fi
done

echo "[09] Done. Authorization config ready for: ${JOURNALS[*]}"
echo "     Runtime check: audience=$RESOURCE_SERVER_CLIENT_ID, permission={journal}-articles#{read|write}"
