#!/usr/bin/env bash
# One Drupal-site client PER journal in JOURNALS — the loop-ified form
# of create-journal-client.sh (kept standalone too, for adding ONE more
# journal later without re-running everything). journal_codes is a
# CLIENT attribute (comma-separated, though always one value here) —
# see providers/highwire-keycloak-mapper's JournalCodesMapper: a
# client_credentials token has no reader behind it, so the mapper falls
# back to the client's own attribute instead of a user's.
#
# multivalued=true on 05's mapper is what keeps this a real JSON array
# ["code"] on the token instead of a bare string "code" — confirmed
# live; see 05-create-journal-codes-scope.sh's own comment.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")" && source common.sh

SCOPE_NAME="journal-codes"
SCOPE_ID=$(kc get client-scopes -r "$REALM_NAME" --fields id,name 2>/dev/null \
  | grep -B1 "\"$SCOPE_NAME\"" | grep -o '"[a-f0-9-]\{36\}"' | tr -d '"' || true)
if [ -z "$SCOPE_ID" ]; then
  echo "[07] '$SCOPE_NAME' client scope doesn't exist yet — run 05-create-journal-codes-scope.sh first." >&2
  exit 1
fi

for journal in "${JOURNALS[@]}"; do
  client_id="drupal-$journal"
  secret="${journal}-secret"

  existing=$(kc get clients -r "$REALM_NAME" -q clientId="$client_id" --fields id 2>/dev/null \
    | grep -o '"[a-f0-9-]*"' | tr -d '"' || true)

  if [ -n "$existing" ]; then
    echo "[07] Client '$client_id' already exists in '$REALM_NAME' (id=$existing)."
    client_uuid="$existing"
  else
    echo "[07] Creating client '$client_id' for journal '$journal' ..."
    kc create clients -r "$REALM_NAME" \
      -s clientId="$client_id" \
      -s publicClient=false \
      -s serviceAccountsEnabled=true \
      -s standardFlowEnabled=false \
      -s directAccessGrantsEnabled=true \
      -s secret="$secret" \
      -s "attributes={\"journal_codes\":\"$journal\"}"
    client_uuid=$(kc get clients -r "$REALM_NAME" -q clientId="$client_id" --fields id 2>/dev/null \
      | grep -o '"[a-f0-9-]*"' | tr -d '"')
  fi

  kc update "clients/$client_uuid/default-client-scopes/$SCOPE_ID" -r "$REALM_NAME" 2>/dev/null || true
  echo "[07]   clientId='$client_id' secret='$secret' journal_codes=['$journal']"
done

echo "[07] Done. Journals: ${JOURNALS[*]}"
