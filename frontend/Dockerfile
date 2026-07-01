FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./

RUN npm ci

COPY . .

RUN npm run build
RUN mkdir -p /app/nginx-html && cp -r "$(dirname "$(find dist -type f -name index.html | head -n 1)")"/* /app/nginx-html/

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/nginx-html/ /usr/share/nginx/html/

EXPOSE 80
