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
- SHOGUN SAKURA サイト本文だけを根拠に回答するテキスト専用 AI チャット
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

Docker を使う場合:

- Docker Desktop または Docker Engine（Compose v2 対応）
- Xubuntu では `docker.io` と `docker-compose-v2` パッケージを利用できます。

手動で起動する場合:

- Java 21
- Maven
- バックエンドから接続できる PostgreSQL
- フロントエンド配信用の静的サーバー。例: VS Code Live Server、または JDK 21 付属の `jwebserver`

Xubuntu で前提条件をインストールする例です。Docker を使う場合は Java、Maven、PostgreSQL のホストインストールは不要です。

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker "$USER"
```

グループ変更を現在のターミナルへ反映するには、次を実行します。反映されない場合は一度ログアウトしてからログインし直してください。

```bash
newgrp docker
```

確認には次を実行します。

```bash
docker --version
docker compose version
```

### Docker Compose で起動する

リポジトリルートで次を実行します。PostgreSQL、Spring Boot API、フロントエンド配信サーバーがまとめて起動します。

```bash
cp .env.example .env
docker compose up --build
```

`.env` の `GEMINI_API_KEY` を設定するとチャットもローカルで利用できます。空欄のままでも注文機能とサイト表示は起動します。DB パスワードや MCP 内部トークンを変更する場合も `.env` を編集してください。

ブラウザで `http://127.0.0.1:5500/frontend/` を開きます。API のヘルスチェックは `http://127.0.0.1:8080/api/health` です。

チャットもローカルで使う場合は、起動前に Gemini API キーを環境変数へ設定してください。未設定でも注文機能とサイト表示は起動します。

```bash
export GEMINI_API_KEY="your_gemini_api_key"
docker compose up --build
```

停止するには `Ctrl+C` を押し、コンテナも削除する場合は `docker compose down` を実行します。注文データを含む PostgreSQL データも削除する場合は `docker compose down -v` を実行してください。

### 手動でバックエンドを起動する

リポジトリルートで、PostgreSQL 接続情報と MCP 内部設定を環境変数として設定します。

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/shogun_sakura"
export SPRING_DATASOURCE_USERNAME="shogun_sakura_user"
export SPRING_DATASOURCE_PASSWORD="your_local_password"
export MCP_INTERNAL_TOKEN="local-long-random-token"
export SITE_SOURCE_BASE_URL="http://127.0.0.1:5500/frontend/"
export ALLOWED_ORIGINS="http://localhost:5500,http://127.0.0.1:5500,null"
```

チャットを使う場合は `GEMINI_API_KEY` も設定してください。

テストは `backend` ディレクトリで実行します。リポジトリルートでは POM が見つからないため、必ず `cd backend` してください。

```bash
cd backend
mvn clean test
```

API を起動する場合は、テストとは別のターミナルで次を実行します。

```bash
cd backend
mvn spring-boot:run
```

デフォルトでは `http://localhost:8080` で起動します。ポートを変更する場合は `PORT` を設定してください。

### フロントエンドの起動

別のターミナルでリポジトリルートへ移動し、ルートを静的サーバーで配信します。

```bash
jwebserver -p 5500
```

ブラウザで `http://127.0.0.1:5500/frontend/` を開きます。VS Code Live Server を使う場合も、同じ URL 形式で `frontend/` を開いてください。

`frontend/index.html` を `file://` で直接開いた場合も、デフォルトでは Render 上のバックエンド API を呼び出します。ローカルのバックエンドに向けたい場合は、ローカルサーバー経由で `frontend/` を開くか、ページのスクリプト読み込み前に `window.API_BASE_URL` を定義するか、対応ページの `body` に `data-api-base-url` を設定してください。

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

### `POST /api/chat`

SHOGUN SAKURA サイト本文に基づく AI チャット回答を返します。フロントエンドはこの API だけを呼び、Gemini API や内部用 `/mcp` エンドポイントを直接呼びません。

```json
{
  "message": "セットの価格はいくらですか？"
}
```

入力ルール:

- `message`: 必須、trim 後に空でないこと、500 文字以内
- チャット内容は DB に保存しません。

成功時のレスポンス例:

```json
{
  "answer": "桜の押し花と将軍の扇セットは、税込のデモ価格で4,800円です。"
}
```

サイト本文に根拠がない場合は、次の固定文を返します。

```text
このサイト内に記載がないため、お答えできません。
```

### 内部 MCP

`/mcp` はバックエンド内部の AI チャット処理専用です。`MCP_INTERNAL_TOKEN` と Origin 検証で保護し、フロントエンドから直接呼び出しません。

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
