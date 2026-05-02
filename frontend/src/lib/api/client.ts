/**
 * アプリ全体で 1 つだけ持つ ApiClient のインスタンス。
 * 認証トークンは sessionStorage に保持し、リクエスト毎に getAuthToken で取得される。
 */

import { createApiClient } from "./apiClient";

const TOKEN_KEY = "sakeec.adminToken";

export function getAdminToken(): string | undefined {
  if (typeof sessionStorage === "undefined") return undefined;
  return sessionStorage.getItem(TOKEN_KEY) ?? undefined;
}

export function setAdminToken(token: string | undefined): void {
  if (typeof sessionStorage === "undefined") return;
  if (token) sessionStorage.setItem(TOKEN_KEY, token);
  else sessionStorage.removeItem(TOKEN_KEY);
}

export const api = createApiClient({
  baseUrl: "/api",
  getAuthToken: getAdminToken,
});
