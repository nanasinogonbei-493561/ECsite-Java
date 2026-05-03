/**
 * 構造化ログのスキーマ。
 *
 * 検索しやすさを最優先しているため、フィールド名は固定し、ad-hoc な追加情報は
 * すべて `context` フィールドに入れる。`event` は列挙化された定数 (LogEvent) を使う。
 */

export type LogLevel = "debug" | "info" | "warn" | "error" | "fatal";

/**
 * 列挙された LogEvent。検索クエリで `event:API_ERROR` のように厳密一致で
 * 引けるようにする。新しいイベントを追加するときは必ずこの型に追記する。
 */
export type LogEvent =
  // API
  | "API_REQUEST"
  | "API_RESPONSE"
  | "API_ERROR"           // status >= 400 のレスポンス
  | "NETWORK_ERROR"       // fetch そのものが失敗 (DNS, CORS, offline)
  // グローバルエラー
  | "UNCAUGHT_ERROR"      // window.onerror
  | "UNHANDLED_REJECTION" // unhandledrejection
  | "REACT_BOUNDARY_CAUGHT"
  | "REACT_RECOVERABLE_ERROR"
  // ユーザ操作 / ライフサイクル
  | "USER_ACTION"
  | "PAGE_VIEW"
  | "APP_BOOT"
  // 顧客フロー (年齢確認 / 商品 / 注文)
  | "AGE_VERIFICATION_SUCCEEDED"
  | "AGE_VERIFICATION_FAILED"
  | "PRODUCTS_LOADED"
  | "PRODUCT_VIEWED"
  | "ORDER_INPUT_SUBMITTED"
  | "ORDER_INPUT_INVALID"
  | "ORDER_CREATED"
  | "ORDER_COMPLETE_VIEWED";

/**
 * 1行分のログレコード。出力時はこれをそのまま JSON.stringify する。
 */
export interface StructuredLogEntry {
  /** ISO8601 (UTC) */
  timestamp: string;
  level: LogLevel;
  event: LogEvent;
  /** ブラウザタブ単位で一定 */
  sessionId: string;
  /** API リクエスト単位で更新 (バックエンド MDC と突き合わせ可) */
  traceId?: string;
  /** 認証済みユーザ名 (取得できる場合) */
  userId?: string;
  /** 自由文 (人間可読) */
  message: string;
  /** 例外情報 (あれば) */
  error?: {
    name: string;
    message: string;
    stack?: string;
  };
  /** 追加の構造化フィールド (任意キー) */
  context?: Record<string, unknown>;
  /** 環境 (dev / prod) */
  env: "dev" | "prod";
  /** クライアント情報 */
  url?: string;
  userAgent?: string;
}
