#!/usr/bin/env bash
# Creates the realm if it doesn't already exist — if it does (e.g. 'nejm'
# was already created by hand earlier), this just confirms that and moves
# on, so run-all.sh's chain continues to 03/04 either way; it never stops
# the chain just because this step had nothing to do.
#
# Also enables unmanaged attributes — confirmed live (first real
# journal_codes grant attempt) that Keycloak's declarative User Profile
# silently DROPS any attribute not declared in its schema unless this is
# explicitly turned on. Every realm needs it, so it's done here
# unconditionally (safe to re-run on an existing realm too).
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")" && source common.sh

EXISTING=$(kc get realms/"$REALM_NAME" --fields realm 2>/dev/null || true)

if [ -n "$EXISTING" ]; then
  echo "[02] Realm '$REALM_NAME' already exists — not recreating."
else
  echo "[02] Creating realm '$REALM_NAME' ..."
  kc create realms -s realm="$REALM_NAME" -s displayName="$DISPLAY_NAME" -s enabled=true
fi

echo "[02] Enabling unmanaged attributes on '$REALM_NAME' ..."
kc update users/profile -r "$REALM_NAME" -s unmanagedAttributePolicy=ENABLED
echo "[02] Done."
