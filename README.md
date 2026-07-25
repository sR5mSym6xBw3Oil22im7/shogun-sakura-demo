# SHOGUN SAKURA

SHOGUN SAKURA は、ポートフォリオ向けのデモ EC サイトです。フロントエンドは静的 HTML/CSS/JavaScript で構成され、バックエンドは Spring Boot REST API を使って注文受付、注文履歴、AI チャット機能を提供します。

## 公開 URL

- フロントエンド: `https://sr5msym6xbw3oil22im7.github.io/shogun-sakura-demo/frontend/`
- バックエンド: `https://shogun-sakura-demo.onrender.com/`
- ヘルスチェック: `https://shogun-sakura-demo.onrender.com/api/health`

## 主な機能

- 商品紹介と注文のデモフロー
- 注文フォームからバックエンド API へのデモ注文送信
- 注文履歴画面で保存済み注文の一覧表示
- サイト本文に基づく AI チャット回答
- Spring Boot による REST API と入力バリデーション
- Render Web Service 内の H2 インメモリ DB への注文データ保存
- GitHub Pages / ローカル開発向けの CORS 設定

## ディレクトリ構成

```text
.
├── frontend/              # 静的フロントエンド
├── backend/               # Spring Boot バックエンド
├── docs/                  # 追加の静的ページ
├── render.yaml            # Render 用サービス定義
└── index.html             # ルート用エントリページ
```

## 前提条件

- Bash
- Git
- Docker Engine
- Docker Compose v2
- Java 21（手動起動時に必要）
- Maven Wrapper（`backend` で使用）
- Selenium テスト実行時は Chrome または Chromium

### Docker の例

```bash
sudo apt update
sudo apt install -y git curl ca-certificates docker.io docker-compose-v2 openjdk-21-jdk
sudo systemctl enable --now docker
```

非 root で Docker を利用する場合:

```bash
sudo usermod -aG docker "$USER"
```

### 動作確認コマンド

```bash
bash --version
git --version
docker --version
docker compose version
java -version
```

### Selenium テスト用ブラウザ確認

```bash
google-chrome --version
# または
chromium --version
```

## Docker Compose で起動する

リポジトリルートで次を実行します。Spring Boot API とフロントエンド配信サーバーがまとめて起動します。

```bash
cp .env.example .env
# .env では GEMINI_API_KEY, MCP_INTERNAL_TOKEN, MCP_BASE_URL を必ず設定してください
docker compose up --build
```

停止するには次を実行します。

```bash
docker compose down
```

H2 の注文データはプロセス終了で消えます。コンテナを停止するだけで十分です。

```bash
docker compose down
```

## 手動で起動する

### バックエンド

`backend` ディレクトリで Maven Wrapper を使って起動します。

```bash
cd backend
./mvnw clean test
./mvnw spring-boot:run
```

別ポートで起動する場合:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

`frontend/index.html` の `window.API_BASE_URL` は、ローカルで `file://` から開いた場合に `http://localhost:8080` になるように設定されています。

- バックエンドを `8080` で起動すると、`frontend/index.html` とそのまま連携します。
- それ以外のポートで起動する場合は、`frontend/index.html` の `API_BASE_URL` を変更するか、静的サーバーを使って開いてください。

### フロントエンド

Java 21 の静的サーバーを使う例:

```bash
jwebserver -p 5500
```

## Selenium テスト

Chrome または Chromium が必要です。Selenium Manager がドライバを取得できる環境で実行してください。

```bash
export CHROME_BIN="$(command -v google-chrome || command -v chromium)"
cd backend
./mvnw test
```

Docker Compose 連携テストを実行する場合は、先に:

```bash
docker compose up --build -d
```

## クリーンアップ

生成物を削除するには:

```bash
./scripts/clean-generated.sh
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

保存済みの注文一覧を返します。同一デプロイ中のみ保持され、再起動や再デプロイで消えます。

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

AI チャット回答を返します。フロントエンドはこの API のみを呼び出します。

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

根拠がない場合の固定応答:

```text
このサイト内に記載がないため、お答えできません。
```

### 内部 MCP

`/mcp` はバックエンド内部で AI チャット用のサイト本文取得に使う内部エンドポイントです。フロントエンドから直接呼び出しません。

## ローカル開発の補足

- `file://` で直接ファイルを開くとブラウザの Origin は `null` になります。公開環境では `null` を許可しないでください。
- `file://` でテストする場合もバックエンドが先に起動している必要があります。
