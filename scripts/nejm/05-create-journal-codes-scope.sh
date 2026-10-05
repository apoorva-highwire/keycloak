#!/usr/bin/env bash
# One realm-level client scope carrying the highwire-journal-codes-mapper
# (providers/highwire-keycloak-mapper) — attached as a DEFAULT scope on
# every per-journal client (see 06-create-journal-client.sh) so
# journal_codes always lands on the token without a caller having to ask
# for it via scope=.
#
# multivalued=true is NOT optional — confirmed live that without it, a
# single journal code renders as a bare JSON string ("journal_codes":
# "nejm-core") instead of an array ("journal_codes":["nejm-core"]),
# which crashes CallerResolvingFilter's List<String> cast on every
# single-journal client's token, i.e. the common case. jsonType.label=
# String keeps each element a plain string rather than Keycloak trying
# to infer a type from the (comma-split) client attribute value.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")" && source common.sh

SCOPE_NAME="journal-codes"

SCOPE_ID=$(kc get client-scopes -r "$REALM_NAME" --fields id,name 2>/dev/null \
  | grep -B1 "\"$SCOPE_NAME\"" | grep -o '"[a-f0-9-]\{36\}"' | tr -d '"' || true)

if [ -z "$SCOPE_ID" ]; then
  echo "[05] Creating client scope '$SCOPE_NAME' in '$REALM_NAME' ..."
  kc create client-scopes -r "$REALM_NAME" \
    -s name="$SCOPE_NAME" \
    -s protocol=openid-connect \
    -s 'attributes={"include.in.token.scope":"true","display.on.consent.screen":"false"}'
  SCOPE_ID=$(kc get client-scopes -r "$REALM_NAME" --fields id,name 2>/dev/null \
    | grep -B1 "\"$SCOPE_NAME\"" | grep -o '"[a-f0-9-]\{36\}"' | tr -d '"')
else
  echo "[05] Client scope '$SCOPE_NAME' already exists in '$REALM_NAME' (id=$SCOPE_ID)."
fi

MAPPER_EXISTS=$(kc get "client-scopes/$SCOPE_ID/protocol-mappers/models" -r "$REALM_NAME" --fields name 2>/dev/null \
  | grep -c "\"$SCOPE_NAME\"" || true)

if [ "$MAPPER_EXISTS" -eq 0 ]; then
  echo "[05] Attaching highwire-journal-codes-mapper to '$SCOPE_NAME' ..."
  kc create "client-scopes/$SCOPE_ID/protocol-mappers/models" -r "$REALM_NAME" \
    -s name="$SCOPE_NAME" \
    -s protocol=openid-connect \
    -s protocolMapper=highwire-journal-codes-mapper \
    -s 'config={"claim.name":"journal_codes","access.token.claim":"true","id.token.claim":"true","userinfo.token.claim":"true","multivalued":"true","jsonType.label":"String"}'
else
  echo "[05] Mapper already attached to '$SCOPE_NAME'."
fi

echo "[05] Done. Any client with this scope DEFAULT-attached gets journal_codes on its token"
echo "     (from its own 'journal_codes' client attribute, or the authenticated user's, if one exists)."
