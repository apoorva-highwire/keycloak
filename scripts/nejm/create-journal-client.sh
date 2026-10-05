#!/usr/bin/env bash
# Creates one Drupal-site client for ONE journal — run once per journal
# a publisher has, AFTER run-all.sh (not part of it: run-all.sh's 0*.sh
# glob only covers realm-wide, run-once-per-realm setup; a publisher with
# N journals calls this N times with a different JOURNAL_CODE each time).
#
# journal_codes is a CLIENT attribute here (comma-separated, though this
# script only ever sets one) — see providers/highwire-keycloak-mapper's
# JournalCodesMapper for why: a client_credentials token has no reader
# behind it, so the mapper falls back to the CLIENT's own journal_codes
# attribute instead of a user's. Needs 05-create-journal-codes-scope.sh
# to have already run in this realm, or the client scope this attaches
# won't exist yet.
#
# Usage: JOURNAL_CODE=nejm-core ./create-journal-client.sh
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")" && source common.sh

JOURNAL_CODE="${JOURNAL_CODE:?Set JOURNAL_CODE, e.g. JOURNAL_CODE=nejm-core ./create-journal-client.sh}"
JOURNAL_CLIENT_ID="drupal-$JOURNAL_CODE"
JOURNAL_CLIENT_SECRET="${JOURNAL_CLIENT_SECRET:-${JOURNAL_CODE}-secret}"

SCOPE_NAME="journal-codes"
SCOPE_ID=$(kc get client-scopes -r "$REALM_NAME" --fields id,name 2>/dev/null \
  | grep -B1 "\"$SCOPE_NAME\"" | grep -o '"[a-f0-9-]\{36\}"' | tr -d '"' || true)
if [ -z "$SCOPE_ID" ]; then
  echo "[create-journal-client] '$SCOPE_NAME' client scope doesn't exist yet in '$REALM_NAME' —" >&2
  echo "[create-journal-client] run ./05-create-journal-codes-scope.sh first." >&2
  exit 1
fi

EXISTING=$(kc get clients -r "$REALM_NAME" -q clientId="$JOURNAL_CLIENT_ID" --fields id 2>/dev/null \
  | grep -o '"[a-f0-9-]*"' | tr -d '"' || true)

if [ -n "$EXISTING" ]; then
  echo "[create-journal-client] Client '$JOURNAL_CLIENT_ID' already exists in '$REALM_NAME' (id=$EXISTING)."
  CLIENT_UUID="$EXISTING"
else
  echo "[create-journal-client] Creating client '$JOURNAL_CLIENT_ID' for journal '$JOURNAL_CODE' ..."
  kc create clients -r "$REALM_NAME" \
    -s clientId="$JOURNAL_CLIENT_ID" \
    -s publicClient=false \
    -s serviceAccountsEnabled=true \
    -s standardFlowEnabled=false \
    -s directAccessGrantsEnabled=true \
    -s secret="$JOURNAL_CLIENT_SECRET" \
    -s "attributes={\"journal_codes\":\"$JOURNAL_CODE\"}"
  CLIENT_UUID=$(kc get clients -r "$REALM_NAME" -q clientId="$JOURNAL_CLIENT_ID" --fields id 2>/dev/null \
    | grep -o '"[a-f0-9-]*"' | tr -d '"')
fi

echo "[create-journal-client] Attaching '$SCOPE_NAME' as a default scope ..."
kc update "clients/$CLIENT_UUID/default-client-scopes/$SCOPE_ID" -r "$REALM_NAME" 2>/dev/null || true

echo "[create-journal-client] Done. clientId='$JOURNAL_CLIENT_ID', secret='$JOURNAL_CLIENT_SECRET',"
echo "     journal_codes=['$JOURNAL_CODE'] on every token this client gets."
