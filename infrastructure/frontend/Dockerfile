# Dev image: Vite dev server. The compose file bind-mounts ./frontend over /app and re-runs
# pnpm install at start, so this build just warms the layer cache.
FROM node:22-alpine

ENV CI=true
RUN corepack enable && corepack prepare pnpm@12.3.4 --activate
WORKDIR /app

# pnpm-workspace.yaml carries the build-script approvals; .npmrc the run-check opt-outs.
COPY package.json pnpm-lock.yaml .npmrc pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
EXPOSE 5173
CMD ["pnpm", "run", "dev", "--", "--host"]
