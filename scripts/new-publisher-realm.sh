#!/usr/bin/env bash
# Scaffold a brand-new publisher: the Keycloak realm + roles (from
# realms/publisher-template.json) AND a matching theme folder (generated
# directly, inheriting from hw-base via parent=hw-base — no reference
# theme folder to copy from; that role used to be publisher-example,
# removed as redundant once a real publisher realm (bmj) existed), in one
# step. This is the "realm provisioning automation" item flagged as
# not-yet-built in MULTI-TENANT-KEYCLOAK-IAM-PLAN.md's open items.
#
# Usage: ./scripts/new-publisher-realm.sh acme-journal "Acme Journal"
set -euo pipefail

REALM_NAME="${1:?Usage: $0 <realm-name> <display-name>}"
DISPLAY_NAME="${2:?Usage: $0 <realm-name> <display-name>}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KEYCLOAK_ROOT="$(dirname "$SCRIPT_DIR")"
THEMES_DIR="$KEYCLOAK_ROOT/themes"
REALM_DIR="$THEMES_DIR/$REALM_NAME"

if [ -d "$REALM_DIR" ]; then
  echo "themes/$REALM_NAME already exists — aborting so nothing is overwritten." >&2
  exit 1
fi

echo "Generating themes/$REALM_NAME (parent=hw-base) ..."
mkdir -p "$REALM_DIR/login/resources/css" "$REALM_DIR/login/resources/img"
mkdir -p "$REALM_DIR/account/resources/css"
mkdir -p "$REALM_DIR/email"

cat > "$REALM_DIR/login/theme.properties" <<'EOF'
parent=hw-base
import=common/keycloak

# Both files are loaded in order: the base theme's variables first, then
# this publisher's overrides on top. Keycloak resolves css/login.css from
# the parent (hw-base) automatically since this theme doesn't ship its
# own copy.
styles=css/login.css css/publisher-overrides.css
EOF

cat > "$REALM_DIR/account/theme.properties" <<'EOF'
parent=hw-base
import=common/keycloak

# Reuses the same publisher-overrides.css pattern as the login theme —
# Keycloak resolves each theme's CSS from its own resource path, so this
# account theme ships its own copy that just re-declares the same --hw-*
# values. Keep both files' values identical for one publisher.
styles=css/account.css css/publisher-overrides.css
EOF

cat > "$REALM_DIR/email/theme.properties" <<'EOF'
parent=hw-base
import=common/keycloak
locales=en

# Inherits hw-base's html/template.ftl unchanged. Only copy and edit that
# file here if this publisher's email needs a different header/logo than
# hw-base's own (see hw-base/email/html/template.ftl's header comment).
EOF

# Placeholder values = hw-base's own defaults, so a new publisher clearly
# sees "not customized yet" colors rather than an arbitrary example
# palette — change every --hw-* value below to the real brand before
# pointing traffic at this realm.
LOGIN_OVERRIDES="$REALM_DIR/login/resources/css/publisher-overrides.css"
cat > "$LOGIN_OVERRIDES" <<EOF
/*
 * $DISPLAY_NAME — redefines only the CSS custom properties declared in
 * hw-base's css/login.css. Values below are hw-base's own defaults
 * (HighWire maroon), not yet customized for this publisher — change them,
 * and drop the real logo into resources/img/logo.svg.
 *
 * Deliberately safe to leave --hw-primary unset: the reference design
 * keeps the "Sign in" button a constant dark navy across every publisher
 * regardless of that publisher's own hero color (a two-tone brand system —
 * the hero/accent color is the publisher's identity, the button color is
 * HighWire's own constant).
 */

:root {
  --hw-header-bg: #6a1130;
  --hw-hero-to: #591029;
  --hw-header-fg: #ffffff;

  --hw-link: #6a1130;

  --hw-tile-bg: #faf2f4;
  --hw-tile-border: #e9d3da;
  --hw-tile-hover: #f3e3e7;
  --hw-tile-fg: #1a2233;

  --hw-logo-url: url("../img/logo.svg");
  --hw-logo-width: 150px;
}
EOF

ACCOUNT_OVERRIDES="$REALM_DIR/account/resources/css/publisher-overrides.css"
cat > "$ACCOUNT_OVERRIDES" <<EOF
/*
 * Mirrors ../../../login/resources/css/publisher-overrides.css — keep
 * both files' values identical for this publisher. --hw-primary
 * deliberately not overridden, same reasoning as the login override.
 */

:root {
  --hw-header-bg: #6a1130;
  --hw-header-fg: #ffffff;
}
EOF

cat > "$REALM_DIR/login/resources/img/README.md" <<EOF
Drop this publisher's real logo here as \`logo.svg\` (referenced by
--hw-logo-url in ../css/publisher-overrides.css) before going live.
EOF

echo "  -> edit themes/$REALM_NAME/{login,account}/resources/css/publisher-overrides.css"
echo "     and drop the real logo into themes/$REALM_NAME/login/resources/img/logo.svg"

# Written into scripts/realms/ itself (not /tmp) — that directory is bind-
# mounted into Keycloak's own /opt/keycloak/data/import with --import-realm
# (see docker/docker-compose.yml), so every provisioned publisher becomes
# part of the auto-import set on every future container start, not just
# this one-time kcadm call.
REALM_JSON="$SCRIPT_DIR/realms/${REALM_NAME}.json"
sed -e "s/__REALM_NAME__/$REALM_NAME/g" -e "s/__DISPLAY_NAME__/$DISPLAY_NAME/g" \
  "$SCRIPT_DIR/publisher-template.json" > "$REALM_JSON"

echo "Importing Keycloak realm '$REALM_NAME' ..."
"$SCRIPT_DIR/import-realm.sh" "$REALM_JSON"

echo ""
echo "Done. Next: restart Keycloak (docker compose restart keycloak) so the"
echo "new theme folder is picked up, then customize themes/$REALM_NAME before"
echo "pointing real traffic at the '$REALM_NAME' realm."
