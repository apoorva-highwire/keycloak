import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Off so dev-mode client fetches (Roles/Groups/Users' useEffect calls)
  // match production exactly — Strict Mode's intentional double-mount
  // was firing every one of them twice, confirmed in the dev server log.
  // Trade-off: also loses Strict Mode's effect-safety double-invoke
  // check everywhere else, not just here.
  reactStrictMode: false,
  // A self-contained server bundle (its own node_modules subset, no
  // reliance on the rest of the image) — what the Dockerfile's final
  // stage actually runs via `node server.js`, not `next start`.
  output: "standalone",
};

export default nextConfig;
