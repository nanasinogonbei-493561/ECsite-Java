# ECsite-Java

日本酒を題材とした EC サイトのフルスタック実装です。購入者向けの注文フローと、管理者向けの商品・注文管理機能を備えています。Spring Boot による REST API と React フロントエンドを分離し、業務システムで標準的に採用される構成・運用観点を、MVP 規模でも忠実に踏襲することを目指しています。

---

## 技術スタック

### バックエンド

| カテゴリ | 採用技術 |
|---|---|
| 言語 / ランタイム | Java 17 |
| フレームワーク | Spring Boot 3.5 |
| ビルド | Maven |
| 永続化 | Spring Data JPA / MyBatis 3.0 |
| DB (本番) | MySQL / MariaDB |
| DB (開発) | H2 (インメモリ) |
| 認証 | Spring Security 6 + JWT (jjwt 0.12) |
| パスワード | BCrypt |
| バリデーション | Jakarta Bean Validation |
| ロギング | Logback + Logstash Encoder (構造化 JSON) |
| API ドキュメント | OpenAPI (YAML) / Spring REST Docs |
| 補助 | Lombok / Spring DevTools |

### フロントエンド

| カテゴリ | 採用技術 |
|---|---|
| 言語 | TypeScript 5.8 |
| フレームワーク | React 19 |
| ビルド | Vite 7 |
| テスト | Vitest 4 + React Testing Library |
| 状態 / 認証 | カスタム hooks (`useAuth`) + localStorage の JWT 保管 |
| ロギング | バックエンドと同形式の構造化ログ ([frontend/src/lib/logging/](frontend/src/lib/logging/)) |

### インフラ / 開発環境

| カテゴリ | 採用技術 |
|---|---|
| コンテナ | Docker Compose ([docker/](docker/)) |
| データベースサービス | MariaDB / MySQL (両方をローカルで起動可能) |

---

## ディレクトリ構成

```
ECsite.java/
├─ backend/        Spring Boot アプリケーション
│  ├─ demo/        Maven プロジェクト本体 (src/main/java, resources/)
│  └─ application.yml
├─ frontend/       React + Vite アプリケーション
│  └─ src/
│     ├─ pages/    画面コンポーネント (購入者画面 / 管理画面)
│     ├─ lib/      API クライアント・認証・ロギング
│     └─ components/
├─ docker/         compose.yaml (MariaDB / MySQL)
├─ docs/           設計ドキュメント (基本設計書, API 仕様, ER, クラス図)
└─ diagram/        図表類 (RDD, シーケンス・フロー)
```

バックエンドのパッケージは `com.example.sakeec` 配下で、`controller / service / repository / entity / dto / security / config / logging / exception` に分割しています。

---

## 設計意図

### 1. 三層アーキテクチャによる責務の分離

`Controller → Service → Repository` の三層を厳守し、各層の役割を以下のように固定しています。

- **Controller**: HTTP リクエスト受け口。バリデーションとレスポンス整形のみを行い、ビジネスロジックは持たせない。
- **Service**: ドメインロジックとトランザクション境界。複数 Repository を束ねる調整役。
- **Repository**: 永続化のみを担当。Spring Data JPA を基本としつつ、複雑なクエリは MyBatis で記述する余地を確保。

エンティティを直接 API レスポンスに使わず、必ず DTO を介すことで、内部スキーマと公開仕様を切り離し、後方互換性を保ちながらの改修を可能にしています。

### 2. ステートレス JWT 認証

セッションを廃し、JWT による完全ステートレス構成を採用しました。理由は以下の通りです。

- フロントエンドとバックエンドを別オリジンで運用することを前提としているため、Cookie セッションよりトークン方式の方がデプロイ自由度が高い。
- 将来的なスケールアウト・サーバ分割時にもセッション同期の問題が発生しない。
- 認可ルールはシンプルに保ち、`/api/admin/**` のみ認証必須・購入者向けエンドポイントは公開、という二分構成にしています ([backend/demo/src/main/java/com/example/sakeec/config/SecurityConfig.java](backend/demo/src/main/java/com/example/sakeec/config/SecurityConfig.java))。

パスワードは BCrypt で保管し、`JwtAuthenticationFilter` を `UsernamePasswordAuthenticationFilter` の前段に挿入してトークン検証を一元化しています。

### 3. 購入者画面 / 管理画面の明確な分離

UX とセキュリティ要件が大きく異なるため、ルーティング・レイアウト・API ともに購入者と管理者を明確に分けています。

- 購入者導線: 年齢確認 → 商品一覧 → 商品詳細 → 注文入力 → 注文確認 → 注文完了
- 管理者導線: ログイン → 商品 CRUD / 注文一覧 / 注文詳細

管理画面は [frontend/src/components/AdminLayout.tsx](frontend/src/components/AdminLayout.tsx) で共通レイアウトを切り、購入者向け画面と視覚的にも分離しています。年齢確認はセッション内で記録し、未成年者購入を抑止する一次ゲートとして機能させています。

### 4. 全層で揃える構造化ロギング

バックエンド・フロントエンドの両方で **Logstash 形式の構造化ログ** を統一して出力しています。

- 業務的な識別子 (`orderId`, `userId`, `amount`, `event` など) をキーとして埋め込み、ログ検索・集計を容易にする。
- バックエンドは Logback + Logstash Encoder、フロントエンドは [frontend/src/lib/logging/](frontend/src/lib/logging/) で同等のスキーマを再現。
- MDC を活用し、リクエスト単位のトレース ID をログ全体に伝播。

監視・障害解析・ビジネス指標取得のいずれにも同じログ基盤を使えることを意図しています。

### 5. 型安全な API 契約

フロントエンドは [frontend/src/lib/api/](frontend/src/lib/api/) に API クライアントを集約し、エンドポイント定義 (`endpoints.ts`)、リクエスト・レスポンス型 (`types.ts`)、共通エラーハンドリング (`errorMessage.ts`) を分けて管理しています。OpenAPI YAML に記述した仕様と TypeScript 型を整合させることで、契約の食い違いを早期に検出できます。

### 6. データベース可搬性

開発時は H2 のインメモリ DB で即起動でき、本番想定の MySQL / MariaDB は Docker Compose で再現可能です。スキーマは [backend/demo/src/main/resources/schema.sql](backend/demo/src/main/resources/schema.sql) で DDL 管理しており、商品・注文・注文明細・管理者の 4 テーブル構成になっています。

---

## 起動方法

### バックエンド

```bash
cd backend/demo
./mvnw spring-boot:run
```

デフォルトで H2 上で起動します。MySQL/MariaDB を使う場合は [docker/compose.yaml](docker/compose.yaml) を起動した上で、[backend/demo/src/main/resources/application.yml](backend/demo/src/main/resources/application.yml) の接続情報を切り替えてください。

### フロントエンド

```bash
cd frontend
npm install
npm run dev
```

### データベース (任意)

```bash
cd docker
docker compose up -d
```

---

## ドキュメント

詳細な設計資料は [docs/](docs/) に集約しています。

- [基本設計書.md](docs/基本設計書.md) — 画面遷移・機能要件・MVP スコープ
- [APISpecification.md](docs/APISpecification.md) — REST API 仕様
- [RDD.md](docs/RDD.md) — 要件定義
- [ER.drawio](docs/ER.drawio) — エンティティ関連図
- [ClassDiagram.drawio](docs/ClassDiagram.drawio) — クラス図
- [Wireframe.drawio](docs/Wireframe.drawio) — 画面ワイヤーフレーム
