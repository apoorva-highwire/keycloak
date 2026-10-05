import { ReactNode } from "react";
import { LucideIcon } from "lucide-react";

export function PageHeader({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 mb-8">
      <div>
        <h1 className="font-display text-2xl text-fg">{title}</h1>
        {description && <p className="mt-1.5 text-sm text-fg-muted max-w-xl">{description}</p>}
      </div>
      {action}
    </div>
  );
}

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-xl border border-border bg-surface shadow-sm ${className}`}>{children}</div>
  );
}

export function StatCard({ icon: Icon, label, value, hint }: { icon: LucideIcon; label: string; value: string | number; hint?: string }) {
  return (
    <Card className="p-5">
      <div className="flex items-center gap-3">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-accent-soft text-accent">
          <Icon size={18} strokeWidth={2} />
        </div>
        <p className="text-sm text-fg-muted">{label}</p>
      </div>
      <p className="mt-3 font-display text-3xl tabular text-fg">{value}</p>
      {hint && <p className="mt-1 text-xs text-fg-muted">{hint}</p>}
    </Card>
  );
}

type PillTone = "accent" | "good" | "bad" | "warn" | "neutral";

const PILL_TONES: Record<PillTone, string> = {
  accent: "bg-accent-soft text-accent",
  good: "bg-good-soft text-good",
  bad: "bg-bad-soft text-bad",
  warn: "bg-warn-soft text-warn",
  neutral: "bg-surface-muted text-fg-muted",
};

export function Pill({ children, tone = "neutral" }: { children: ReactNode; tone?: PillTone }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${PILL_TONES[tone]}`}>
      {children}
    </span>
  );
}

export function EmptyState({ icon: Icon, title, description }: { icon: LucideIcon; title: string; description?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 py-16 text-center">
      <div className="flex h-11 w-11 items-center justify-center rounded-full bg-surface-muted text-fg-muted">
        <Icon size={20} strokeWidth={2} />
      </div>
      <p className="text-sm font-medium text-fg">{title}</p>
      {description && <p className="text-sm text-fg-muted max-w-sm">{description}</p>}
    </div>
  );
}

export function Spinner() {
  return (
    <div className="flex items-center justify-center py-16">
      <div className="h-6 w-6 animate-spin rounded-full border-2 border-border border-t-accent" />
    </div>
  );
}

export function Button({
  children,
  onClick,
  type = "button",
  variant = "primary",
  disabled,
  icon: Icon,
}: {
  children: ReactNode;
  onClick?: () => void;
  type?: "button" | "submit";
  variant?: "primary" | "secondary" | "ghost" | "danger";
  disabled?: boolean;
  icon?: LucideIcon;
}) {
  const base = "inline-flex items-center gap-1.5 rounded-lg px-3.5 py-2 text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed";
  const variants: Record<string, string> = {
    primary: "bg-accent text-accent-fg hover:bg-accent-strong",
    secondary: "bg-surface-muted text-fg hover:bg-border border border-border",
    ghost: "text-fg-muted hover:bg-surface-muted hover:text-fg",
    danger: "bg-bad-soft text-bad hover:bg-bad hover:text-white",
  };
  return (
    <button type={type} onClick={onClick} disabled={disabled} className={`${base} ${variants[variant]}`}>
      {Icon && <Icon size={15} strokeWidth={2} />}
      {children}
    </button>
  );
}

export function Modal({ open, onClose, title, children }: { open: boolean; onClose: () => void; title: string; children: ReactNode }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4" onClick={onClose}>
      <div
        className="w-full max-w-md rounded-xl border border-border bg-surface p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="font-display text-lg text-fg mb-4">{title}</h2>
        {children}
      </div>
    </div>
  );
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block mb-3.5">
      <span className="block text-xs font-medium text-fg-muted mb-1.5">{label}</span>
      {children}
    </label>
  );
}

export const inputClass =
  "w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-fg placeholder:text-fg-muted/60 focus:outline-none focus:ring-2 focus:ring-accent/40 focus:border-accent";

export function Banner({ tone, children }: { tone: "bad" | "good"; children: ReactNode }) {
  return (
    <div
      className={`rounded-lg px-3.5 py-2.5 text-sm mb-4 ${
        tone === "bad" ? "bg-bad-soft text-bad" : "bg-good-soft text-good"
      }`}
    >
      {children}
    </div>
  );
}
