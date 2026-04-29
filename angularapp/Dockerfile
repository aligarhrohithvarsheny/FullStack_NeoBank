FROM node:20-alpine
WORKDIR /app

# Install production dependencies (express etc.)
COPY package.json package-lock.json* ./
RUN npm ci --omit=dev || npm install --production

# Copy pre-built Angular SSR distribution
COPY dist/angularapp ./dist/angularapp

EXPOSE 4000
ENV PORT=4000
ENV NODE_ENV=production
CMD ["node", "dist/angularapp/server/server.mjs"]
