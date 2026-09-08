# HYEDA frontend — Vite build + nginx (Render Free Web Service)
# Build context: HYEDA/frontend
# Pass API URL at build time:
#   docker build --build-arg VITE_API_URL=https://hyeda-api.onrender.com/api -t hyeda-web .

FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_URL=http://localhost:8081/api
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

# Render Free expects the process to listen on $PORT
ENV PORT=80
EXPOSE 80

CMD ["sh", "-c", "sed -i \"s/listen 80;/listen ${PORT};/\" /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]
