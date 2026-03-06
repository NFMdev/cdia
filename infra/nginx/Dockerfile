FROM node:22-alpine AS web-builder
WORKDIR /src/web

COPY web/package*.json ./
RUN npm install

COPY web/ ./
ARG VITE_API_BASE_URL=/api
ARG VITE_USE_MOCK_API=false
ARG VITE_ENABLE_HEATMAP=true
ARG VITE_FILTER_BY_BBOX=false
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_USE_MOCK_API=$VITE_USE_MOCK_API
ENV VITE_ENABLE_HEATMAP=$VITE_ENABLE_HEATMAP
ENV VITE_FILTER_BY_BBOX=$VITE_FILTER_BY_BBOX
RUN npm run build

FROM nginx:1.27-alpine AS runtime
COPY infra/nginx/nginx.conf /etc/nginx/nginx.conf
COPY infra/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf
COPY infra/nginx/conf.d/observability.locations.conf /etc/nginx/snippets/observability.locations.conf
COPY --from=web-builder /src/web/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
