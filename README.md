# SHOGUN SAKURA

桜の押し花と白扇「将軍」を紹介する、職業訓練校卒業制作のポートフォリオ用デモサイトです。

## 概要

- `frontend/` は GitHub Pages で公開する静的サイトです。
- `backend/` は Java 21 / Spring Boot / Maven で作る注文受付 API です。
- 実販売、決済、配送、在庫管理は行いません。
- フロントの購入フォームから送信した内容は、API 経由で Render PostgreSQL の `orders` テーブルに保存します。

## 構成

- `frontend/`: HTML / CSS / JavaScript
- `backend/`: Spring Boot API
- `docs/`: 参考資料

## ローカル起動

### フロントエンド

`frontend/index.html` をブラウザまたは Live Server で開きます。

### バックエンド

```powershell
cd backend
mvn clean package
mvn spring-boot:run
```

## API

- `GET /api/health`
- `POST /api/orders`

### POST `/api/orders`

Content-Type は `application/json` です。

```json
{
  "name": "山田 太郎",
  "email": "test@example.com",
  "postalCode": "100-0001",
  "address": "東京都千代田区1-1-1",
  "quantity": 1,
  "note": "デモ注文です"
}
```

成功時のレスポンス例:

```json
{
  "orderId": 1001,
  "message": "注文を受け付けました。",
  "createdAt": "2026-07-08T13:00:00+09:00"
}
```

失敗時のレスポンス例:

```json
{
  "message": "入力内容を確認してください。",
  "fieldErrors": {
    "email": "正しいメールアドレス形式で入力してください。",
    "quantity": "数量は1から9の範囲で入力してください。"
  }
}
```

## curl.exe での確認

PowerShell では `curl` ではなく `curl.exe` を使います。

```powershell
curl.exe -i http://localhost:8080/api/health
```

```powershell
curl.exe -i -X POST "http://localhost:8080/api/orders" `
  -H "Content-Type: application/json" `
  -d "{\"name\":\"山田 太郎\",\"email\":\"test@example.com\",\"postalCode\":\"100-0001\",\"address\":\"東京都千代田区1-1-1\",\"quantity\":1,\"note\":\"デモ注文です\"}"
```

## 環境変数

- `DATABASE_URL`: PostgreSQL の JDBC URL
- `DATABASE_USERNAME`: DB ユーザー名
- `DATABASE_PASSWORD`: DB パスワード
- `ALLOWED_ORIGINS`: CORS 許可オリジン
- `PORT`: 起動ポート。未指定時は `8080`

Render の External Database URL が `postgres://...` または `postgresql://...` の場合でも、`backend/src/main/java/com/shogunsakura/demo/config/RenderDatabaseUrlEnvironmentPostProcessor.java` が `jdbc:postgresql://...` に変換します。

## Docker / Render

Render Web Service では `Language: Docker` を選び、`backend/Dockerfile` を使います。

- Dockerfile: `backend/Dockerfile`
- `.dockerignore`: `backend/.dockerignore`
- Build: Dockerfile に委任
- Start: Dockerfile の `CMD` に委任

`render.yaml` を使う場合も、Docker 前提の設定にしてください。

## DB 確認例

```sql
SELECT id, product_code, customer_name, email, quantity, total_amount, created_at
FROM orders
ORDER BY id DESC;
```

## 注意

- このサイトはポートフォリオ用デモです。
- 秘密情報を GitHub にコミットしないでください。

