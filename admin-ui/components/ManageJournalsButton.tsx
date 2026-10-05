"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Plus, Settings2, X } from "lucide-react";
import { Banner, Button, Field, inputClass, Modal } from "@/components/ui";

/**
 * Edits the realm's own declared journal list (see iam-service's
 * RealmJournalsController) — the authoritative "which journals does
 * this publisher have", independent of whether each one's Authorization
 * Services config exists yet. Only rendered for a PUBLISHER-scoped
 * login by the page that uses this; the PUT itself is also enforced
 * server-side (iam-service 403s a JOURNAL-scoped caller), so this isn't
 * the only thing standing between a journal login and editing it.
 */
export function ManageJournalsButton() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [journals, setJournals] = useState<string[]>([]);
  const [newCode, setNewCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (open) {
      fetch("/api/journals/declared")
        .then((r) => r.json())
        .then(setJournals);
    }
  }, [open]);

  function addCode() {
    const code = newCode.trim();
    if (code && !journals.includes(code)) {
      setJournals([...journals, code]);
    }
    setNewCode("");
  }

  function removeCode(code: string) {
    setJournals(journals.filter((j) => j !== code));
  }

  async function save() {
    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch("/api/journals/declared", {
        method: "PUT",
        body: JSON.stringify({ journals }),
      });
      if (!res.ok) {
        const body = await res.json();
        throw new Error(body.message ?? "Could not save the journal list");
      }
      setOpen(false);
      router.refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unknown error");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <Button variant="secondary" icon={Settings2} onClick={() => setOpen(true)}>
        Manage journals
      </Button>

      <Modal open={open} onClose={() => setOpen(false)} title="Declared journals">
        {error && <Banner tone="bad">{error}</Banner>}
        <p className="text-sm text-fg-muted mb-3">
          This realm&apos;s own record of which journals it has — stored as a Keycloak realm attribute, independent
          of whether each one&apos;s resource/policy/permission setup is finished yet.
        </p>
        <Field label="Journal codes">
          <div className="flex flex-wrap gap-1.5 mb-2">
            {journals.map((code) => (
              <span
                key={code}
                className="inline-flex items-center gap-1 rounded-full bg-accent-soft text-accent pl-2.5 pr-1.5 py-0.5 text-xs font-medium"
              >
                {code}
                <button onClick={() => removeCode(code)} className="rounded-full p-0.5 hover:bg-accent/15">
                  <X size={11} />
                </button>
              </span>
            ))}
          </div>
          <div className="flex gap-2">
            <input
              className={inputClass}
              value={newCode}
              onChange={(e) => setNewCode(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  addCode();
                }
              }}
              placeholder="e.g. nejm-insight"
            />
            <Button variant="secondary" icon={Plus} onClick={addCode} disabled={!newCode.trim()}>
              Add
            </Button>
          </div>
        </Field>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button onClick={save} disabled={submitting}>
            {submitting ? "Saving…" : "Save"}
          </Button>
        </div>
      </Modal>
    </>
  );
}
