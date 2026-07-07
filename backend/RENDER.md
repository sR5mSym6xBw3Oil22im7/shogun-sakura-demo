# Render Deployment

このバックエンドは Render 上で動かす前提で以下を使います。

## Web Service

- Build Command: `mvn clean package -DskipTests`
- Start Command: `java -jar target/shogun-sakura-demo-backend-0.0.1-SNAPSHOT.jar`
- Health Check Path: `/api/health`
- Root Directory: `backend`

## Environment Variables

- `PORT`: Render が割り当てるポート
- `DATABASE_URL`: Render PostgreSQL の接続文字列
- `DATABASE_USERNAME`: 任意
- `DATABASE_PASSWORD`: 任意
- `ALLOWED_ORIGINS`: GitHub Pages の公開 URL

`DATABASE_URL` が `postgres://...` または `postgresql://...` の場合でも、アプリ側で `jdbc:postgresql://...` に自動変換します。

## CORS

`ALLOWED_ORIGINS` に GitHub Pages の URL をカンマ区切りで設定してください。

例:

```text
https://your-account.github.io
https://your-account.github.io/your-repo
```
