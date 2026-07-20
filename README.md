# SHOGUN SAKURA

SHOGUN SAKURA は、ポートフォリオ向けのデモ EC サイトです。フロントエンドは GitHub Pages で公開する静的サイト、バックエンドは Render にデプロイする Spring Boot API で構成されています。注文データはデモ用レコードとして PostgreSQL に保存します。

## 公開 URL

- フロントエンド: `https://sr5msym6xbw3oil22im7.github.io/shogun-sakura-demo/frontend/`
- バックエンド: `https://shogun-sakura-demo.onrender.com/`
- ヘルスチェック: `https://shogun-sakura-demo.onrender.com/api/health`

## 主な機能

- HTML、CSS、JavaScript による静的な商品・注文フロー
- 注文確認画面からバックエンド API へデモ注文を送信
- 注文履歴画面で保存済み注文を API から取得
- Spring Boot REST API による入力バリデーション
- PostgreSQL への注文レコード保存
- GitHub Pages とローカル開発向けの CORS 設定

## ディレクトリ構成

```text
.
├── frontend/              # GitHub Pages で公開するフロントエンド
│   ├── index.html
│   ├── orderConfirmation.html
│   ├── orderConfirmed.html
│   ├── orderhistory.html
│   ├── css/
│   └── js/
├── backend/               # Spring Boot バックエンド
│   ├── Dockerfile
│   ├── RENDER.md
│   ├── pom.xml
│   └── src/
├── docs/                  # 追加の静的ページ出力
├── render.yaml            # Render サービス定義
└── index.html             # ルート用のエントリページ
```

## 技術スタック

- フロントエンド: HTML、CSS、Vanilla JavaScript
- バックエンド: Java 21、Spring Boot 3.3.5、Spring JDBC、Bean Validation
- データベース: PostgreSQL
- デプロイ: GitHub Pages、Render Docker Web Service

## ローカル開発

### 前提条件

- Java 21
- Maven
- バックエンドから接続できる PostgreSQL
- フロントエンド配信用の静的サーバー。例: VS Code Live Server、または `python3 -m http.server`

### バックエンドの起動

PostgreSQL 接続情報を環境変数として設定します。

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=shogun_sakura
export DB_USER=shogun_sakura_user
export DB_PASSWORD=your_password
export ALLOWED_ORIGINS=http://localhost:5500,http://127.0.0.1:5500
```

API を起動します。

```bash
cd backend
mvn spring-boot:run
```

デフォルトでは `http://localhost:8080` で起動します。ポートを変更する場合は `PORT` を設定してください。

### フロントエンドの起動

`frontend/` ディレクトリを静的サーバーで配信します。

```bash
cd frontend
python3 -m http.server 5500
```

ブラウザで `http://localhost:5500/` を開きます。

デフォルトでは、フロントエンドは Render 上のバックエンド API を呼び出します。ローカルのバックエンドに向けたい場合は、ページのスクリプト読み込み前に `window.API_BASE_URL` を定義するか、対応ページの `body` に `data-api-base-url` を設定してください。

```html
<script>
  window.API_BASE_URL = 'http://localhost:8080';
</script>
```

## API

### `GET /api/health`

サービスの稼働状態を返します。

```json
{
  "status": "ok"
}
```

### `POST /api/orders`

デモ注文を作成します。

```json
{
  "name": "テスト太郎",
  "email": "test@example.com",
  "postalCode": "100-0001",
  "address": "東京都千代田区1-1",
  "quantity": 2,
  "note": "demo order"
}
```

入力ルール:

- `name`: 必須、50 文字以内
- `email`: 必須、メールアドレス形式、100 文字以内
- `postalCode`: 任意、数字とハイフンのみ、8 文字以内
- `address`: 必須、200 文字以内
- `quantity`: 必須、1 から 9 の整数
- `note`: 任意、200 文字以内

成功時のレスポンス例:

```json
{
  "orderId": 1,
  "message": "注文を受け付けました。",
  "createdAt": "2026-07-21T12:00:00+09:00"
}
```

### `GET /api/orders`

注文履歴画面で表示する保存済みのデモ注文を返します。

```json
[
  {
    "orderId": 1,
    "productName": "SHOGUN SAKURA SET",
    "customerName": "テスト太郎",
    "email": "test@example.com",
    "postalCode": "100-0001",
    "address": "東京都千代田区1-1",
    "quantity": 2,
    "totalAmount": 9600,
    "note": "demo order",
    "createdAt": "2026-07-21T12:00:00+09:00"
  }
]
```

## デプロイ

### フロントエンド

フロントエンドは、このリポジトリから GitHub Pages に公開します。

### バックエンド

バックエンドは Render の Docker Web Service としてデプロイします。

- サービス名: `shogun-sakura-demo`
- Docker コンテキスト: `backend`
- Dockerfile: `backend/Dockerfile`
- ヘルスチェックパス: `/api/health`
- 自動デプロイ: `render.yaml` で有効

Render 向けの補足は `backend/RENDER.md` に記載しています。

## デモとしての注意点

- 実際の決済、配送、認証、在庫管理は実装していません。
- 送信された注文はデモ用レコードです。
- 商品情報はバックエンドで固定しており、商品名は `SHOGUN SAKURA SET`、単価は `4800` です。
