# Role taxonomy

Keycloak realm roles only — this is the authentication-side role set each
realm's tokens carry. It is deliberately small and stable; anything more
granular (per-journal permissions, resource-level grants) lives in the IAM
service's own Postgres schema instead (MULTI-TENANT-KEYCLOAK-IAM-PLAN.md
§3c's `roles`/`grants`/`obligations` tables), not as more Keycloak roles —
adding a new realm role for every fine-grained permission doesn't scale
across N publisher realms, and the IAM service is already the place that
aggregates across realms.

## highwire-admin (staff only — see realms/highwire-admin.json)

| Role | Scope |
|---|---|
| `super-admin` | Full cross-realm access via the IAM service's `iam-service` client. |
| `support-staff` | Read/limited-write on publisher accounts for support tickets. No billing, no realm config. |
| `billing-admin` | Manage subscriptions/entitlements across publishers. No user-data access. |

## highwire-iam (service accounts only — see realms/highwire-iam.json)

| Role | Scope |
|---|---|
| `cross-realm-admin` | Held by the `iam-service` client's own service account. Actual realm-admin delegation into each publisher realm is configured per-realm (a separate client/role grant in that realm), not stored centrally — see the plan doc §2 for why. |

## Every publisher realm (see realms/publisher-template.json)

| Role | Scope |
|---|---|
| `reader` | Browse/read content the journal has made available. |
| `author` | Submit and manage their own article submissions. |
| `reviewer` | View and respond to submissions assigned to them. |
| `editor` | Manage the full editorial workflow for this journal. |
| `publisher-admin` | Full admin rights within this one realm only — never crosses into another publisher's realm or into `highwire-admin`. |

Same five roles for every publisher, on purpose: a service's authorization
middleware can check `realm_access.roles` the same way regardless of
which publisher realm issued the token, instead of each realm inventing
its own role names.
