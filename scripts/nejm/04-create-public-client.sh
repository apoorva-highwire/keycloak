#!/usr/bin/env bash
# Public client (no secret) for Drupal's own login redirect (SSO,
# standardFlowEnabled) AND Postman's username/password login
# (directAccessGrantsEnabled — the Resource Owner Password Credentials
# grant) — both enabled on the same client so either tool can use it
# without needing two separate client registrations.
#
# Redirect URIs cover Postman's real OAuth2 callback
# (https://oauth.pstmn.io/v1/callback, Postman's own documented
# redirect for its Authorization Code helper) plus a generic localhost
# wildcard for local Drupal dev — replace the localhost one with
# Drupal's real callback URL before this touches anything other than
# local dev; a wildcard redirect URI is fine for local iteration, not
# for anything shared.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")" && source common.sh

EXISTING=$(kc get clients -r "$REALM_NAME" -q clientId="$CLIENT_ID" --fields id 2>/dev/null \
  | grep -o '"[a-f0-9-]*"' | tr -d '"' || true)

if [ -n "$EXISTING" ]; then
  echo "[04] Client '$CLIENT_ID' already exists in '$REALM_NAME' (id=$EXISTING) — not recreating."
else
  echo "[04] Creating public client '$CLIENT_ID' in '$REALM_NAME' ..."
  kc create clients -r "$REALM_NAME" \
    -s clientId="$CLIENT_ID" \
    -s publicClient=true \
    -s standardFlowEnabled=true \
    -s directAccessGrantsEnabled=true \
    -s serviceAccountsEnabled=false \
    -s 'redirectUris=["https://oauth.pstmn.io/v1/callback","http://localhost:*"]' \
    -s 'webOrigins=["+"]'
fi

echo "[04] Done. Drupal/Postman can now authenticate against realm '$REALM_NAME' with client_id='$CLIENT_ID'"
echo "     (no client secret — public client). Username/password: use '$ADMIN_USERNAME' from step 03,"
echo "     or any other realm user. SSO/browser login uses the redirect URIs above."
