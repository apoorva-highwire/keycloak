import { CheckCircle2, XCircle } from "lucide-react";
import { getJournalsOverview } from "@/lib/iam";
import { getCallerJournal, readSession } from "@/lib/session";
import { Card, PageHeader, Pill } from "@/components/ui";
import { ManageJournalsButton } from "@/components/ManageJournalsButton";

export const dynamic = "force-dynamic";

function StepRow({ ok, label, detail }: { ok: boolean; label: string; detail: string }) {
  return (
    <div className="flex items-center gap-3 py-2.5 border-b border-border last:border-0">
      {ok ? <CheckCircle2 size={16} className="text-good shrink-0" /> : <XCircle size={16} className="text-fg-muted/40 shrink-0" />}
      <div className="min-w-0">
        <p className="text-sm text-fg">{label}</p>
        <p className="text-xs text-fg-muted tabular truncate">{detail}</p>
      </div>
    </div>
  );
}

export default async function JournalsPage() {
  const session = await readSession();
  const callerJournal = session ? getCallerJournal(session) : null;

  const allJournals = await getJournalsOverview();
  // Display-only filter — see getCallerJournal's own comment on why this
  // isn't a real security boundary, just what this console chooses to show.
  const journals = callerJournal ? allJournals.filter((j) => j.code === callerJournal) : allJournals;

  return (
    <div className="max-w-5xl px-10 py-10">
      <PageHeader
        title="Journals"
        description="Each journal is one Authorization Services resource, two per-journal roles, two role-policies, and two scope-permissions — created by scripts/<realm>/06-09, read live here."
        action={callerJournal ? undefined : <ManageJournalsButton />}
      />

      <div className="grid md:grid-cols-2 gap-5">
        {journals.map((j) => (
          <Card key={j.code} className="p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-display text-lg text-fg">{j.code}</h3>
              {j.configured ? (
                <Pill tone="good">Configured</Pill>
              ) : j.resource ? (
                <Pill tone="warn">Incomplete</Pill>
              ) : (
                <Pill tone="neutral">Pending setup</Pill>
              )}
            </div>

            <StepRow
              ok={!!j.resource}
              label="Resource"
              detail={j.resource ? `${j.code}-articles — scopes: ${j.resource.scopes.join(", ")}` : `${j.code}-articles (missing)`}
            />
            <StepRow
              ok={!!j.readPolicy}
              label="Read policy"
              detail={j.readPolicy ? `${j.readPolicy.name} — role: ${j.readPolicy.roleNames.join(", ")}` : `${j.code}-readers (missing)`}
            />
            <StepRow
              ok={!!j.writePolicy}
              label="Write policy"
              detail={j.writePolicy ? `${j.writePolicy.name} — role: ${j.writePolicy.roleNames.join(", ")}` : `${j.code}-writers (missing)`}
            />
            <StepRow
              ok={!!j.readPermission}
              label="Read permission"
              detail={j.readPermission ? `${j.code}-articles#read → ${j.readPermission.policies.join(", ")}` : "missing"}
            />
            <StepRow
              ok={!!j.writePermission}
              label="Write permission"
              detail={j.writePermission ? `${j.code}-articles#write → ${j.writePermission.policies.join(", ")}` : "missing"}
            />
          </Card>
        ))}
      </div>
    </div>
  );
}
