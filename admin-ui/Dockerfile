# Multi-stage: install+build in a full Node image, run the standalone
# output (next.config.ts's output: "standalone") in a minimal one —
# the standalone build already traces and copies only the node_modules
# this app's server actually needs, so the final stage doesn't npm
# install anything itself.

FROM node:22-slim AS builder
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# No real secrets needed at build time — Next.js only bakes in
# NEXT_PUBLIC_* vars at build, and this app has none; every value in
# lib/config.ts is read at request time via process.env on the server.
RUN npm run build

FROM node:22-slim
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /build/public ./public
COPY --from=builder /build/.next/standalone ./
COPY --from=builder /build/.next/static ./.next/static

EXPOSE 3000
CMD ["node", "server.js"]
