import { getJournalsOverview, listResources } from "@/lib/iam";
import { RESOURCE_SERVER_CLIENT_ID } from "@/lib/config";
import { getCallerJournal, readSession } from "@/lib/session";
import { Card, PageHeader, Pill } from "@/components/ui";
import { NewResourceButton } from "@/components/NewResourceButton";

export const dynamic = "force-dynamic";

export default async function AuthorizationPage() {
  const session = await readSession();
  const callerJournal = session ? getCallerJournal(session) : null;

  const [allJournals, resources] = await Promise.all([getJournalsOverview(), listResources()]);
  // Display-only filter — see getCallerJournal's own comment. A journal-
  // scoped login also doesn't get "New resource" or the cross-journal
  // "Other resources" table below — both are publisher-level concerns.
  const journals = callerJournal ? allJournals.filter((j) => j.code === callerJournal) : allJournals;

  const rows = journals.flatMap((j) => [
    { journal: j.code, scope: "read", resource: j.resource?.name ?? `${j.code}-articles`, policy: j.readPolicy, permission: j.readPermission },
    { journal: j.code, scope: "write", resource: j.resource?.name ?? `${j.code}-articles`, policy: j.writePolicy, permission: j.writePermission },
  ]);

  const journalResourceNames = new Set(allJournals.map((j) => j.resource?.name).filter(Boolean));
  const otherResources = resources.filter((r) => !journalResourceNames.has(r.name));

  return (
    <div className="max-w-5xl px-10 py-10">
      <PageHeader
        title="Authorization"
        description={`Keycloak Authorization Services under the resource-server client "${RESOURCE_SERVER_CLIENT_ID}" — every permission here is what a consuming service's @PreAuthorize("hasPermission(resource, scope)") actually checks at request time.`}
        action={callerJournal ? undefined : <NewResourceButton />}
      />

      <h2 className="font-display text-base text-fg mb-3">Journal resources</h2>
      <Card className="overflow-hidden mb-8">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-surface-muted text-left text-xs uppercase tracking-wide text-fg-muted">
              <th className="px-5 py-3 font-medium">Journal</th>
              <th className="px-5 py-3 font-medium">Resource#scope</th>
              <th className="px-5 py-3 font-medium">Granted via role</th>
              <th className="px-5 py-3 font-medium">Permission</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, i) => (
              <tr key={i} className="border-b border-border last:border-0">
                <td className="px-5 py-3 font-display">{row.journal}</td>
                <td className="px-5 py-3 tabular text-fg-muted">
                  {row.resource}#{row.scope}
                </td>
                <td className="px-5 py-3">
                  {row.policy ? (
                    <div className="flex flex-wrap gap-1">
                      {row.policy.roleNames.map((r) => (
                        <Pill key={r} tone="accent">
                          {r}
                        </Pill>
                      ))}
                    </div>
                  ) : (
                    <span className="text-fg-muted">not configured</span>
                  )}
                </td>
                <td className="px-5 py-3 tabular text-fg-muted">{row.permission?.name ?? "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      {!callerJournal && (
        <>
      <h2 className="font-display text-base text-fg mb-3">Other resources</h2>
      {otherResources.length === 0 ? (
        <p className="text-sm text-fg-muted mb-4">
          None yet — resources created here that aren&apos;t part of the per-journal convention show up in this
          table. Go to Roles to grant a role access to one (creates its policy/permission for you).
        </p>
      ) : (
        <Card className="overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-muted text-left text-xs uppercase tracking-wide text-fg-muted">
                <th className="px-5 py-3 font-medium">Resource</th>
                <th className="px-5 py-3 font-medium">Scopes</th>
              </tr>
            </thead>
            <tbody>
              {otherResources.map((r) => (
                <tr key={r.name} className="border-b border-border last:border-0">
                  <td className="px-5 py-3 font-display">{r.name}</td>
                  <td className="px-5 py-3">
                    <div className="flex flex-wrap gap-1">
                      {r.scopes.map((s) => (
                        <Pill key={s} tone="neutral">
                          {s}
                        </Pill>
                      ))}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
        </>
      )}

      <p className="mt-6 text-xs text-fg-muted">
        Runtime check (not run from here): <code className="tabular">grant_type=urn:ietf:params:oauth:grant-type:uma-ticket</code>,{" "}
        <code className="tabular">audience={RESOURCE_SERVER_CLIENT_ID}</code>, cached ~30s in Dragonfly per caller+resource+scope.
      </p>
    </div>
  );
}
