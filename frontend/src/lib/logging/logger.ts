/**
 * 構造化ロガー。
 *
 * - 開発時 (`import.meta.env.DEV`): 色付きラベルで読みやすく、JSON も同時に出力
 * - 本番時: JSON 1 行のみ。リモート送信フック (`installRemoteSink`) も用意
 *
 * すべてのログ呼び出しは `StructuredLogEntry` の固定スキーマに揃える。
 * 検索クエリは `event:API_ERROR AND context.status:409` のような形で書ける。
 */

import { getSessionId } from "./traceId";
import type { LogEvent, LogLevel, StructuredLogEntry } from "./types";

type LogInput = {
  event: LogEvent;
  message: string;
  traceId?: string;
  userId?: string;
  error?: unknown;
  context?: Record<string, unknown>;
};

type RemoteSink = (entry: StructuredLogEntry) => void;

let remoteSink: RemoteSink | null = null;
let currentUserId: string | undefined;

/** 認証成功時など、後続ログに userId を載せたい場合に呼ぶ。 */
export function setUserId(id: string | undefined): void {
  currentUserId = id;
}

/**
 * 本番時にログを外部に送るシンクを登録する。
 * 例: Datadog, Sentry, 自社ログ集約 API など。同期/非同期どちらでもよい。
 * シンク内部の例外は握り潰して console.error に落とす (ログがログを生まないように)。
 */
export function installRemoteSink(sink: RemoteSink): void {
  remoteSink = sink;
}

export const logger = {
  debug: (input: LogInput) => emit("debug", input),
  info:  (input: LogInput) => emit("info",  input),
  warn:  (input: LogInput) => emit("warn",  input),
  error: (input: LogInput) => emit("error", input),
  fatal: (input: LogInput) => emit("fatal", input),
};

// ---- internal ----

function emit(level: LogLevel, input: LogInput): void {
  const entry: StructuredLogEntry = {
    timestamp: new Date().toISOString(),
    level,
    event: input.event,
    sessionId: getSessionId(),
    traceId: input.traceId,
    userId: input.userId ?? currentUserId,
    message: input.message,
    error: normalizeError(input.error),
    context: input.context,
    env: import.meta.env.DEV ? "dev" : "prod",
    url: typeof location !== "undefined" ? location.href : undefined,
    userAgent: typeof navigator !== "undefined" ? navigator.userAgent : undefined,
  };

  // omit undefined
  const compact = JSON.parse(JSON.stringify(entry)) as StructuredLogEntry;

  if (import.meta.env.DEV) {
    devPrint(level, compact);
  } else {
    // prod: JSON 1 行
    consoleFn(level)(JSON.stringify(compact));
  }

  if (remoteSink) {
    try {
      remoteSink(compact);
    } catch (e) {
      // シンクの失敗をログ化するとループするため console に直接
      // eslint-disable-next-line no-console
      console.error("[logger] remote sink threw:", e);
    }
  }
}

function consoleFn(level: LogLevel): (...args: unknown[]) => void {
  /* eslint-disable no-console */
  switch (level) {
    case "debug": return console.debug;
    case "info":  return console.info;
    case "warn":  return console.warn;
    case "error":
    case "fatal": return console.error;
  }
  /* eslint-enable no-console */
}

function levelStyle(level: LogLevel): string {
  switch (level) {
    case "debug": return "color:#888";
    case "info":  return "color:#0a7";
    case "warn":  return "color:#c80;font-weight:bold";
    case "error": return "color:#c33;font-weight:bold";
    case "fatal": return "color:#fff;background:#c33;font-weight:bold;padding:0 4px";
  }
}

function devPrint(level: LogLevel, entry: StructuredLogEntry): void {
  const tag = `%c${level.toUpperCase()} %c${entry.event}`;
  const eventStyle = "color:#06c;font-weight:bold";
  const args: unknown[] = [
    `${tag} ${entry.message}`,
    levelStyle(level),
    eventStyle,
    entry,
  ];
  consoleFn(level)(...args);
}

function normalizeError(e: unknown): StructuredLogEntry["error"] | undefined {
  if (!e) return undefined;
  if (e instanceof Error) {
    return {
      name: e.name,
      message: e.message,
      stack: e.stack,
    };
  }
  return {
    name: "NonError",
    message: typeof e === "string" ? e : safeJson(e),
  };
}

function safeJson(v: unknown): string {
  try {
    return JSON.stringify(v);
  } catch {
    return String(v);
  }
}
