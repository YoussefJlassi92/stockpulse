# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependency manifests first for layer-cache efficiency
COPY package.json package-lock.json ./
RUN npm ci

# Copy the rest of the source and build for production
COPY . .
RUN npm run build

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM nginx:alpine AS runtime

# Remove the default nginx config and replace with ours
RUN rm /etc/nginx/nginx.conf
COPY nginx.conf /etc/nginx/nginx.conf

# Copy the Angular browser bundle from the build stage
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
