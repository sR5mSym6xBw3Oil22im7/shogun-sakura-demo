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
├── backend/               # Spring Boot バックエンド
├── docs/                  # 追加の静的ページ出力
├── render.yaml            # Render サービス定義
└── index.html             # ルート用のエントリページ
```

## README 更新ルール

- README は日本語で記述します。
- 既存内容を保持し、必要な箇所だけを差分編集します。
- 秘密情報は記載しません。

## 前提条件

Xubuntu 上での開発・実行に必要な基本要件です。

- Bash
- Git
- Docker Engine
- Docker Compose v2
- 手動起動時のみ Java 21
- Maven Wrapper を使用するため、ホストへの Maven インストールは原則不要
- Selenium テスト実行時のみ Google Chrome または Chromium

Docker を Xubuntu へ導入する例です。

```bash
sudo apt update
sudo apt install -y git curl ca-certificates docker.io docker-compose-v2 openjdk-21-jdk
sudo systemctl enable --now docker
```

非 root で Docker を利用する場合は、次を実行してください。設定は再ログイン後に有効になります。

```bash
sudo usermod -aG docker "$USER"
```

動作確認コマンドは次の通りです。

```bash
bash --version
git --version
docker --version
docker compose version
java -version
```

Selenium テストを実行する場合は、ブラウザが利用可能であることを確認してください。

```bash
google-chrome --version
# または
chromium --version
```

## Docker Compose で起動する

リポジトリルートで次を実行します。PostgreSQL、Spring Boot API、フロントエンド配信サーバーがまとめて起動します。

```bash
cp .env.example .env
# 必要な場合のみ .env の GEMINI_API_KEY 等を編集する
docker compose up --build
```

停止するには次を実行します。

```bash
docker compose down
```

PostgreSQL のローカルデータも削除する場合は、次を実行してください。

```bash
docker compose down -v
```

## 手動で起動する

バックエンドは `backend` ディレクトリで Maven Wrapper を使って起動します。

```bash
cd backend
./mvnw clean test
./mvnw spring-boot:run
```

フロントエンドは、リポジトリルートで Java 21 付属の静的サーバーを利用できます。

```bash
jwebserver -p 5500
```

## Selenium テスト

Chrome または Chromium が必要です。Selenium Manager がドライバを取得または検出できるネットワーク・実行環境が必要です。ブラウザ実行ファイルを自動検出できない場合は `CHROME_BIN` を設定できます。

```bash
export CHROME_BIN="$(command -v google-chrome || command -v chromium)"
cd backend
./mvnw test
```

Docker Compose 連携テストを実行する場合は、先に `docker compose up --build -d` を実行してください。

## クリーンアップ

生成物を削除するには次を実行します。

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
