# SHOGUN SAKURA

押し花白扇販売デモのポートフォリオサイトです。実販売や決済は行わず、購入デモの入力内容をフロントエンドから Spring Boot API に送信して、Render PostgreSQL の `orders` テーブルへ保存します。

## 構成

- `frontend/`: GitHub Pages 用の静的サイト
- `backend/`: Java 21 / Spring Boot / Maven の API
- `docs/`: 参考資料置き場

## フロントエンド起動

`frontend/index.html` を Live Server などで開きます。

API の接続先は `frontend/index.html` 内の `window.API_BASE_URL` で切り替えられます。

```html
<script>
  window.API_BASE_URL = "http://localhost:8080";
</script>
```

GitHub Pages へ公開する場合は、ここを Render Web Service の URL に変更してください。

## バックエンド起動

```powershell
cd backend
mvn clean package
mvn spring-boot:run
```

### 環境変数

- `DATABASE_URL`: JDBC 形式の PostgreSQL URL
- `DATABASE_USERNAME`: DB ユーザー名
- `DATABASE_PASSWORD`: DB パスワード
- `ALLOWED_ORIGINS`: CORS 許可 URL のカンマ区切り
- `PORT`: 起動ポート。未指定時は `8080`

Render の External Database URL が `postgresql://...` 形式なら、`DATABASE_URL` には `jdbc:postgresql://...` 形式を設定します。

## API

- `GET /api/health`
- `POST /api/orders`

### 正常時のリクエスト例

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

### curl.exe での確認

PowerShell では `curl` ではなく `curl.exe` を使います。

```powershell
curl.exe -i http://localhost:8080/api/health
```

```powershell
curl.exe -i -X POST "http://localhost:8080/api/orders" `
  -H "Content-Type: application/json" `
  -d "{\"name\":\"山田 太郎\",\"email\":\"test@example.com\",\"postalCode\":\"100-0001\",\"address\":\"東京都千代田区1-1-1\",\"quantity\":1,\"note\":\"デモ注文です\"}"
```

## DB 確認

```sql
SELECT id, product_code, customer_name, email, quantity, total_amount, created_at
FROM orders
ORDER BY id DESC;
```

## 注意事項

- 実販売、決済、配送、在庫管理、会員登録、ログイン、問い合わせフォームは実装しません。
- DB パスワードや Render の接続情報は GitHub にコミットしません。
- フロントエンドから PostgreSQL へ直接接続しません。

