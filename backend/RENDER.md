# Render Deployment

バックエンドは Java ではなく `Docker` で Render にデプロイします。

## 設定

- Language: `Docker`
- Dockerfile Path: `backend/Dockerfile`
- Build Command: Dockerfile に委任
- Start Command: Dockerfile の `CMD` に委任
- Health Check Path: `/api/health`

## 環境変数

- `PORT`
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `ALLOWED_ORIGINS`

`DATABASE_URL` が `postgres://...` または `postgresql://...` の場合でも、アプリ側で `jdbc:postgresql://...` に変換します。
