# Publisher Console

A Next.js (App Router) demo UI for managing one HighWire publisher realm —
journals, roles, groups, users, and the Authorization Services
(resource/policy/permission) config behind them. Talks to `iam-service`
(not Keycloak directly) over its real REST API.

## Running

Requires the `docker/docker-compose.yml` stack (Keycloak, Postgres,
Dragonfly, iam-service) already up, and at least one realm set up via
`scripts/<realm>/run-all.sh`.

```bash
npm install
npm run dev
```

Open http://localhost:3000.

## Login / logout

Real Authorization Code flow against the realm's own public client
(`drupal-client` by default — same one Drupal/Postman use, no secret
needed). `/login` → `/api/auth/login` redirects to Keycloak →
`/api/auth/callback` exchanges the code and sets an httpOnly session
cookie → every page after that calls iam-service **as that logged-in
user**, not with a shared service credential, so PUBLISHER-scope admins
see exactly what they're allowed to (a JOURNAL-scoped caller would be
rejected by `UserController`'s group/role endpoints, same as the real
API). `middleware.ts` gates every route, refreshing an expired access
token transparently before any page or API route runs. "Log out" in the
sidebar ends the session at Keycloak too, not just locally.

## How it talks to iam-service

`lib/iam.ts` (a `server-only` module) reads the logged-in admin's access
token from the session cookie and forwards it to iam-service — it never
holds its own credential. Pages that need live interactivity (`roles`,
`groups`, `users`) are client components that call this app's own
`/api/*` routes, which proxy to iam-service server-side. Read-only pages
(`/`, `/journals`, `/authorization`) are server components calling
`lib/iam.ts` directly — one less round trip.

## Configuration

See `.env.local` (gitignored). `REALM`, `RESOURCE_SERVER_CLIENT_ID`, and
`JOURNALS` (comma-separated) select which publisher this console
manages — change all four `PUBLISHER_*`/`REALM`/`RESOURCE_SERVER_CLIENT_ID`/
`JOURNALS` values together to point it at a different one. Must match
what `scripts/<realm>/run-all.sh` actually provisioned.

## What's demo-only

- The "Authorization" page is read-only — creating new resources/policies/
  permissions is intentionally left to the setup scripts, not exposed here.
- Any realm user can log in; there's no extra "must be publisher-admin"
  gate on top of Keycloak's own login — a real deployment would check
  for the `publisher-admin` role after login, not just a valid session.
