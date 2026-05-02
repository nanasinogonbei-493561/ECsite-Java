/**
 * 軽量な fetch ラッパ。すべての HTTP コールを構造化ログに記録する。
 *
 * 主な機能:
 *   1. 各リクエストに `X-Trace-Id` を採番して付与
 *      → バックエンドの MdcInjectionFilter がそれを引き継ぐので、
 *      ブラウザのログとサーバのログが同じ traceId で串刺し検索できる
 *   2. リクエスト/レスポンス/エラー/ネットワーク失敗を分けてログ
 *   3. 4xx/5xx は `ApiError` として throw (ステータスとレスポンスボディ込み)
 *   4. JSON ベースのリクエストを既定 (POST/PUT 時に Content-Type 自動設定)
 */

import { logger } from "../logging/logger";
import { newTraceId } from "../logging/traceId";

const TRACE_ID_HEADER = "X-Trace-Id";

export interface ApiClientOptions {
  /** API のベース URL。例: "/api" もしくは "https://api.example.com" */
  baseUrl?: string;
  /** Bearer トークン取得関数 (設定済みなら Authorization ヘッダに自動付与) */
  getAuthToken?: () => string | undefined;
}

export interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
  /** JSON 化されてボディに入る */
  body?: unknown;
  /** 任意の追加ヘッダ (Content-Type / Authorization は通常自動) */
  headers?: Record<string, string>;
  /** AbortSignal */
  signal?: AbortSignal;
}

export class ApiError extends Error {
  readonly status: number;
  readonly errorCode: string | undefined;
  readonly traceId: string | undefined;
  readonly responseBody: unknown;

  constructor(
    status: number,
    errorCode: string | undefined,
    traceId: string | undefined,
    responseBody: unknown,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.errorCode = errorCode;
    this.traceId = traceId;
    this.responseBody = responseBody;
  }
}

export function createApiClient(opts: ApiClientOptions = {}) {
  const baseUrl = (opts.baseUrl ?? "").replace(/\/+$/, "");

  async function request<T>(path: string, ro: RequestOptions = {}): Promise<T> {
    const traceId = newTraceId();
    const method = ro.method ?? "GET";
    const url = baseUrl + path;
    const start = performance.now();

    const headers: Record<string, string> = {
      Accept: "application/json",
      [TRACE_ID_HEADER]: traceId,
      ...(ro.headers ?? {}),
    };

    let body: BodyInit | undefined;
    if (ro.body !== undefined) {
      headers["Content-Type"] = headers["Content-Type"] ?? "application/json";
      body = JSON.stringify(ro.body);
    }

    const token = opts.getAuthToken?.();
    if (token) headers["Authorization"] = `Bearer ${token}`;

    logger.debug({
      event: "API_REQUEST",
      message: `${method} ${path}`,
      traceId,
      context: { method, url: path, hasBody: body !== undefined, hasAuth: !!token },
    });

    let res: Response;
    try {
      res = await fetch(url, { method, headers, body, signal: ro.signal });
    } catch (e) {
      const durationMs = Math.round(performance.now() - start);
      logger.error({
        event: "NETWORK_ERROR",
        message: `network error: ${method} ${path}`,
        traceId,
        error: e,
        context: { method, url: path, durationMs },
      });
      throw e;
    }

    const durationMs = Math.round(performance.now() - start);
    // バックエンドが echo してくる traceId を信頼 (両者一致するはずだが念のため上書き)
    const serverTraceId = res.headers.get(TRACE_ID_HEADER) ?? traceId;
    const responseBody = await safeReadBody(res);

    if (!res.ok) {
      const errorCode = extractErrorCode(responseBody);
      logger.warn({
        event: "API_ERROR",
        message: `${method} ${path} failed: ${res.status}`,
        traceId: serverTraceId,
        context: {
          method,
          url: path,
          status: res.status,
          errorCode,
          durationMs,
          responseBody,
        },
      });
      throw new ApiError(
        res.status,
        errorCode,
        serverTraceId,
        responseBody,
        `${method} ${path} -> ${res.status}`,
      );
    }

    logger.info({
      event: "API_RESPONSE",
      message: `${method} ${path} ${res.status}`,
      traceId: serverTraceId,
      context: { method, url: path, status: res.status, durationMs },
    });

    return responseBody as T;
  }

  return {
    get:    <T>(path: string, ro?: Omit<RequestOptions, "method" | "body">) =>
              request<T>(path, { ...ro, method: "GET" }),
    post:   <T>(path: string, body?: unknown, ro?: Omit<RequestOptions, "method" | "body">) =>
              request<T>(path, { ...ro, method: "POST", body }),
    put:    <T>(path: string, body?: unknown, ro?: Omit<RequestOptions, "method" | "body">) =>
              request<T>(path, { ...ro, method: "PUT", body }),
    delete: <T>(path: string, ro?: Omit<RequestOptions, "method" | "body">) =>
              request<T>(path, { ...ro, method: "DELETE" }),
  };
}

// ---- internal ----

async function safeReadBody(res: Response): Promise<unknown> {
  const ct = res.headers.get("Content-Type") ?? "";
  if (res.status === 204 || res.headers.get("Content-Length") === "0") return undefined;
  try {
    if (ct.includes("application/json")) return await res.json();
    return await res.text();
  } catch {
    return undefined;
  }
}

function extractErrorCode(body: unknown): string | undefined {
  if (body && typeof body === "object" && "code" in body) {
    const c = (body as { code: unknown }).code;
    if (typeof c === "string") return c;
  }
  return undefined;
}
