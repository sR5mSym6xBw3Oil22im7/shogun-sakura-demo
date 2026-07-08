# Render Deployment

このバックエンドは Render Web Service として `Docker` でデプロイします。

## 設定

- Language: `Docker`
- Dockerfile Path: `backend/Dockerfile`
- Health Check Path: `/api/health`

## 環境変数

- `PORT`
- `ORDER_STORAGE_PATH`
- `ALLOWED_ORIGINS`

`ORDER_STORAGE_PATH` は注文データを書き込むテキストファイルの保存先です。Render では例として `/tmp/shogun-sakura/orders.txt` を使います。

## 補足

PostgreSQL は使いません。注文は JSON Lines 形式のテキストファイルに追記されます。
