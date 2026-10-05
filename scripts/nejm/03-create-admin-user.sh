#!/usr/bin/env bash
# Creates ADMIN_USERNAME in the realm if missing, sets a non-temporary
# password, and makes sure ADMIN_ROLE exists and is assigned. Realms
# created ad hoc (e.g. via an API call rather than the full
# publisher-template.json) may not have the standard
# reader/author/reviewer/editor/publisher-admin roles yet — this creates
# the role too if it's missing, rather than assuming it's already there.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")" && source common.sh

EXISTING_ROLE=$(kc get roles/"$ADMIN_ROLE" -r "$REALM_NAME" --fields name 2>/dev/null || true)
if [ -z "$EXISTING_ROLE" ]; then
  echo "[03] Role '$ADMIN_ROLE' doesn't exist in '$REALM_NAME' — creating it ..."
  kc create roles -r "$REALM_NAME" -s name="$ADMIN_ROLE"
else
  echo "[03] Role '$ADMIN_ROLE' already exists in '$REALM_NAME'."
fi

EXISTING_USER_ID=$(kc get users -r "$REALM_NAME" -q username="$ADMIN_USERNAME" --fields id 2>/dev/null \
  | grep -o '"[a-f0-9-]*"' | tr -d '"' || true)

if [ -n "$EXISTING_USER_ID" ]; then
  echo "[03] User '$ADMIN_USERNAME' already exists in '$REALM_NAME' (id=$EXISTING_USER_ID) — not recreating."
else
  echo "[03] Creating user '$ADMIN_USERNAME' in '$REALM_NAME' ..."
  kc create users -r "$REALM_NAME" -s username="$ADMIN_USERNAME" -s enabled=true
  EXISTING_USER_ID=$(kc get users -r "$REALM_NAME" -q username="$ADMIN_USERNAME" --fields id 2>/dev/null \
    | grep -o '"[a-f0-9-]*"' | tr -d '"')
fi

echo "[03] Setting password for '$ADMIN_USERNAME' ..."
kc set-password -r "$REALM_NAME" --username "$ADMIN_USERNAME" --new-password "$ADMIN_PASSWORD"

echo "[03] Assigning role '$ADMIN_ROLE' to '$ADMIN_USERNAME' ..."
kc add-roles -r "$REALM_NAME" --uusername "$ADMIN_USERNAME" --rolename "$ADMIN_ROLE"

echo "[03] Done. Login: username='$ADMIN_USERNAME' password='$ADMIN_PASSWORD' realm='$REALM_NAME'"
