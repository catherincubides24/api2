# Build stage
FROM node:20-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ARG VITE_INACTIVITY_TIMEOUT_MS=300000
ARG VITE_INACTIVITY_WARNING_MS=150000
ENV VITE_INACTIVITY_TIMEOUT_MS=$VITE_INACTIVITY_TIMEOUT_MS
ENV VITE_INACTIVITY_WARNING_MS=$VITE_INACTIVITY_WARNING_MS
ARG VITE_GOOGLE_CLIENT_ID
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID
RUN npm run build


# Runtime stage
FROM nginx:1.27-alpine
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]