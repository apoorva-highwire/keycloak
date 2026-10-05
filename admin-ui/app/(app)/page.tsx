import Link from "next/link";
import { BookOpenText, CheckCircle2, KeyRound, Users, UsersRound, XCircle } from "lucide-react";
import { getJournalsOverview, listGroups, listRoles, listUsers } from "@/lib/iam";
import { getCallerJournal, readSession } from "@/lib/session";
import { Card, PageHeader, Pill, StatCard } from "@/components/ui";

export const dynamic = "force-dynamic";

export default async function DashboardPage() {
  const session = await readSession();
  const callerJournal = session ? getCallerJournal(session) : null;

  const [allJournals, roles, groups, users] = await Promise.all([
    getJournalsOverview(),
    listRoles(),
    listGroups(),
    listUsers(),
  ]);

  // Display-only filter, not a security boundary — see getCallerJournal's
  // own comment. A journal-scoped login sees just its own journal here,
  // matching what UserController already restricts it to for the users
  // count above (iam-service itself scopes that one server-side).
  const journals = callerJournal ? allJournals.filter((j) => j.code === callerJournal) : allJournals;

  const configuredCount = journals.filter((j) => j.configured).length;

  return (
    <div className="max-w-5xl px-10 py-10">
      <PageHeader
        title="Publisher overview"
        description="Everything this realm's journals, roles, groups and authorization rules look like right now — read live from Keycloak via iam-service."
      />

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-10">
        <StatCard icon={BookOpenText} label="Journals" value={journals.length} hint={`${configuredCount} fully configured`} />
        <StatCard icon={KeyRound} label="Roles" value={roles.length} />
        <StatCard icon={UsersRound} label="Groups" value={groups.length} />
        <StatCard icon={Users} label="Users" value={users.length} />
      </div>

      <div className="flex items-center justify-between mb-4">
        <h2 className="font-display text-lg text-fg">Journals</h2>
        <Link href="/journals" className="text-sm text-accent hover:underline">
          View all
        </Link>
      </div>

      <div className="grid md:grid-cols-2 gap-4">
        {journals.map((j) => (
          <Card key={j.code} className="p-5">
            <div className="flex items-center justify-between">
              <p className="font-display text-base text-fg">{j.code}</p>
              {j.configured ? (
                <Pill tone="good">
                  <CheckCircle2 size={12} className="mr-1 inline" />
                  Configured
                </Pill>
              ) : j.resource ? (
                <Pill tone="warn">
                  <XCircle size={12} className="mr-1 inline" />
                  Incomplete
                </Pill>
              ) : (
                <Pill tone="neutral">Pending setup</Pill>
              )}
            </div>
            <div className="mt-3 flex flex-wrap gap-1.5">
              <Pill tone="accent">{j.readRole}</Pill>
              <Pill tone="accent">{j.writeRole}</Pill>
            </div>
            <p className="mt-3 text-xs text-fg-muted">
              Resource: <span className="tabular">{j.resource ? `${j.code}-articles` : "not created"}</span>
            </p>
          </Card>
        ))}
      </div>
    </div>
  );
}
