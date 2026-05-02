/**
 * セッション ID とリクエスト trace ID を管理する。
 *
 * - `sessionId`: ブラウザタブが開いている間ずっと一定。タブごとに別。
 *   タブのリロードでも保持するため sessionStorage に永続化。
 * - `traceId`: 1 リクエストごとに採番。バックエンドの MDC に乗るキーと同じ名前。
 *
 * `crypto.randomUUID()` は HTTPS or localhost で利用可能 (Vite dev server は localhost)。
 */

const SESSION_KEY = "sakeec.sessionId";

function newUuid(): string {
  // 通常ブラウザ (HTTPS or localhost) では crypto.randomUUID が使える
  const c = typeof crypto !== "undefined" ? crypto : undefined;
  if (c?.randomUUID) {
    return c.randomUUID();
  }
  // RFC4122 v4 風の手書き fallback (古いブラウザ / テスト)
  const bytes = new Uint8Array(16);
  if (c?.getRandomValues) {
    c.getRandomValues(bytes);
  } else {
    for (let i = 0; i < 16; i++) bytes[i] = Math.floor(Math.random() * 256);
  }
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = [...bytes].map((b) => b.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export function getSessionId(): string {
  // SSR / テストで sessionStorage が無い場合は in-memory にフォールバック
  if (typeof sessionStorage === "undefined") {
    return inMemorySessionId;
  }
  let id = sessionStorage.getItem(SESSION_KEY);
  if (!id) {
    id = newUuid();
    sessionStorage.setItem(SESSION_KEY, id);
  }
  return id;
}

export function newTraceId(): string {
  return newUuid();
}

// ---- internal ----

const inMemorySessionId = newUuid();
