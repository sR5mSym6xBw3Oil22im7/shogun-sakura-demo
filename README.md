# SHOGUN SAKURA

和風テイストのデモ向け商品ページです。フロントエンドは GitHub Pages、バックエンドは Render の Docker Web Service で動かします。

## 構成

- フロントエンド: GitHub Pages
- バックエンド: Render Web Service + テキストファイル保存
- デモ URL: `https://sr5msym6xbw3oil22im7.github.io/shogun-sakura-demo/frontend/`

## できること

- 商品一覧を表示します
- 注文フォームから送信された内容を API で受け取ります
- 注文データは PostgreSQL ではなく、バックエンド内のテキストファイルへ追記します
- 実販売、決済、配送、在庫管理は行いません

## ディレクトリ

- `frontend/`: GitHub Pages 用の静的サイト
- `backend/`: Java 21 / Spring Boot / Maven の API
- `docs/`: 補助資料

## ローカル実行

### フロントエンド

`frontend/index.html` をブラウザで開くか、Live Server で表示します。

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

### 成功レスポンス

```json
{
  "orderId": 1001,
  "message": "注文を受け付けました",
  "createdAt": "2026-07-08T13:00:00+09:00"
}
```

### 保存ファイル

- Render: `/tmp/shogun-sakura/orders.txt`
- ローカル: `./backend/data/orders.txt`

各行は 1 件の注文を表す JSON Lines 形式です。

## Render

- Language: `Docker`
- Dockerfile: `backend/Dockerfile`
- Health check path: `/api/health`
- 保存先: `ORDER_STORAGE_PATH`

## 補足

フロントはローカルでは `http://localhost:8080`、本番では `https://shogun-sakura-demo-backend.onrender.com` を使う想定です。
