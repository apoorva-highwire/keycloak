// Server-only configuration — never imported from a 'use client' file.
// Matches the local-dev values already used throughout hosting-new/keycloak's
// own scripts/compose files; a real deployment injects these from a
// secrets store instead of .env.local.

// Server-to-server address (token exchange in the callback route,
// refresh in middleware.ts) — safe to be the internal docker-network
// hostname once this runs in a container, since nothing but this
// server ever calls it.
export const KEYCLOAK_URL = process.env.KEYCLOAK_URL ?? "http://localhost:8081";
// The address the USER'S OWN BROWSER is redirected to (login kickoff,
// logout) — must stay externally reachable even when KEYCLOAK_URL
// becomes an internal docker hostname the browser could never resolve.
// Defaults to KEYCLOAK_URL so the host-run dev case (where they're the
// same address) needs no extra config.
export const KEYCLOAK_PUBLIC_URL = process.env.KEYCLOAK_PUBLIC_URL ?? KEYCLOAK_URL;
export const IAM_SERVICE_URL = process.env.IAM_SERVICE_URL ?? "http://localhost:8090";

// This app's OWN externally-reachable address — needed because the
// standalone Node server's req.url reports the container's own
// hostname (confirmed live: a login redirect built from it carried
// redirect_uri=http://<container-id>:3000/..., which Keycloak's
// registered redirectUris could never match), not whatever Host header
// the browser actually used. Every redirect sent to the BROWSER builds
// from this instead of req.url; middleware.ts's redirects are unaffected
// (it emits root-relative Locations) so it doesn't need this.
export const APP_PUBLIC_URL = process.env.APP_PUBLIC_URL ?? "http://localhost:3000";

// ---- Which publisher this console manages — the one thing that changes
// per deployment of this app. Point it at a different publisher by
// changing REALM/RESOURCE_SERVER_CLIENT_ID/PUBLISHER_DISPLAY_NAME in
// .env.local; nothing else in the app needs to change. No JOURNALS list
// here — lib/iam.ts's getJournalsOverview() discovers journals from
// Keycloak's own resources instead of a fixed list that could drift. ----
export const REALM = process.env.REALM ?? "nejm";
export const RESOURCE_SERVER_CLIENT_ID = process.env.RESOURCE_SERVER_CLIENT_ID ?? "nejm-journal-api";
export const PUBLISHER_DISPLAY_NAME = process.env.PUBLISHER_DISPLAY_NAME ?? "NEJM Group";

// ---- Login — Authorization Code flow against the SAME public client
// scripts/<realm>/04-create-public-client.sh already provisions
// (standardFlowEnabled=true, redirectUris includes http://localhost:*).
// No client secret: it's a public client, same one Drupal/Postman use.
// The console now calls iam-service AS the logged-in admin (PUBLISHER
// scope), not with a separate service credential — one real identity
// behind every action, not a shared one. ----
export const LOGIN_CLIENT_ID = process.env.LOGIN_CLIENT_ID ?? "drupal-client";
