/**
 * window レベルでキャッチされない例外と Promise reject を構造化ログに変換する。
 * `installGlobalLogHandlers()` を main.tsx で起動時に 1 回だけ呼ぶ。
 */

import { logger } from "./logger";

let installed = false;

export function installGlobalLogHandlers(): void {
  if (installed) return;
  installed = true;

  window.addEventListener("error", (ev: ErrorEvent) => {
    logger.error({
      event: "UNCAUGHT_ERROR",
      message: ev.message || "uncaught error",
      error: ev.error ?? new Error(ev.message),
      context: {
        source: ev.filename,
        line: ev.lineno,
        col: ev.colno,
      },
    });
  });

  window.addEventListener("unhandledrejection", (ev: PromiseRejectionEvent) => {
    logger.error({
      event: "UNHANDLED_REJECTION",
      message: "unhandled promise rejection",
      error: ev.reason,
    });
  });

  logger.info({
    event: "APP_BOOT",
    message: "frontend boot",
    context: {
      env: import.meta.env.MODE,
      buildTime: new Date().toISOString(),
    },
  });
}
