import { BookOpenText } from "lucide-react";
import { PUBLISHER_DISPLAY_NAME, REALM } from "@/lib/config";

const ERROR_MESSAGES: Record<string, string> = {
  state: "That login attempt couldn't be verified — please try again.",
  exchange: "Keycloak didn't accept that login — please try again.",
};

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ error?: string }> }) {
  const { error } = await searchParams;

  return (
    <div className="min-h-screen flex items-center justify-center bg-bg px-4">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center text-center mb-8">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-accent-soft text-accent mb-4">
            <BookOpenText size={22} strokeWidth={2} />
          </div>
          <h1 className="font-display text-xl text-fg">{PUBLISHER_DISPLAY_NAME}</h1>
          <p className="mt-1 text-sm text-fg-muted">
            Publisher Console — realm <span className="tabular">{REALM}</span>
          </p>
        </div>

        <div className="rounded-xl border border-border bg-surface p-6 shadow-sm">
          {error && (
            <div className="mb-4 rounded-lg bg-bad-soft px-3.5 py-2.5 text-sm text-bad">
              {ERROR_MESSAGES[error] ?? "Something went wrong signing you in."}
            </div>
          )}
          <a
            href="/api/auth/login"
            className="flex w-full items-center justify-center rounded-lg bg-accent px-4 py-2.5 text-sm font-medium text-accent-fg hover:bg-accent-strong transition-colors"
          >
            Sign in with Keycloak
          </a>
          <p className="mt-4 text-center text-xs text-fg-muted">
            Redirects to this realm's own login — your publisher-admin (or any realm user) credentials.
          </p>
        </div>
      </div>
    </div>
  );
}
