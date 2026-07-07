# SHOGUN SAKURA

桜の押し花と白扇「将軍」を紹介する、職業訓練校卒業制作のポートフォリオ用デモサイトです。

## 公開構成

- フロントエンド: GitHub Pages
- バックエンド: Render Web Service + PostgreSQL
- デモURL: `https://sr5msym6xbw3oil22im7.github.io/shogun-sakura-demo/frontend/`

## 仕様

- デザインは既存のギャラリー調レイアウトを維持します
- 実販売、決済、配送、在庫管理は行いません
- フロントの注文フォームから送信された内容をバックエンド API 経由で PostgreSQL に保存します

## ディレクトリ

- `frontend/`: GitHub Pages で公開する静的サイト
- `backend/`: Java 21 / Spring Boot / Maven の API
- `docs/`: 補助資料

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

### `POST /api/orders`

```json
{
  "name": "Taro Yamada",
  "email": "test@example.com",
  "postalCode": "100-0001",
  "address": "Tokyo 1-1-1",
  "quantity": 1,
  "note": "Browser order demo"
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

## GitHub Pages と Render の接続

フロントは、ローカルでは `http://localhost:8080` を使い、本番では `https://shogun-sakura-demo-backend.onrender.com` を使うようにしています。

Render 側の CORS 許可元には次を設定します。

- `https://sr5msym6xbw3oil22im7.github.io`
- `http://localhost:5500`
- `http://127.0.0.1:5500`

## curl.exe での確認

PowerShell では `curl` ではなく `curl.exe` を使います。

```powershell
curl.exe -i http://localhost:8080/api/health
```

```powershell
curl.exe -i -X POST "http://localhost:8080/api/orders" `
  -H "Content-Type: application/json" `
  -d "{\"name\":\"Taro Yamada\",\"email\":\"test@example.com\",\"postalCode\":\"100-0001\",\"address\":\"Tokyo 1-1-1\",\"quantity\":1,\"note\":\"Browser order demo\"}"
```

## Render

- Language: `Docker`
- Dockerfile: `backend/Dockerfile`
- `.dockerignore`: `backend/.dockerignore`
- Health check path: `/api/health`

## DB 確認例

```sql
SELECT id, product_code, customer_name, email, quantity, total_amount, created_at
FROM orders
ORDER BY id DESC;
```

