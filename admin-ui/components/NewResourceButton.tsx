"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Plus, X } from "lucide-react";
import { Banner, Button, Field, inputClass, Modal } from "@/components/ui";

const DEFAULT_SCOPES = ["read", "write"];

export function NewResourceButton() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [scopes, setScopes] = useState<string[]>(DEFAULT_SCOPES);
  const [newScope, setNewScope] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function addScope() {
    const s = newScope.trim();
    if (s && !scopes.includes(s)) {
      setScopes([...scopes, s]);
    }
    setNewScope("");
  }

  function removeScope(s: string) {
    setScopes(scopes.filter((x) => x !== s));
  }

  function reset() {
    setName("");
    setScopes(DEFAULT_SCOPES);
    setNewScope("");
    setError(null);
  }

  async function submit() {
    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch("/api/resources", {
        method: "POST",
        body: JSON.stringify({ name, scopes }),
      });
      if (!res.ok) {
        const body = await res.json();
        throw new Error(body.message ?? "Could not create resource");
      }
      setOpen(false);
      reset();
      router.refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unknown error");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <Button icon={Plus} onClick={() => setOpen(true)}>
        New resource
      </Button>

      <Modal
        open={open}
        onClose={() => {
          setOpen(false);
          reset();
        }}
        title="New resource"
      >
        {error && <Banner tone="bad">{error}</Banner>}
        <Field label="Name">
          <input className={inputClass} value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. nejm-editorials" />
        </Field>
        <Field label="Scopes">
          <div className="flex flex-wrap gap-1.5 mb-2">
            {scopes.map((s) => (
              <span key={s} className="inline-flex items-center gap-1 rounded-full bg-accent-soft text-accent pl-2.5 pr-1.5 py-0.5 text-xs font-medium">
                {s}
                <button onClick={() => removeScope(s)} className="rounded-full p-0.5 hover:bg-accent/15">
                  <X size={11} />
                </button>
              </span>
            ))}
          </div>
          <div className="flex gap-2">
            <input
              className={inputClass}
              value={newScope}
              onChange={(e) => setNewScope(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  addScope();
                }
              }}
              placeholder="e.g. download"
            />
            <Button variant="secondary" onClick={addScope} disabled={!newScope.trim()}>
              Add
            </Button>
          </div>
        </Field>
        <div className="flex justify-end gap-2 mt-5">
          <Button
            variant="secondary"
            onClick={() => {
              setOpen(false);
              reset();
            }}
          >
            Cancel
          </Button>
          <Button onClick={submit} disabled={!name || scopes.length === 0 || submitting}>
            {submitting ? "Creating…" : "Create resource"}
          </Button>
        </div>
      </Modal>
    </>
  );
}
