# SHOGUN SAKURA

SHOGUN SAKURA は、ポートフォリオ向けのデモ EC サイトです。画面は静的 HTML/CSS/JavaScript で構成され、Spring Boot バックエンドが画面と REST API（注文受付、注文履歴、AI チャット）を同一オリジンで配信します。

以前は GitHub Pages で画面を配信していましたが、AI チャットの応答性を改善するため、画面をバックエンドへ移行しました。旧 URL（GitHub Pages の `frontend/`）にアクセスすると、バックエンドのトップページへ自動で遷移します。

このリポジトリでは、商品紹介、注文確認、注文完了、注文履歴の各画面を提供しています。システム構成図ページは現在含まれていません。

## 公開 URL

- サイト（画面・API）: `https://shogun-sakura-demo.onrender.com/`
- 旧フロントエンド URL: `https://sr5msym6xbw3oil22im7.github.io/shogun-sakura-demo/frontend/`（上記サイトへ自動遷移）
- ヘルスチェック: `https://shogun-sakura-demo.onrender.com/api/health`

## 主な機能

- 商品紹介と注文のデモフロー
- 注文フォームからバックエンド API へのデモ注文送信
- 注文履歴画面で保存済み注文の一覧表示
- サイト本文に基づく AI チャット回答
- Spring Boot による REST API と入力バリデーション
- Render Web Service 内の H2 インメモリ DB への注文データ保存
- 画面と API の同一オリジン配信（`file://` でのローカル確認向けに CORS 設定を維持）

## 画面一覧

画面ファイルは `backend/src/main/resources/static/` に配置しています。

- `/`（`index.html`）: 商品紹介と注文入力
- `/orderConfirmation.html`: 注文内容の確認
- `/orderConfirmed.html`: 注文完了
- `/orderhistory.html`: 注文履歴一覧

## ディレクトリ構成

```text
.
├── backend/               # Spring Boot バックエンド（画面は src/main/resources/static/）
├── frontend/              # 旧 URL 用の遷移ページ（index.html のみ）
├── doc/                   # 開発文書
├── render.yaml            # Render 用サービス定義
└── index.html             # ルート用エントリページ（バックエンドへ遷移）
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

リポジトリルートで次を実行します。Spring Boot が画面と API をまとめて配信します。起動後、`http://localhost:8080/` を開いてください。

```bash
cp .env.example .env
# .env では GEMINI_API_KEY, MCP_INTERNAL_TOKEN, MCP_BASE_URL を必ず設定してください
docker compose up --build
```

停止するには次を実行します。

```bash
docker compose down
```

H2 の注文データはプロセス終了で消えます。保存データの退避は不要で、停止だけで十分です。

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

起動後、ブラウザで `http://localhost:8080/` を開くと画面が表示されます。画面は API を同一オリジン（`location.origin`）で呼び出します。

- 画面ファイル（`backend/src/main/resources/static/*.html`）を `file://` で直接開いた場合は、`http://localhost:8080` の API を呼び出します。
- 別ポートで起動した場合は、そのポート（例: `http://localhost:8081/`）を開けば画面と API が連携します。

## Selenium テスト

Chrome または Chromium が必要です。Selenium Manager がドライバを取得できる環境で実行してください。

```bash
export CHROME_BIN="$(command -v google-chrome || command -v chromium)"
cd backend
./mvnw test
```

Docker Compose 連携テスト（`ComposeChatbotFlowTest`）は `http://localhost:8080/` に到達できる場合のみ実行されます。実行する場合は、先に:

```bash
docker compose up --build -d
```

`orderSubmissionUsesLocalApiWhenPageIsOpenedFromFileUrl` も `localhost:8080` で起動中のバックエンドを必要とします。

画面設定の Node テスト:

```bash
node --test backend/src/test/js/localBackendConfig.test.js
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
  "createdAt": "2026-07-26T12:00:00+09:00"
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
    "createdAt": "2026-07-26T12:00:00+09:00"
  }
]
```

### `POST /api/chat`

AI チャット回答を返します。画面はこの API のみを呼び出します。

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

`/mcp` はバックエンド内部で AI チャット用のサイト本文取得に使う内部エンドポイントです。画面から直接呼び出しません。サイト本文は既定で自プロセスが配信する画面（`http://127.0.0.1:${server.port}/`）から取得し、`SITE_SOURCE_BASE_URL` で上書きできます。

## ローカル開発の補足

- 通常は `http://localhost:8080/` から画面を開いてください（同一オリジンのため CORS 設定は不要です）。
- `file://` で直接ファイルを開くとブラウザの Origin は `null` になります。公開環境では `null` を許可しないでください。
- `file://` でテストする場合もバックエンドが先に起動している必要があります。
- Render の `ALLOWED_ORIGINS` は `https://shogun-sakura-demo.onrender.com` を設定します。
