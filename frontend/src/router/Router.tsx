/**
 * 依存ゼロの hash router。
 *
 * URL は `#/admin/products` のような形式で表現する。
 * リロードしてもパスが残り、ブラウザの戻る/進むも自然に動く。
 */

import { useEffect, useState } from "react";
import { logger } from "../lib/logging";

function getCurrentPath(): string {
  const hash = window.location.hash || "";
  return hash.startsWith("#") ? hash.slice(1) || "/" : "/";
}

export function useRoute(): string {
  const [path, setPath] = useState<string>(getCurrentPath());
  useEffect(() => {
    const onChange = () => {
      const next = getCurrentPath();
      setPath(next);
      logger.info({
        event: "PAGE_VIEW",
        message: `navigate ${next}`,
        context: { path: next },
      });
    };
    window.addEventListener("hashchange", onChange);
    return () => window.removeEventListener("hashchange", onChange);
  }, []);
  return path;
}

export function navigate(to: string): void {
  if (getCurrentPath() === to) return;
  window.location.hash = to;
}
