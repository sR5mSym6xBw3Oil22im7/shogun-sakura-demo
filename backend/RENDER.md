# Render Deployment

このバックエンドは Render Web Service で `Docker` としてデプロイします。

## 設定

- Language: `Docker`
- Dockerfile Path: `backend/Dockerfile`
- Health Check Path: `/api/health`
- Build / Start: Dockerfile に委任

## 環境変数

- `PORT`
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `ALLOWED_ORIGINS`

`ALLOWED_ORIGINS` には次を含めます。

- `https://sr5msym6xbw3oil22im7.github.io`
- `http://localhost:5500`
- `http://127.0.0.1:5500`

## Database URL

Render の External Database URL が `postgres://...` または `postgresql://...` の場合でも、アプリ側で `jdbc:postgresql://...` に変換します。

